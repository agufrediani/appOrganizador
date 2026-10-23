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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Cómo está el tramo con la plataforma. */
enum class EstadoSincro { VERDE, AMARILLO, ROJO }

/**
 * Botón de sincronización de tres colores (sin carteles):
 * verde = al día con la web · amarillo = hay cambios por subir y hay conexión · rojo = no sincroniza.
 * Tocarlo sincroniza ahora.
 */
@Composable
fun BotonSincro(estado: EstadoSincro, pendientes: Int, sincronizando: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = when (estado) {
        EstadoSincro.VERDE -> Color(0xFF16A34A)
        EstadoSincro.AMARILLO -> Color(0xFFEAB308)
        EstadoSincro.ROJO -> Color(0xFFDC2626)
    }
    val descripcion = when (estado) {
        EstadoSincro.VERDE -> "Sincronizado con la web"
        EstadoSincro.AMARILLO -> "$pendientes cambios por subir: tocá para sincronizar"
        EstadoSincro.ROJO -> "No sincroniza con la web: tocá para reintentar"
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.5.dp, color, RoundedCornerShape(8.dp))
            .clickable(enabled = !sincronizando) { onClick() }
    ) {
        if (sincronizando) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = color, modifier = Modifier.size(16.dp))
        } else {
            BadgedBox(badge = {
                if (estado != EstadoSincro.VERDE && pendientes > 0) Badge(containerColor = color) { Text("$pendientes", color = Color.Black, fontSize = 8.sp) }
            }) {
                Icon(
                    when (estado) {
                        EstadoSincro.VERDE -> Icons.Default.CloudDone
                        EstadoSincro.AMARILLO -> Icons.Default.CloudUpload
                        EstadoSincro.ROJO -> Icons.Default.CloudOff
                    },
                    contentDescription = descripcion, tint = color, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
