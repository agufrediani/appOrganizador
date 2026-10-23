package com.example.roadbookorganizador.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aviso chiquito arriba de la pantalla, que se va solo (en lugar del cartel de abajo de Android).
 * Una sola para toda la app: se pone una vez en MainActivity.
 */
object AvisoSuperior {
    const val SINCRONIZADO = "Sincronizado con la web"

    data class Aviso(val id: Long, val texto: String)

    private val _actual = MutableStateFlow<Aviso?>(null)
    val actual: StateFlow<Aviso?> = _actual.asStateFlow()

    fun mostrar(texto: String) {
        if (texto.isBlank()) return
        _actual.value = Aviso(System.nanoTime(), texto)
    }
}

@Composable
fun AvisoSuperiorHost(modifier: Modifier = Modifier) {
    val aviso by AvisoSuperior.actual.collectAsState()
    var visible by remember { mutableStateOf(false) }
    var mostrado by remember { mutableStateOf<AvisoSuperior.Aviso?>(null) }

    LaunchedEffect(aviso?.id) {
        val a = aviso ?: return@LaunchedEffect
        mostrado = a
        visible = true
        // El de "sincronizado" dura poco; los demás, un poco más para poder leerlos
        delay(if (a.texto == AvisoSuperior.SINCRONIZADO) 1800 else 4000)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        val a = mostrado ?: return@AnimatedVisibility
        val ok = a.texto == AvisoSuperior.SINCRONIZADO
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 6.dp, start = 16.dp, end = 16.dp)
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF0F172A).copy(alpha = 0.92f))
                .border(1.dp, if (ok) Color(0xFF06D6A0).copy(alpha = 0.6f) else Color(0xFF334155), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Icon(
                if (ok) Icons.Default.CloudDone else Icons.Default.Info, contentDescription = null,
                tint = if (ok) Color(0xFF06D6A0) else Color(0xFF38BDF8), modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(a.texto, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
