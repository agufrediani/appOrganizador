package com.example.roadbookorganizador.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UsuarioSesion(
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val nombre: String = "",
    val club: String = "",
    val rol: String = "ORGANIZADOR",
    val token: String = ""
)

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("roadbook_sesion", Context.MODE_PRIVATE)

    private val _serverUrl = MutableStateFlow(prefs.getString("server_url", "http://192.168.1.10:8000") ?: "http://192.168.1.10:8000")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _sesion = MutableStateFlow(obtenerSesionGuardada())
    val sesion: StateFlow<UsuarioSesion> = _sesion.asStateFlow()

    fun guardarServerUrl(url: String) {
        val sanitized = url.trim().trimEnd('/')
        prefs.edit().putString("server_url", sanitized).apply()
        _serverUrl.value = sanitized
    }

    private fun obtenerSesionGuardada(): UsuarioSesion {
        val logged = prefs.getBoolean("is_logged_in", false)
        return if (logged) {
            UsuarioSesion(
                isLoggedIn = true,
                email = prefs.getString("email", "") ?: "",
                nombre = prefs.getString("nombre", "Organizador Oficial") ?: "",
                club = prefs.getString("club", "Frediani Competición") ?: "",
                rol = prefs.getString("rol", "ORGANIZADOR_TRAMO") ?: "",
                token = prefs.getString("token", "dummy_token_12345") ?: ""
            )
        } else {
            UsuarioSesion(isLoggedIn = false)
        }
    }

    companion object {
        const val DEFAULT_MAPBOX_TOKEN = ""
    }

    private val _mapboxToken = MutableStateFlow(
        prefs.getString("mapbox_token", "")?.takeIf { it.isNotBlank() } ?: DEFAULT_MAPBOX_TOKEN
    )
    val mapboxToken: StateFlow<String> = _mapboxToken.asStateFlow()

    fun guardarMapboxToken(token: String) {
        val sanitized = token.trim()
        prefs.edit().putString("mapbox_token", sanitized).apply()
        _mapboxToken.value = sanitized
    }

    private val _snapToRoad = MutableStateFlow(prefs.getBoolean("snap_to_road", true))
    val snapToRoad: StateFlow<Boolean> = _snapToRoad.asStateFlow()

    fun guardarSnapToRoad(enabled: Boolean) {
        prefs.edit().putBoolean("snap_to_road", enabled).apply()
        _snapToRoad.value = enabled
    }

    fun guardarSesion(email: String, nombre: String, club: String, rol: String, token: String) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("email", email)
            .putString("nombre", nombre)
            .putString("club", club)
            .putString("rol", rol)
            .putString("token", token)
            .apply()

        _sesion.value = UsuarioSesion(
            isLoggedIn = true,
            email = email,
            nombre = nombre,
            club = club,
            rol = rol,
            token = token
        )
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
        _sesion.value = UsuarioSesion(isLoggedIn = false)
    }
}

