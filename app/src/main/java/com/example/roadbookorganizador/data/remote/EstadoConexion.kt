package com.example.roadbookorganizador.data.remote

import android.content.Context
import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.SessionManager
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import com.example.roadbookorganizador.ui.components.AvisoSuperior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado de la conexión con la plataforma, el mismo para toda la app (el indicador de colores
 * de las cabeceras): si el servidor contesta, cuántos cambios de la tablet faltan subir y si la
 * última sincronización falló. Cada 20 s se revisa; si hay señal y cambios pendientes, se
 * sincroniza solo cada minuto.
 */
object EstadoConexion {
    private val _enLinea = MutableStateFlow<Boolean?>(null)
    /** null = todavía no se revisó. */
    val enLinea: StateFlow<Boolean?> = _enLinea.asStateFlow()

    private val _pendientes = MutableStateFlow(0)
    val pendientes: StateFlow<Int> = _pendientes.asStateFlow()

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    private val _fallo = MutableStateFlow(false)
    val fallo: StateFlow<Boolean> = _fallo.asStateFlow()

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var arrancado = false

    fun arrancar(contexto: Context) {
        if (arrancado) return
        arrancado = true
        val app = contexto.applicationContext
        alcance.launch {
            var ultimaAuto = 0L
            while (true) {
                revisar(app)
                val ahora = System.currentTimeMillis()
                if (_enLinea.value == true && _pendientes.value > 0 && !_sincronizando.value && ahora - ultimaAuto > 60_000 &&
                    SessionManager(app).sesion.value.isLoggedIn
                ) {
                    ultimaAuto = ahora
                    sincronizarAhora(app, conAviso = false)
                }
                delay(20_000)
            }
        }
    }

    /** Revisa el servidor y cuenta lo pendiente (también se llama después de guardar algo). */
    suspend fun revisar(contexto: Context) {
        val app = contexto.applicationContext
        _enLinea.value = try { ApiCliente(app).revisar() == null } catch (e: Exception) { false }
        contarPendientes(app)
    }

    suspend fun contarPendientes(contexto: Context) {
        _pendientes.value = try {
            RoadbookRepository(AppDatabase.getInstance(contexto.applicationContext)).contarPendientesTodos()
        } catch (e: Exception) { _pendientes.value }
    }

    /** Tocar el indicador: sube lo pendiente y baja lo último de la plataforma. */
    fun sincronizar(contexto: Context) {
        val app = contexto.applicationContext
        alcance.launch { sincronizarAhora(app, conAviso = true) }
    }

    private suspend fun sincronizarAhora(app: Context, conAviso: Boolean) {
        if (_sincronizando.value) return
        _sincronizando.value = true
        try {
            val r = WebSyncService(app, RoadbookRepository(AppDatabase.getInstance(app))).sincronizarConServidor()
            _fallo.value = !r.success
            if (!r.success && conAviso) withContext(Dispatchers.Main) { AvisoSuperior.mostrar(r.message) }
        } catch (e: Exception) {
            _fallo.value = true
        } finally {
            _sincronizando.value = false
            revisar(app)
        }
    }

    /** Lo que avisa cualquier otra sincronización (por ejemplo la del tramo abierto). */
    fun informarResultado(ok: Boolean) { _fallo.value = !ok }
}
