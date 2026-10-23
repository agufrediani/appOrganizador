package com.example.roadbookorganizador.data.local

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencias del usuario, las mismas que la plataforma (botón del engranaje, solapa General).
 * Se guardan en esta tablet y cambian al instante el editor.
 */
data class PreferenciasVista(
    /** Palito del GPS en las flechas. Por defecto NO (apagado no se dibuja, pero no se borra). */
    val mostrarPalitoGps: Boolean = false,
    /** Rumbo (CAP) de la viñeta, solo cuando lo tiene cargado. Por defecto NO. */
    val mostrarRumboCap: Boolean = false,
    /** Decimales de las distancias en el editor: 2 (0,00) o 3 (0,000). */
    val decimalesKm: Int = 2,
    /** Ruta automática "estilo navegador" (revisión 22): una viñeta en cada maniobra. Por defecto NO. */
    val rutaAutomatica: Boolean = false
) {
    /** Distancia con la cantidad de decimales elegida y coma decimal (como la plataforma). */
    fun km(v: Double): String = String.format(java.util.Locale.US, "%.${decimalesKm}f", v).replace('.', ',')
}

object Preferencias {
    private const val ARCHIVO = "preferencias_vista"
    private val estado = MutableStateFlow(PreferenciasVista())
    @Volatile private var cargado = false

    fun flujo(context: Context): StateFlow<PreferenciasVista> {
        cargar(context)
        return estado.asStateFlow()
    }

    fun actual(context: Context): PreferenciasVista {
        cargar(context)
        return estado.value
    }

    private fun cargar(context: Context) {
        if (cargado) return
        synchronized(this) {
            if (cargado) return
            val sp = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            estado.value = PreferenciasVista(
                mostrarPalitoGps = sp.getBoolean("mostrarPalitoGps", false),
                mostrarRumboCap = sp.getBoolean("mostrarRumboCap", false),
                decimalesKm = if (sp.getInt("decimalesKm", 2) == 3) 3 else 2,
                rutaAutomatica = sp.getBoolean("rutaAutomatica", false)
            )
            cargado = true
        }
    }

    fun cambiar(context: Context, nuevo: PreferenciasVista) {
        cargar(context)
        estado.value = nuevo
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit()
            .putBoolean("mostrarPalitoGps", nuevo.mostrarPalitoGps)
            .putBoolean("mostrarRumboCap", nuevo.mostrarRumboCap)
            .putInt("decimalesKm", nuevo.decimalesKm)
            .putBoolean("rutaAutomatica", nuevo.rutaAutomatica)
            .apply()
    }
}

/** Las preferencias actuales; la pantalla se vuelve a dibujar cuando cambian. */
@Composable
fun preferenciasVista(): PreferenciasVista {
    val context = LocalContext.current
    val p by Preferencias.flujo(context).collectAsState()
    return p
}
