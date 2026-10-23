package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.ui.components.ColoresPlataforma
import com.example.roadbookorganizador.ui.components.TamanoPantalla
import com.example.roadbookorganizador.ui.components.tamanoPantalla
import com.example.roadbookorganizador.ui.viewmodel.PlataformaViewModel
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// Lectura de lo que manda la plataforma (acepta camelCase y PascalCase)
// ---------------------------------------------------------------------------------------------
private fun JsonObject.v(k: String): JsonElement? =
    (get(k) ?: get(k.replaceFirstChar { it.uppercase() }))?.takeIf { !it.isJsonNull }
private fun JsonObject.s(k: String): String = try { v(k)?.asString ?: "" } catch (e: Exception) { "" }
private fun JsonObject.i(k: String): Int? = try { v(k)?.asInt } catch (e: Exception) { null }
private fun JsonObject.d(k: String): Double? = try { v(k)?.asDouble } catch (e: Exception) { null }
private fun JsonObject.b(k: String): Boolean = try { v(k)?.asBoolean ?: false } catch (e: Exception) { false }
private fun JsonObject.o(k: String): JsonObject? = v(k)?.takeIf { it.isJsonObject }?.asJsonObject
private fun JsonObject.lista(k: String): List<JsonObject> =
    v(k)?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }.orEmpty()

private fun km(x: Double?): String = if (x == null) "" else String.format(java.util.Locale.US, "%.2f", x).replace('.', ',')
private fun minutos(m: Int?): String = if (m == null) "" else if (m >= 60) String.format("%d:%02d", m / 60, m % 60) else "$m'"

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val c = ColoresPlataforma
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.tarjeta())
            .border(1.dp, c.borde(), RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = contenido
    )
}

@Composable
private fun TituloPestana(icono: ImageVector, color: Color, titulo: String, subtitulo: String, derecha: @Composable RowScope.() -> Unit = {}) {
    val c = ColoresPlataforma
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.18f)).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        ) { Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = c.texto(), letterSpacing = 0.5.sp)
            if (subtitulo.isNotBlank()) Text(subtitulo, fontSize = 12.sp, color = c.apagado())
        }
        derecha()
    }
}

@Composable
private fun BotonRecargar(cargando: Boolean, onClick: () -> Unit) {
    val c = ColoresPlataforma
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.superficie())
            .border(1.dp, c.borde(), RoundedCornerShape(12.dp))
            .clickable(enabled = !cargando) { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        if (cargando) CircularProgressIndicator(strokeWidth = 2.dp, color = c.celeste, modifier = Modifier.size(14.dp))
        else Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = c.celeste, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("ACTUALIZAR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = c.texto())
    }
}

@Composable
private fun CartelError(texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1AF43F5E))
            .border(1.dp, Color(0x4DF43F5E), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFB7185))
    }
}

// =============================================================================================
// ROADBOOK: el roadbook de cada tramo para exportar desde la tablet. El libro de ruta en PDF
// (portada, reglamento, mapas) se arma en la plataforma.
// =============================================================================================
@Composable
fun RoadbookScreen(viewModel: PlataformaViewModel, onExportar: (Long) -> Unit) {
    val c = ColoresPlataforma
    val tramos by viewModel.tramos.collectAsState()
    val chico = tamanoPantalla() == TamanoPantalla.CHICO

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (chico) 12.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Tarjeta {
            TituloPestana(Icons.Default.Download, c.celeste, "Roadbook", "El roadbook de cada tramo: GPX, KMZ o JSON Frediani 2.1")
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.superficie()).padding(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = c.apagado(), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("El libro de ruta en PDF (portada, reglamento y mapas) se arma en la plataforma.", fontSize = 12.sp, color = c.apagado())
            }
        }
        Tarjeta {
            if (tramos.isEmpty()) Text("El rally no tiene tramos todavía.", fontSize = 12.sp, color = c.apagado())
            tramos.forEachIndexed { i, t ->
                if (i > 0) HorizontalDivider(color = c.borde())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    val esPe = t.tipo.equals("PE", true)
                    Text(
                        t.identificador, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (esPe) Color.White else c.texto(),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (esPe) Color(0xFFDC2626) else c.superficie()).padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.nombre, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.texto(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${km(t.distanciaTotalEstimada)} km", fontSize = 11.sp, color = c.apagado(), fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.celeste)
                            .clickable { onExportar(t.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        if (!chico) {
                            Spacer(Modifier.width(6.dp))
                            Text("EXPORTAR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
