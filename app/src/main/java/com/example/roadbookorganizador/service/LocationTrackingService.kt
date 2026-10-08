package com.example.roadbookorganizador.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.roadbookorganizador.MainActivity
import com.example.roadbookorganizador.R
import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.entity.TrackPointEntity
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var usandoFused = false
    private var nativeListener: android.location.LocationListener? = null
    private var wakeLock: PowerManager.WakeLock? = null

    val odometerEngine = sharedOdometerEngine
    private lateinit var persistencia: OdometroPersistencia
    private var ultimoTrackPointMs = 0L
    private var ultimaNotificacionMs = 0L

    private val raceBoxListener: (com.example.roadbookorganizador.gps.racebox.RaceBoxTelemetry) -> Unit = { telemetry ->
        if (telemetry.hasValidFix) _fixes.tryEmit(telemetry.toGpsFix(System.currentTimeMillis()))
        val aceptado = odometerEngine.procesarTelemetriaRaceBox(telemetry)
        actualizarNotificacion()
        if (aceptado != null) guardarTrackPoint(aceptado)
    }

    /** Punto único de entrada de las posiciones de la tablet (una sola fuente activa). */
    private fun onUbicacionTablet(location: Location) {
        val fix = location.toGpsFix()
        _fixes.tryEmit(fix)
        val aceptado = odometerEngine.procesarFix(fix)
        actualizarNotificacion()
        if (aceptado) guardarTrackPoint(fix)
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        crearCanalNotificacion()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoadbookOrganizador:TrackingWakeLock")

        // Si Android cerró la app en medio de un trazado, retomar el tramo y el odómetro guardados
        persistencia = OdometroPersistencia(this)
        if (tramoActivoId == null && odometerEngine.state.value.odometroTotalKm == 0.0) {
            persistencia.leer()?.let { g ->
                if (g.tramoId > 0 && g.activo) {
                    tramoActivoId = g.tramoId
                    odometerEngine.restaurar(g.totalKm, g.parcialKm)
                }
            }
        }
        actualizarWakeLock()

        // Guardar el odómetro del tramo activo cada 2 segundos
        serviceScope.launch {
            while (isActive) {
                delay(2000L)
                guardarEstadoOdometro()
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    onUbicacionTablet(location)
                }
            }
        }

        // Cargar factor de calibración activo
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val cal = db.calibracionDao().getCalibracionActivaSync()
            cal?.let { odometerEngine.setFactorCalibracion(it.factorCorreccion) }
        }

        // Suscribir al motor GPS Externo 25Hz BLE
        val bleManager = com.example.roadbookorganizador.gps.racebox.RaceBoxBleManager.getInstance(applicationContext)
        bleManager.addTelemetryListener(raceBoxListener)

        serviceScope.launch {
            bleManager.connectionStatus.collect { status ->
                val conectado = (status == com.example.roadbookorganizador.gps.racebox.RaceBoxConnectionStatus.CONNECTED)
                odometerEngine.setRaceBoxBleConectado(conectado)
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification("Iniciando GPS / Wi-Fi de alta precisión..."))
        iniciarSolicitudUbicacion()
    }

    private var simulacionJob: kotlinx.coroutines.Job? = null

    fun iniciarSimulacion(velocidadKmh: Float = 60.0f) {
        simulacionJob?.cancel()
        odometerEngine.setModoSimulacion(true)
        simulacionJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val mPorSegundo = (velocidadKmh / 3.6)
                odometerEngine.simularPaso(deltaMetros = mPorSegundo, velKmh = velocidadKmh)
                actualizarNotificacion()
            }
        }
    }

    fun detenerSimulacion() {
        simulacionJob?.cancel()
        simulacionJob = null
        odometerEngine.setModoSimulacion(false)
        actualizarNotificacion()
    }

    fun toggleSimulacion(velocidadKmh: Float = 60.0f) {
        if (simulacionJob != null && simulacionJob?.isActive == true) {
            detenerSimulacion()
        } else {
            iniciarSimulacion(velocidadKmh)
        }
    }

    /**
     * Una sola fuente de ubicación de la tablet:
     * - Google Fused Location si la tablet tiene Google Play Services.
     * - Si no, el GPS nativo (GPS_PROVIDER). Nunca la ubicación por red (Wi-Fi / antenas),
     *   que tiene errores de 20 a 100 m y suma distancia falsa.
     */
    @SuppressLint("MissingPermission")
    private fun iniciarSolicitudUbicacion() {
        val playServicesOk = try {
            com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this) == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            false
        }

        if (playServicesOk) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setWaitForAccurateLocation(false)
                .build()
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
                usandoFused = true
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback: GPS nativo (tablets sin Google Play Services)
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val listener = android.location.LocationListener { loc -> onUbicacionTablet(loc) }
            locationManager.requestLocationUpdates(
                android.location.LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                listener,
                mainLooper
            )
            nativeListener = listener
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun guardarEstadoOdometro() {
        val tramoId = tramoActivoId ?: return
        val s = odometerEngine.state.value
        persistencia.guardar(tramoId, s.odometroTotalKm, s.odometroParcialKm)
    }

    /** El procesador se mantiene despierto solo mientras hay un tramo en trazado. */
    @Synchronized
    fun actualizarWakeLock() {
        val wl = wakeLock ?: return
        if (tramoActivoId != null) {
            if (!wl.isHeld) wl.acquire(12 * 60 * 60 * 1000L) // Hasta 12 horas de jornada
        } else if (wl.isHeld) {
            wl.release()
        }
    }

    private fun guardarTrackPoint(fix: GpsFix) {
        val tramoId = tramoActivoId ?: return
        // Máximo 4 puntos por segundo en la base (el RaceBox manda 25 por segundo)
        if (fix.timeMs - ultimoTrackPointMs in 0L until 250L) return
        ultimoTrackPointMs = fix.timeMs
        val s = odometerEngine.state.value
        serviceScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                db.trackPointDao().insertTrackPoint(
                    TrackPointEntity(
                        tramoId = tramoId,
                        latitud = fix.latitud,
                        longitud = fix.longitud,
                        altitud = fix.altitud,
                        velocidadKmh = s.velocidadKmh,
                        rumbo = fix.rumbo ?: 0f,
                        distanciaAcumulada = s.odometroTotalKm,
                        timestamp = fix.timeMs
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo y Odómetro en Terreno",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activo el odómetro y satélites GNSS durante el trazado de carrera"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(texto: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Frediani Roadbook - Trazador")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun actualizarNotificacion() {
        // Una actualización por segundo alcanza (Android descarta las más frecuentes)
        val ahora = System.currentTimeMillis()
        if (ahora - ultimaNotificacionMs < 1000L) return
        ultimaNotificacionMs = ahora
        val s = odometerEngine.state.value
        val texto = String.format("Total: %.3f km | Parcial: %.3f km | %.0f km/h",
            s.odometroTotalKm, s.odometroParcialKm, s.velocidadKmh)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(texto))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        com.example.roadbookorganizador.gps.racebox.RaceBoxBleManager.getInstance(applicationContext)
            .removeTelemetryListener(raceBoxListener)
        if (usandoFused) fusedLocationClient.removeLocationUpdates(locationCallback)
        nativeListener?.let { listener ->
            try {
                (getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager)
                    .removeUpdates(listener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        nativeListener = null
        guardarEstadoOdometro()
        wakeLock?.let { if (it.isHeld) it.release() }
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "roadbook_tracking_channel"
        const val NOTIFICATION_ID = 1001

        val sharedOdometerEngine = OdometerEngine()

        /**
         * Todas las posiciones crudas (RaceBox con fix válido y tablet), para otros consumidores
         * como el calibrador o el mapa libre. Así nadie más registra listeners de GPS propios.
         */
        private val _fixes = MutableSharedFlow<GpsFix>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val fixes: SharedFlow<GpsFix> = _fixes.asSharedFlow()

        fun Location.toGpsFix(): GpsFix = GpsFix(
            latitud = latitude,
            longitud = longitude,
            altitud = altitude,
            velocidadKmh = if (hasSpeed()) speed * 3.6f else null,
            rumbo = if (hasBearing()) bearing else null,
            precisionMetros = if (hasAccuracy()) accuracy else null,
            timeMs = if (time > 0) time else System.currentTimeMillis(),
            fuente = FuenteGps.TABLET
        )
        var instance: LocationTrackingService? = null

        /**
         * Tramo en trazado. Mientras no sea null se graba el track, se guarda el odómetro y se
         * mantiene el procesador despierto. No depende de que el servicio ya esté creado.
         */
        @Volatile
        var tramoActivoId: Long? = null
            private set

        fun setTramoActivo(tramoId: Long?) {
            tramoActivoId = tramoId
            instance?.actualizarWakeLock()
        }

        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }
}
