package com.example.roadbookorganizador.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sesión del organizador, la misma de la plataforma (ESPECIFICACION_TAQUEO punto 0):
 * usuario y token que devuelve `POST /api/auth/login`. El token dura 12 horas.
 */
data class UsuarioSesion(
    val isLoggedIn: Boolean = false,
    val usuarioId: Int = 0,
    val username: String = "",
    val nombre: String = "",
    val rol: String = "",
    val email: String = "",
    val token: String = "",
    /** Hasta cuándo vale el token (milisegundos). */
    val venceMs: Long = 0L,
    /** Compatibilidad con pantallas viejas. */
    val club: String = ""
)

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("roadbook_sesion", Context.MODE_PRIVATE)

    init {
        // Una sola sesión para toda la app (todas las pantallas ven el mismo estado)
        synchronized(Companion) {
            if (!cargado) {
                _serverUrl.value = prefs.getString("server_url", null)
                    ?.takeIf { it.startsWith("http") && !it.endsWith(":8000") } // la dirección vieja de prueba no sirve más
                    ?: SERVIDOR_PRODUCCION
                _sesion.value = obtenerSesionGuardada()
                _mapboxToken.value = prefs.getString("mapbox_token", "")?.takeIf { it.isNotBlank() } ?: DEFAULT_MAPBOX_TOKEN
                _snapToRoad.value = prefs.getBoolean("snap_to_road", true)
                cargado = true
            }
        }
    }

    val serverUrl: StateFlow<String> get() = _serverUrl.asStateFlow()
    val sesion: StateFlow<UsuarioSesion> get() = _sesion.asStateFlow()
    val mapboxToken: StateFlow<String> get() = _mapboxToken.asStateFlow()
    val snapToRoad: StateFlow<Boolean> get() = _snapToRoad.asStateFlow()

    /** Dirección del servidor (ajuste editable). Sin barra al final. */
    fun guardarServerUrl(url: String) {
        val sanitized = url.trim().trimEnd('/').ifBlank { SERVIDOR_PRODUCCION }
        prefs.edit().putString("server_url", sanitized).apply()
        _serverUrl.value = sanitized
    }

    private fun obtenerSesionGuardada(): UsuarioSesion {
        val token = prefs.getString("token", "") ?: ""
        val vence = prefs.getLong("token_vence_ms", 0L)
        // Las sesiones de antes (sin token del servidor) no valen: hay que entrar de nuevo
        if (token.isBlank() || vence <= System.currentTimeMillis()) return UsuarioSesion(isLoggedIn = false)
        return UsuarioSesion(
            isLoggedIn = true,
            usuarioId = prefs.getInt("usuario_id", 0),
            username = prefs.getString("username", "") ?: "",
            nombre = prefs.getString("nombre", "") ?: "",
            rol = prefs.getString("rol", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            token = token,
            venceMs = vence
        )
    }

    fun guardarMapboxToken(token: String) {
        val sanitized = token.trim()
        prefs.edit().putString("mapbox_token", sanitized).apply()
        _mapboxToken.value = sanitized
    }

    fun guardarSnapToRoad(enabled: Boolean) {
        prefs.edit().putBoolean("snap_to_road", enabled).apply()
        _snapToRoad.value = enabled
    }

    /** Guarda lo que devolvió el login del servidor. */
    fun guardarSesion(
        usuarioId: Int, username: String, nombre: String, rol: String, email: String,
        token: String, expiraEnHoras: Double
    ) {
        val vence = System.currentTimeMillis() + (expiraEnHoras * 3_600_000).toLong()
        prefs.edit()
            .putInt("usuario_id", usuarioId)
            .putString("username", username)
            .putString("nombre", nombre)
            .putString("rol", rol)
            .putString("email", email)
            .putString("token", token)
            .putLong("token_vence_ms", vence)
            .putString("ultimo_usuario", username)
            .apply()
        _sesion.value = UsuarioSesion(true, usuarioId, username, nombre, rol, email, token, vence)
    }

    /** Último usuario que entró (para no tener que escribirlo de nuevo). */
    val ultimoUsuario: String get() = prefs.getString("ultimo_usuario", "") ?: ""

    /** Cierra la sesión. Solo borra la sesión: el servidor, Mapbox y demás ajustes quedan. */
    fun cerrarSesion() {
        prefs.edit()
            .remove("is_logged_in").remove("usuario_id").remove("username").remove("nombre")
            .remove("rol").remove("email").remove("club").remove("token").remove("token_vence_ms")
            .apply()
        _sesion.value = UsuarioSesion(isLoggedIn = false)
    }

    companion object {
        const val DEFAULT_MAPBOX_TOKEN = ""
        /** Servidor de la plataforma publicada (Azure). Para probar en la PC: http://<IP-de-la-PC>:5000 */
        const val SERVIDOR_PRODUCCION = "https://frediani-roadbook-api.azurewebsites.net"

        @Volatile private var cargado = false
        private val _serverUrl = MutableStateFlow(SERVIDOR_PRODUCCION)
        private val _sesion = MutableStateFlow(UsuarioSesion())
        private val _mapboxToken = MutableStateFlow(DEFAULT_MAPBOX_TOKEN)
        private val _snapToRoad = MutableStateFlow(true)
    }
}
