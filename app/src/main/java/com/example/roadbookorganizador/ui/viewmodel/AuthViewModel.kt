package com.example.roadbookorganizador.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadbookorganizador.data.local.SessionManager
import com.example.roadbookorganizador.data.local.UsuarioSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    val sesion: StateFlow<UsuarioSesion> = sessionManager.sesion

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun login(email: String, clave: String, onExito: () -> Unit) {
        if (email.isBlank() || clave.isBlank()) {
            _loginError.value = "Por favor ingrese su usuario y contraseña"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null

            // Simulación / Verificación de credenciales de organizador
            kotlinx.coroutines.delay(800)

            if (clave.length >= 4) {
                val nombreOrganizador = if (email.contains("@")) email.substringBefore("@").replace(".", " ").capitalize() else "Organizador"
                sessionManager.guardarSesion(
                    email = email.trim(),
                    nombre = nombreOrganizador,
                    club = "Frediani Competición",
                    rol = "ORGANIZADOR_TRAMO",
                    token = "token_auth_rally_${System.currentTimeMillis()}"
                )
                _isLoading.value = false
                onExito()
            } else {
                _isLoading.value = false
                _loginError.value = "Contraseña incorrecta (mínimo 4 caracteres)"
            }
        }
    }

    fun logout(onCompletado: () -> Unit) {
        sessionManager.cerrarSesion()
        onCompletado()
    }
}
