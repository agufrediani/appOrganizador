package com.example.roadbookorganizador.util

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.roadbookorganizador.data.remote.ApiCliente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Imágenes del rally que se cargan en la plataforma: la portada (foto grande del Panel) y la
 * chapa oficial. Se bajan al sincronizar y quedan guardadas en la tablet (se ven sin señal).
 */
object ImagenesRally {
    const val PORTADA = "portada"
    const val CHAPA = "chapa"

    /** Sube cada vez que se bajan imágenes, para que las pantallas las vuelvan a leer. */
    val version = MutableStateFlow(0)

    fun archivo(contexto: Context, rallyWebId: Int, tipo: String): File =
        File(File(contexto.filesDir, "rallies"), "${rallyWebId}_$tipo.img")

    /** Baja la imagen de [url] (vacía = el rally ya no tiene esa imagen: se borra la guardada). */
    suspend fun bajar(contexto: Context, api: ApiCliente, rallyWebId: Int, tipo: String, url: String) {
        val destino = archivo(contexto, rallyWebId, tipo)
        if (url.isBlank()) {
            if (destino.exists()) { destino.delete(); version.value++ }
            return
        }
        val bytes = try { api.bajarArchivo(url) } catch (e: Exception) { return } // sin señal: queda la que había
        withContext(Dispatchers.IO) {
            if (bytes == null) { if (destino.exists()) destino.delete() }
            else {
                destino.parentFile?.mkdirs()
                val tmp = File(destino.parentFile, destino.name + ".tmp")
                tmp.writeBytes(bytes)
                tmp.renameTo(destino)
            }
        }
        version.value++
    }

    /** Lee la imagen guardada, achicada para que no ocupe memoria de más. */
    fun leer(contexto: Context, rallyWebId: Int, tipo: String, ladoMaximo: Int = 1600): ImageBitmap? {
        val f = archivo(contexto, rallyWebId, tipo)
        if (!f.exists()) return null
        return try {
            val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, medidas)
            var escala = 1
            while (maxOf(medidas.outWidth, medidas.outHeight) / (escala * 2) >= ladoMaximo) escala *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = escala })?.asImageBitmap()
        } catch (e: Exception) { null }
    }
}

/** La imagen del rally para mostrar (null si no tiene o todavía no se bajó). */
@Composable
fun imagenRally(rallyWebId: Int?, tipo: String, ladoMaximo: Int = 1600): ImageBitmap? {
    val contexto = LocalContext.current
    val version by ImagenesRally.version.collectAsState()
    val img by produceState<ImageBitmap?>(null, rallyWebId, tipo, version) {
        value = if (rallyWebId == null) null else withContext(Dispatchers.IO) { ImagenesRally.leer(contexto, rallyWebId, tipo, ladoMaximo) }
    }
    return img
}

/**
 * Fondo de portada del rally: la foto si la tiene (recortada para llenar) y la chapa oficial
 * arriba a la izquierda, como en la plataforma. Sin foto queda el degradé de siempre.
 */
@Composable
fun androidx.compose.foundation.layout.BoxScope.PortadaRally(rallyWebId: Int?, anchoChapa: androidx.compose.ui.unit.Dp) {
    val portada = imagenRally(rallyWebId, ImagenesRally.PORTADA)
    val chapa = imagenRally(rallyWebId, ImagenesRally.CHAPA, ladoMaximo = 600)
    if (portada != null) {
        androidx.compose.foundation.Image(
            bitmap = portada, contentDescription = "Portada del rally",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = androidx.compose.ui.Modifier.matchParentSize()
        )
    }
    // Sombra para que se lean las letras encima de la foto
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier.matchParentSize().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(androidx.compose.ui.graphics.Color(0x80000000), androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color(0xFF0A0E17))
            )
        )
    )
    if (chapa != null) {
        androidx.compose.foundation.Image(
            bitmap = chapa, contentDescription = "Chapa oficial",
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = androidx.compose.ui.Modifier
                .align(androidx.compose.ui.Alignment.TopStart)
                .padding(12.dp)
                .widthIn(max = anchoChapa)
                .heightIn(max = anchoChapa * 0.7f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                .padding(6.dp)
        )
    }
}
