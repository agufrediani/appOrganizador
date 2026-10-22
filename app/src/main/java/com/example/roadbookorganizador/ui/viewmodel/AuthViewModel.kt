package com.example.roadbookorganizador.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadbookorganizador.data.local.SessionManager
import com.example.roadbookorganizador.data.local.UsuarioSesion
import com.example.roadbookorganizador.data.remote.ApiCliente
import com.example.roadbookorganizador.data.remote.ErrorServidor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado del servidor, como el cartel "Estado del Backend" del login de la plataforma. */
enum class EstadoServidor { REVISANDO, EN_LINEA, DESCONECTADO }

/**
 * Entrada con el mismo usuario y contraseña de la plataforma (`POST /api/auth/login`).
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val api = ApiCliente(application)
    val sesion: StateFlow<UsuarioSesion> = sessionManager.sesion
    val serverUrl: StateFlow<String> = sessionManager.serverUrl
    val ultimoUsuario: String get() = sessionManager.ultimoUsuario

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _estadoServidor = MutableStateFlow(EstadoServidor.REVISANDO)
    val estadoServidor: StateFlow<EstadoServidor> = _estadoServidor.asStateFlow()

    /** Por qué no conecta (null si conecta). */
    private val _motivoServidor = MutableStateFlow<String?>(null)
    val motivoServidor: StateFlow<String?> = _motivoServidor.asStateFlow()

    init {
        revisarServidor()
        // Mientras esté desconectado, se vuelve a probar solo cada 5 segundos
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                if (_estadoServidor.value == EstadoServidor.DESCONECTADO) revisarServidor()
            }
        }
    }

    fun revisarServidor() {
        viewModelScope.launch {
            if (_estadoServidor.value != EstadoServidor.DESCONECTADO) _estadoServidor.value = EstadoServidor.REVISANDO
            val motivo = api.revisar()
            _motivoServidor.value = motivo
            _estadoServidor.value = if (motivo == null) EstadoServidor.EN_LINEA else EstadoServidor.DESCONECTADO
        }
    }

    fun cambiarServidor(url: String) {
        sessionManager.guardarServerUrl(url)
        revisarServidor()
    }

    fun login(usuario: String, clave: String, onExito: () -> Unit) {
        if (usuario.isBlank() || clave.isBlank()) {
            _loginError.value = "Ingresá usuario y contraseña"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            try {
                val r = api.login(usuario.trim(), clave.trim())
                val u = r.getAsJsonObject("usuario")
                fun txt(o: com.google.gson.JsonObject?, k: String) =
                    o?.get(k)?.takeIf { !it.isJsonNull }?.asString ?: ""
                val token = txt(r, "token")
                if (token.isBlank()) throw ErrorServidor(0, "el servidor no mandó el token")
                sessionManager.guardarSesion(
                    usuarioId = u?.get("id")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
                    username = txt(u, "username").ifBlank { usuario.trim() },
                    nombre = txt(u, "nombreCompleto"),
                    rol = txt(u, "rol"),
                    email = txt(u, "email"),
                    token = token,
                    expiraEnHoras = r.get("expiresInHours")?.takeIf { !it.isJsonNull }?.asDouble ?: 12.0
                )
                _estadoServidor.value = EstadoServidor.EN_LINEA
                onExito()
            } catch (e: ErrorServidor) {
                _loginError.value = if (e.codigo == 401 || e.codigo == 400) "Credenciales inválidas" else e.message
            } catch (e: Exception) {
                _estadoServidor.value = EstadoServidor.DESCONECTADO
                _loginError.value = "No se pudo conectar con el servidor (${serverUrl.value}). Revisá la dirección y la conexión."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onCompletado: () -> Unit) {
        sessionManager.cerrarSesion()
        onCompletado()
    }
}
