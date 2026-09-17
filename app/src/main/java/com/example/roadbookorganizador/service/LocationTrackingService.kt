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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var wakeLock: PowerManager.WakeLock? = null

    val odometerEngine = sharedOdometerEngine
    var currentTramoId: Long? = null

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
        wakeLock?.acquire(12 * 60 * 60 * 1000L) // Hasta 12 horas de jornada

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    odometerEngine.procesarNuevaUbicacion(location)
                    actualizarNotificacion()
                    guardarTrackPoint(location)
                }
            }
        }

        // Cargar factor de calibración activo
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val cal = db.calibracionDao().getCalibracionActivaSync()
            cal?.let { odometerEngine.setFactorCalibracion(it.factorCorreccion) }
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

    @SuppressLint("MissingPermission")
    private fun iniciarSolicitudUbicacion() {
        // 1. Google Fused Provider
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(0.5f)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) odometerEngine.procesarNuevaUbicacion(loc)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Android Nativo Fallback (vital para tablets Wi-Fi o sin Google Services completos)
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val nativeListener = android.location.LocationListener { loc ->
                odometerEngine.procesarNuevaUbicacion(loc)
                actualizarNotificacion()
                guardarTrackPoint(loc)
            }

            if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER,
                    1000L,
                    0.5f,
                    nativeListener,
                    mainLooper
                )
            }

            if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0.5f,
                    nativeListener,
                    mainLooper
                )
            }

            val lastGps = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNet
            if (best != null) {
                odometerEngine.procesarNuevaUbicacion(best)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun guardarTrackPoint(loc: Location) {
        val tramoId = currentTramoId ?: return
        val s = odometerEngine.state.value
        serviceScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                db.trackPointDao().insertTrackPoint(
                    TrackPointEntity(
                        tramoId = tramoId,
                        latitud = loc.latitude,
                        longitud = loc.longitude,
                        altitud = loc.altitude,
                        velocidadKmh = loc.speed * 3.6f,
                        rumbo = if (loc.hasBearing()) loc.bearing else 0f,
                        distanciaAcumulada = s.odometroTotalKm
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
        val s = odometerEngine.state.value
        val texto = String.format("Total: %.3f km | Parcial: %.3f km | %.0f km/h",
            s.odometroTotalKm, s.odometroParcialKm, s.velocidadKmh)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(texto))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        fusedLocationClient.removeLocationUpdates(locationCallback)
        wakeLock?.let { if (it.isHeld) it.release() }
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "roadbook_tracking_channel"
        const val NOTIFICATION_ID = 1001

        val sharedOdometerEngine = OdometerEngine()
        var instance: LocationTrackingService? = null

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
