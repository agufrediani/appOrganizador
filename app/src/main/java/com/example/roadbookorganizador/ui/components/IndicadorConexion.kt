package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.remote.EstadoConexion

/** Color del estado de toda la app: verde al día, amarillo cambios por subir, rojo sin conexión. */
@Composable
fun estadoConexionApp(): EstadoSincro {
    val enLinea by EstadoConexion.enLinea.collectAsState()
    val pendientes by EstadoConexion.pendientes.collectAsState()
    val fallo by EstadoConexion.fallo.collectAsState()
    return when {
        enLinea == false || fallo -> EstadoSincro.ROJO
        pendientes > 0 -> EstadoSincro.AMARILLO
        else -> EstadoSincro.VERDE
    }
}

/**
 * Indicador de conexión con la plataforma, el mismo en toda la app. Tocarlo sincroniza.
 * [conTexto] = la versión ancha, con la frase (por ejemplo en "Mis Rallies").
 */
@Composable
fun IndicadorConexion(conTexto: Boolean = false, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val estado = estadoConexionApp()
    val pendientes by EstadoConexion.pendientes.collectAsState()
    val sincronizando by EstadoConexion.sincronizando.collectAsState()
    val enLinea by EstadoConexion.enLinea.collectAsState()
    if (!conTexto) {
        BotonSincro(estado, pendientes, sincronizando || enLinea == null, onClick = { EstadoConexion.sincronizar(contexto) }, modifier = modifier)
        return
    }
    val color = when (estado) {
        EstadoSincro.VERDE -> Color(0xFF16A34A)
        EstadoSincro.AMARILLO -> Color(0xFFEAB308)
        EstadoSincro.ROJO -> Color(0xFFDC2626)
    }
    val texto = when {
        sincronizando -> "Sincronizando…"
        enLinea == null -> "Revisando conexión…"
        estado == EstadoSincro.ROJO -> if (enLinea == false) "Sin conexión con la web" else "No se pudo sincronizar"
        estado == EstadoSincro.AMARILLO -> "$pendientes cambios por subir"
        else -> "Sincronizado con la web"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .clickable(enabled = !sincronizando) { EstadoConexion.sincronizar(contexto) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (sincronizando || enLinea == null) CircularProgressIndicator(strokeWidth = 2.dp, color = color, modifier = Modifier.size(14.dp))
        else Icon(
            when (estado) {
                EstadoSincro.VERDE -> Icons.Default.CloudDone
                EstadoSincro.AMARILLO -> Icons.Default.CloudUpload
                EstadoSincro.ROJO -> Icons.Default.CloudOff
            },
            contentDescription = null, tint = color, modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(texto.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1)
    }
}
