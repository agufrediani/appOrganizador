package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.service.OdometerState
import kotlin.math.*

/**
 * Taqueo en el terreno (ESPECIFICACION_TAQUEO punto 4): la viñeta que viene, a cuántos metros
 * está el auto y tres botones grandes: "Taquear acá", "Está bien" y "Nueva viñeta".
 * Si el auto está a menos de 30 m de otra viñeta, pregunta "¿Es la viñeta N?" en lugar de
 * repetir una.
 */
@Composable
fun PanelTaqueo(
    vinetas: List<VinetaEntity>,
    seleccionadaId: Long?,
    odo: OdometerState,
    pendientes: Int,
    sincronizando: Boolean,
    vinetaCercana: (excluirId: Long?) -> VinetaEntity?,
    onTaquear: (VinetaEntity) -> Unit,
    onEstaBien: (VinetaEntity) -> Unit,
    onNueva: (previa: VinetaEntity?) -> Unit,
    onVolverAnterior: (VinetaEntity) -> Unit,
    onElegir: (VinetaEntity) -> Unit,
    onSincronizar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ColoresPlataforma
    // La que viene: la elegida o, si no, la primera sin verificar
    val proxima = vinetas.firstOrNull { it.id == seleccionadaId } ?: vinetas.firstOrNull { !it.verificadaTerreno }
    val hayGps = !(odo.latitud == 0.0 && odo.longitud == 0.0)
    val tieneCoord = proxima != null && !(proxima.latitud == 0.0 && proxima.longitud == 0.0)
    val distanciaM = if (proxima != null && hayGps && tieneCoord) metros(odo.latitud, odo.longitud, proxima.latitud, proxima.longitud) else null
    // Para "Nueva viñeta": va después de la última que se pasó (la anterior a la que viene)
    val previaNueva = proxima?.let { p -> vinetas.getOrNull(vinetas.indexOf(p) - 1) } ?: vinetas.lastOrNull()

    var pregunta by remember { mutableStateOf<Pair<String, VinetaEntity>?>(null) } // acción + viñeta cercana

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.tarjetaOscura.copy(alpha = 0.96f))
            .border(1.dp, c.bordeOscuro, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // 1) Qué viñeta viene (con flechitas para elegir otra) y el estado con la plataforma
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PRÓXIMA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = c.celeste, letterSpacing = 0.6.sp)
            if (proxima != null) {
                IconButton(onClick = { vinetas.getOrNull(vinetas.indexOf(proxima) - 1)?.let(onElegir) }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Viñeta anterior", tint = Color.White)
                }
                Text("#${proxima.numero}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace)
                IconButton(onClick = { vinetas.getOrNull(vinetas.indexOf(proxima) + 1)?.let(onElegir) }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Viñeta siguiente", tint = Color.White)
                }
                Text(
                    String.format(java.util.Locale.US, "%.3f km", proxima.distanciaTotal).replace('.', ','),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1), fontFamily = FontFamily.Monospace, maxLines = 1
                )
                if (proxima.verificadaTerreno) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Verified, contentDescription = "Verificada en el terreno", tint = c.verde, modifier = Modifier.size(16.dp))
                }
            } else {
                Spacer(Modifier.width(8.dp))
                Text("Todas verificadas", fontSize = 12.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold, maxLines = 1)
            }
            // (El estado con la plataforma está en el botón de colores de arriba)
        }

        // 2) A cuántos metros está el auto
        Text(
            when {
                !hayGps -> "Sin señal de GPS"
                proxima == null -> ""
                !tieneCoord -> "Sin coordenada: taqueala cuando llegues"
                else -> "Estás a ${formatoMetros(distanciaM!!)}" + (if (odo.precisionMetros > 0) "  ·  GPS ±${odo.precisionMetros.roundToInt()} m" else "")
            },
            fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (distanciaM != null && distanciaM <= 30) c.verde else Color.White
        )

        Spacer(Modifier.height(5.dp))
        // 3) Los tres botones: ícono arriba y una palabra abajo, para que entren siempre
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonGrande("TAQUEAR", Icons.Default.MyLocation, c.celeste, Color(0xFF0A0E17), Modifier.weight(1f), enabled = proxima != null && hayGps) {
                val p = proxima ?: return@BotonGrande
                val cerca = vinetaCercana(p.id)
                if (cerca != null) pregunta = "taquear" to cerca else onTaquear(p)
            }
            BotonGrande("ESTÁ BIEN", Icons.Default.CheckCircle, c.verde, Color(0xFF0A0E17), Modifier.weight(1f), enabled = proxima != null && tieneCoord) {
                proxima?.let(onEstaBien)
            }
            BotonGrande("NUEVA", Icons.Default.AddLocationAlt, c.amarillo, Color(0xFF0A0E17), Modifier.weight(1f), enabled = hayGps) {
                val cerca = vinetaCercana(null)
                if (cerca != null) pregunta = "nueva" to cerca else onNueva(previaNueva)
            }
        }
        if (proxima?.latitudAnterior != null && proxima.longitudAnterior != null) {
            TextButton(onClick = { onVolverAnterior(proxima) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                Icon(Icons.Default.Undo, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Volver a la coordenada anterior de la #${proxima.numero}", fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 1)
            }
        }
    }

    // "¿Es la viñeta N?" — para no tener dos viñetas en el mismo lugar
    pregunta?.let { (accion, cerca) ->
        AlertDialog(
            onDismissRequest = { pregunta = null },
            title = { Text("¿Es la viñeta ${cerca.numero}?", fontWeight = FontWeight.Black) },
            text = {
                Text("El auto está a menos de 30 m de la viñeta ${cerca.numero}. Si es esa, se taquea esa en lugar de " +
                    (if (accion == "nueva") "crear otra." else "la #${proxima?.numero}."))
            },
            confirmButton = {
                Button(onClick = { pregunta = null; onElegir(cerca); onTaquear(cerca) }) { Text("SÍ, ES LA ${cerca.numero}", fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pregunta = null
                    if (accion == "nueva") onNueva(previaNueva) else proxima?.let(onTaquear)
                }) { Text(if (accion == "nueva") "NO, ES NUEVA" else "NO, ES LA #${proxima?.numero}", fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
private fun Chip(texto: String, color: Color) {
    Text(
        texto, fontSize = 9.sp, fontWeight = FontWeight.Black, color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun BotonGrande(texto: String, icono: ImageVector, fondo: Color, tinta: Color, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) fondo else fondo.copy(alpha = 0.3f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Black, color = tinta, maxLines = 1, softWrap = false)
    }
}

private fun metros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun formatoMetros(m: Double): String =
    if (m < 1000) "${m.roundToInt()} m" else String.format(java.util.Locale.US, "%.2f km", m / 1000).replace('.', ',')
