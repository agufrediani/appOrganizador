package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =============================================================================================
// DIRECTOR DE CARRERA — copia de la pestaña de la plataforma (ESPECIFICACION_PANTALLAS.md,
// Parte D §3): solicitudes de enganche, encabezado, plantillas rápidas, contraseñas del libro,
// emitir comunicado (con el cambio de horario del cronograma), Bandera Roja e historial.
// =============================================================================================

private val AZUL_D = Color(0xFF0284C7)
private val CELESTE_D = Color(0xFF00B4D8)
private val NEGRO_D = Color(0xFF0F172A)
private val SLATE_50_D = Color(0xFFF8FAFC)
private val SLATE_100_D = Color(0xFFF1F5F9)
private val SLATE_200_D = Color(0xFFE2E8F0)
private val SLATE_300_D = Color(0xFFCBD5E1)
private val SLATE_400_D = Color(0xFF94A3B8)
private val SLATE_500_D = Color(0xFF64748B)
private val SLATE_600_D = Color(0xFF475569)
private val SLATE_700_D = Color(0xFF334155)
private val AMBAR_400_D = Color(0xFFFBBF24)
private val AMBAR_700_D = Color(0xFFB45309)
private val ESMERALDA_D = Color(0xFF10B981)
private val ROSE_500_D = Color(0xFFF43F5E)
private val ROSE_600_D = Color(0xFFE11D48)
private val ROSE_700_D = Color(0xFFBE123C)
private val ROSE_800_D = Color(0xFF9F1239)
private val ROSE_50_D = Color(0xFFFFF1F2)
private val VERDE_EMITIDO = Color(0xFF047857)

private fun JsonObject.v(k: String): JsonElement? = (get(k) ?: get(k.replaceFirstChar { it.uppercase() }))?.takeIf { !it.isJsonNull }
private fun JsonObject.s(k: String): String = try { v(k)?.asString ?: "" } catch (e: Exception) { "" }
private fun JsonObject.i(k: String): Int? = try { v(k)?.asInt } catch (e: Exception) { null }
private fun JsonObject.lista(k: String): List<JsonObject> =
    v(k)?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }.orEmpty()

private fun nombreDiaD(e: JsonObject) = e.s("nombre").ifBlank { "Día ${e.i("numero") ?: ""}" }

/** Fecha y hora local como la web ("7/11, 10:10 a. m."). */
private fun fechaHoraD(iso: String): String = try {
    val t = if (iso.endsWith("Z") || iso.contains('+')) iso else iso + "Z"
    val instante = java.time.OffsetDateTime.parse(t).toInstant()
    java.time.format.DateTimeFormatter.ofPattern("d/M, h:mm a", java.util.Locale("es", "AR"))
        .withZone(java.time.ZoneId.systemDefault()).format(instante)
} catch (e: Exception) { "" }

private fun horaLocalD(iso: String): String = try {
    val t = if (iso.endsWith("Z") || iso.contains('+')) iso else iso + "Z"
    java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault()).format(java.time.OffsetDateTime.parse(t).toInstant())
} catch (e: Exception) { "" }

private fun horaValida(t: String): String? {
    val s = t.trim().replace('.', ':')
    val p = if (':' in s) s.split(':') else if (s.length in 3..4) listOf(s.dropLast(2), s.takeLast(2)) else return null
    val h = p.getOrNull(0)?.toIntOrNull() ?: return null
    val m = p.getOrNull(1)?.toIntOrNull() ?: return null
    if (h !in 0..47 || m !in 0..59) return null
    return String.format("%02d:%02d", h, m)
}
private fun minutosHora(t: String): Int? = horaValida(t)?.let { it.take(2).toInt() * 60 + it.takeLast(2).toInt() }
private fun sumarD(hora: String, d: Int): String? = minutosHora(hora)?.let { (it + d).coerceIn(0, 47 * 60 + 59) }?.let { String.format("%02d:%02d", it / 60, it % 60) }
private fun mascaraD(t: String): String { val dig = t.filter { it.isDigit() }.take(4); return if (dig.length > 2) dig.dropLast(2) + ":" + dig.takeLast(2) else dig }

private val TIPOS_D = listOf(
    "COMUNICADO" to "COMUNICADO GENERAL",
    "PRECAUCION" to "PRECAUCIÓN EN PISTA",
    "BANDERA_AMARILLA" to "BANDERA AMARILLA (30 km/h)",
    "NEUTRALIZACION" to "TRAMO NEUTRALIZADO",
    "CRONOMETRAJE" to "COMISARIATO / TIEMPOS"
)

private data class Plantilla(val boton: String, val tipo: String, val texto: String, val color: Color, val fondo: Color)
private val PLANTILLAS = listOf(
    Plantilla("Largada Demorada 5 min", "COMUNICADO", "Largada demorada 5 minutos por razones operativas y de seguridad.", Color(0xFF0369A1), Color(0xFF0EA5E9)),
    Plantilla("Precaución en Pista", "PRECAUCION", "Precaución: animal suelto / obstáculo en camino. Reducir velocidad.", Color(0xFFB45309), Color(0xFFF59E0B)),
    Plantilla("Bandera Amarilla", "BANDERA_AMARILLA", "Bandera Amarilla en sector: velocidad máxima obligatoria 30 km/h.", Color(0xFFA16207), Color(0xFFEAB308)),
    Plantilla("Tramo Neutralizado", "NEUTRALIZACION", "Tramo neutralizado: continuar en régimen de enlace hasta CH fin.", Color(0xFF7E22CE), Color(0xFFA855F7)),
    Plantilla("Aviso Cronometraje", "CRONOMETRAJE", "Comisariato: tiempos y carnet de control bajo verificación técnica.", Color(0xFF047857), Color(0xFF10B981)),
    Plantilla("Pista Habilitada", "COMUNICADO", "Pista 100% habilitada y despejada. Largada confirmada a horario.", Color(0xFF0F766E), Color(0xFF14B8A6))
)

private data class ConfirmacionD(val texto: String, val accion: () -> Unit)

@Composable
fun DirectorCarreraScreen(viewModel: PlataformaViewModel) {
    val c = ColoresPlataforma
    val chico = tamanoPantalla() == TamanoPantalla.CHICO
    val reenganches by viewModel.reenganches.collectAsState()
    val mensajes by viewModel.mensajes.collectAsState()
    val detalle by viewModel.detalleRally.collectAsState()
    val itinerario by viewModel.itinerario.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val alcance = rememberCoroutineScope()

    var confirmar by remember { mutableStateOf<ConfirmacionD?>(null) }
    var panelEnganche by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<String?>(null) }

    // Comunicado
    var tipo by remember { mutableStateOf("COMUNICADO") }
    var destino by remember { mutableStateOf<Int?>(null) }
    var texto by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    // Cambio de horario (CambioCronograma)
    var sectorCambio by remember { mutableStateOf<Int?>(null) }
    var horaNueva by remember { mutableStateOf("") }
    // Bandera roja
    var motivoRoja by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.cargarCronograma()
        viewModel.cargarReenganches()
        while (true) { viewModel.cargarMensajes(); delay(10_000) }
    }
    LaunchedEffect(banner) { if (banner != null) { delay(8_000); banner = null } }

    val tramosRally = detalle?.lista("tramos").orEmpty()
    val etapasDetalle = detalle?.lista("etapas").orEmpty()
    val inscripciones = detalle?.lista("inscripciones").orEmpty()
    // Todos los sectores de todos los días, para el cambio de horario
    val sectores = itinerario?.lista("etapas").orEmpty().flatMap { e ->
        e.lista("sectores").map { s -> e to s }
    }
    fun descripcionSector(e: JsonObject, s: JsonObject): String = buildString {
        append("Sector ${s.i("numero") ?: ""}")
        s.i("peNumero")?.let { append(" · P.E. $it") }
        s.v("tramo")?.takeIf { it.isJsonObject }?.asJsonObject?.let { append(" · ${it.s("nombre")}") }
    }
    val elegidoCambio = sectores.firstOrNull { it.second.i("id") == sectorCambio }
    val horaActualCambio = elegidoCambio?.second?.let { it.s("horaSalidaCalculada").ifBlank { it.s("horaSalida") } } ?: ""
    val hayCambio = elegidoCambio != null && horaValida(horaNueva) != null && horaValida(horaNueva) != horaValida(horaActualCambio)

    fun emitir() {
        if (texto.isBlank()) { viewModel.mostrarMensaje("Por favor ingrese el texto del mensaje a emitir."); return }
        enviando = true
        alcance.launch {
            val rallyId = viewModel.idRally()
            if (rallyId == null) { enviando = false; return@launch }
            val enviarMensaje: () -> Unit = {
                viewModel.pedir("POST", "/api/control-carrera/emitir-mensaje", JsonObject().apply {
                    addProperty("rallyId", rallyId)
                    if (destino == null) add("tramoId", JsonNull.INSTANCE) else addProperty("tramoId", destino)
                    addProperty("tipo", tipo)
                    addProperty("mensaje", texto.trim())
                    addProperty("prioridad", if (tipo == "BANDERA_ROJA") "CRITICA" else "ALTA")
                }, recargarCronograma = false) { ok, resp ->
                    enviando = false
                    if (ok) {
                        val m = resp?.takeIf { it.isJsonObject }?.asJsonObject?.s("mensaje")?.ifBlank { null } ?: "Mensaje emitido exitosamente a toda la caravana"
                        banner = "[$tipo] $m"
                        texto = ""
                        if (hayCambio) {
                            com.example.roadbookorganizador.ui.components.AvisoSuperior.mostrar("Cronograma actualizado: ${horaValida(horaActualCambio) ?: horaActualCambio} → ${horaValida(horaNueva)}")
                            sectorCambio = null; horaNueva = ""
                            viewModel.cargarCronograma()
                        }
                        viewModel.cargarMensajes()
                    }
                }
            }
            if (hayCambio) {
                // Primero el horario: si falla, no se emite nada
                viewModel.pedir("PUT", "/api/sectores/${sectorCambio}/hora-salida", JsonObject().apply { addProperty("horaSalida", horaValida(horaNueva)) },
                    recargarCronograma = false) { ok, _ -> if (ok) enviarMensaje() else enviando = false }
            } else enviarMensaje()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (chico) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            banner?.let { b ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ROSE_600_D).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(b, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("CERRAR AVISO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Color.White, RoundedCornerShape(8.dp)).clickable { banner = null }.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }

            // ---------------- 3.1 Solicitudes de enganche ----------------
            TarjetaD { SolicitudesEnganche(reenganches, viewModel, onCargar = { panelEnganche = true }, onConfirmar = { t, a -> confirmar = ConfirmacionD(t, a) }) }

            // ---------------- 3.2 a 3.7 Dirección de Carrera ----------------
            TarjetaD {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x33F59E0B)).border(1.dp, Color(0x66F59E0B), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = AMBAR_700_D, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("DIRECCIÓN DE CARRERA", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.texto())
                        Text("Centro de Comunicaciones Oficiales: Mensajes a la caravana, alertas operativas, comisariato y Bandera Roja", fontSize = 12.sp, color = SLATE_500_D)
                    }
                }
                Spacer(Modifier.height(12.dp)); HorizontalDivider(color = c.borde()); Spacer(Modifier.height(14.dp))

                // 3.3 Plantillas rápidas
                RotuloD("Plantillas Rápidas del Director de Carrera (1-Clic para precargar o emitir):")
                FlowRowD {
                    PLANTILLAS.forEach { p ->
                        Text(p.boton, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.color,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(p.fondo.copy(alpha = 0.10f)).border(1.dp, p.fondo.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                                .clickable { tipo = p.tipo; texto = p.texto }.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }

                // 3.4 Contraseñas del libro de ruta
                val conClave = etapasDetalle.filter { it.s("claveLibro").isNotBlank() }
                if (conClave.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    RotuloD("Contraseñas del libro de ruta (se cargan en el comunicado para publicarlas a la hora de entrega):")
                    FlowRowD {
                        conClave.forEach { e ->
                            Row(
                                Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, SLATE_300_D, RoundedCornerShape(10.dp)).clickable {
                                    tipo = "COMUNICADO"; destino = null
                                    val nombre = e.s("nombre").ifBlank { "la etapa ${e.i("numero") ?: ""}" }
                                    texto = "Contraseña del libro de ruta de $nombre: ${e.s("claveLibro")}"
                                }.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(e.s("nombre").ifBlank { "Etapa ${e.i("numero") ?: ""}" }.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.texto())
                                Text(e.s("claveLibro"), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = c.texto())
                                if (e.s("horaEntregaLibro").isNotBlank()) Text("entrega ${e.s("horaEntregaLibro").take(5)}", fontSize = 12.sp, color = c.texto())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // 3.5 Emitir comunicado y 3.6 Bandera roja (7 y 5 columnas)
                val comunicado: @Composable (Modifier) -> Unit = { m ->
                    Column(m.clip(RoundedCornerShape(20.dp)).background(SLATE_50_D).border(1.dp, SLATE_300_D, RoundedCornerShape(20.dp)).padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = AZUL_D, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("EMITIR COMUNICADO OFICIAL A LA CARAVANA", fontSize = 14.sp, fontWeight = FontWeight.Black, color = NEGRO_D, modifier = Modifier.weight(1f))
                            Text("Emisión Instantánea", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SLATE_500_D)
                        }
                        Spacer(Modifier.height(10.dp)); HorizontalDivider(color = SLATE_300_D); Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Tipo de Mensaje:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_600_D); Spacer(Modifier.height(4.dp))
                                SelectorD(TIPOS_D.firstOrNull { it.first == tipo }?.second ?: tipo, TIPOS_D.map { it.second }) { k -> tipo = TIPOS_D[k].first }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Destinatarios:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_600_D); Spacer(Modifier.height(4.dp))
                                val ops = listOf("Toda la caravana (Todos los Tramos)") + tramosRally.map { "${it.s("identificador")} - ${it.s("nombre")}" }
                                val actual = tramosRally.firstOrNull { it.i("id") == destino }?.let { "${it.s("identificador")} - ${it.s("nombre")}" } ?: ops[0]
                                SelectorD(actual, ops, tinta = AZUL_D) { k -> destino = if (k == 0) null else tramosRally[k - 1].i("id") }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // Cambio de horario en el cronograma (opcional)
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SLATE_50_D).border(1.dp, Color(0x66F59E0B), RoundedCornerShape(14.dp)).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = AMBAR_700_D, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp))
                                Text("CAMBIAR HORARIO EN EL CRONOGRAMA (OPCIONAL)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AMBAR_700_D)
                            }
                            Spacer(Modifier.height(8.dp))
                            if (sectores.isEmpty()) Text("El cronograma todavía no tiene sectores. Armalo en la pestaña Cronograma.", fontSize = 12.sp, color = SLATE_500_D)
                            else {
                                val ops = listOf("— Sin cambio de horario —") + sectores.map { (e, s) ->
                                    "${nombreDiaD(e)} · ${descripcionSector(e, s)} · ${s.s("horaSalidaCalculada").ifBlank { s.s("horaSalida") }.ifBlank { "sin hora" }}"
                                }
                                val actual = elegidoCambio?.let { (e, s) -> ops[sectores.indexOf(elegidoCambio) + 1] } ?: ops[0]
                                SelectorD(actual, ops) { k ->
                                    sectorCambio = sectores.getOrNull(k - 1)?.second?.i("id")
                                    horaNueva = sectores.getOrNull(k - 1)?.second?.let { it.s("horaSalidaCalculada").ifBlank { it.s("horaSalida") } } ?: ""
                                }
                                if (elegidoCambio != null) {
                                    Spacer(Modifier.height(8.dp))
                                    FlowRowD {
                                        Text("Largaba ${horaActualCambio.ifBlank { "—" }} →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_700_D, modifier = Modifier.padding(top = 6.dp))
                                        listOf(-10, -5, 5, 10, 15, 30).forEach { d ->
                                            Text(if (d > 0) "+$d" else "−${-d}", fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace,
                                                color = if (d > 0) AMBAR_700_D else SLATE_700_D,
                                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, SLATE_300_D, RoundedCornerShape(8.dp))
                                                    .clickable { sumarD(horaNueva.ifBlank { horaActualCambio }, d)?.let { horaNueva = it } }.padding(horizontal = 9.dp, vertical = 5.dp))
                                        }
                                        CampoHoraD(horaNueva) { horaNueva = it }
                                    }
                                    if (hayCambio) {
                                        Spacer(Modifier.height(8.dp))
                                        Text("Al emitir: ${horaValida(horaActualCambio) ?: horaActualCambio} → ${horaValida(horaNueva)}. Lo que viene después se corre solo.",
                                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AMBAR_700_D)
                                        Spacer(Modifier.height(6.dp))
                                        Text("Escribir el comunicado con este cambio", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AMBAR_700_D,
                                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0x66F59E0B), RoundedCornerShape(8.dp)).clickable {
                                                val a = horaValida(horaActualCambio) ?: horaActualCambio
                                                val b = horaValida(horaNueva) ?: horaNueva
                                                val dif = (minutosHora(b) ?: 0) - (minutosHora(a) ?: 0)
                                                val (e, s) = elegidoCambio
                                                tipo = "COMUNICADO"
                                                texto = "Se informa que la largada del 1° auto en ${descripcionSector(e, s)} ${if (dif > 0) "se demora" else "se adelanta"} ${kotlin.math.abs(dif)} minutos: pasa de las $a a las $b hs. Los horarios siguientes se corren en consecuencia."
                                            }.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Mensaje Oficial (se transmitirá a todas las tablets y vivac):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_600_D)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(texto, { texto = it }, minLines = 3, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ejemplo: Se informa a todas las tripulaciones que la largada de la PE 2 se posterga 10 minutos por razones meteorológicas...", fontSize = 12.sp) })
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${texto.length} caracteres", fontSize = 11.sp, color = SLATE_500_D, modifier = Modifier.weight(1f))
                            Button(onClick = { emitir() }, enabled = !enviando, shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CELESTE_D, contentColor = Color(0xFF0A0E17))) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                                Text(if (enviando) "Transmitiendo..." else "EMITIR COMUNICADO OFICIAL", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                val roja: @Composable (Modifier) -> Unit = { m ->
                    Column(m.clip(RoundedCornerShape(20.dp)).background(ROSE_50_D).border(2.dp, ROSE_500_D.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ROSE_700_D, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                            Text("PROTOCOLO DE EMERGENCIA: BANDERA ROJA", fontSize = 14.sp, fontWeight = FontWeight.Black, color = ROSE_700_D)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Alarma acústica y visual instantánea en las tablets de todos los competidores. Orden de detención total e inmediata de la prueba especial.",
                            fontSize = 12.sp, color = ROSE_800_D)
                        Spacer(Modifier.height(12.dp))
                        Text("Motivo Oficial de la Detención:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ROSE_700_D); Spacer(Modifier.height(4.dp))
                        OutlinedTextField(motivoRoja, { motivoRoja = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ej: Accidente o bloqueo de camino en km 4.2", fontSize = 12.sp) })
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            confirmar = ConfirmacionD("⚠️ ¿CONFIRMAR EMISIÓN DE BANDERA ROJA?\n\nEsta acción enviará una orden de detención inmediata a todas las tripulaciones conectadas a la red.") {
                                alcance.launch {
                                    val rallyId = viewModel.idRally() ?: return@launch
                                    viewModel.pedir("POST", "/api/control-carrera/bandera-roja", JsonObject().apply {
                                        addProperty("rallyId", rallyId)
                                        add("tramoId", JsonNull.INSTANCE) // ver la pregunta en CAMBIOS (revisión 27)
                                        addProperty("motivo", motivoRoja.trim())
                                    }, recargarCronograma = false) { ok, resp ->
                                        if (ok) {
                                            banner = resp?.takeIf { it.isJsonObject }?.asJsonObject?.s("mensaje")?.ifBlank { null } ?: "BANDERA ROJA EMITIDA"
                                            motivoRoja = ""
                                            viewModel.cargarMensajes()
                                        }
                                    }
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ROSE_600_D, contentColor = Color.White)) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                            Text("EMITIR BANDERA ROJA (DETENCIÓN TOTAL)", fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
                        }
                    }
                }
                if (chico) {
                    comunicado(Modifier.fillMaxWidth()); Spacer(Modifier.height(16.dp)); roja(Modifier.fillMaxWidth())
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                        comunicado(Modifier.weight(7f)); roja(Modifier.weight(5f))
                    }
                }

                // 3.7 Historial
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("HISTORIAL DE COMUNICADOS Y BANDERAS EMITIDAS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = SLATE_700_D)
                    Spacer(Modifier.width(8.dp))
                    Text("${mensajes.size} registros", fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = SLATE_600_D,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(SLATE_200_D).padding(horizontal = 8.dp, vertical = 2.dp))
                    Spacer(Modifier.weight(1f))
                    Text("Actualizar Historial", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AZUL_D, modifier = Modifier.clickable { viewModel.cargarMensajes() })
                }
                Spacer(Modifier.height(8.dp)); HorizontalDivider(color = c.borde()); Spacer(Modifier.height(10.dp))
                if (mensajes.isEmpty()) {
                    Text("No se han emitido comunicados ni alertas de seguridad en esta sesión.", fontSize = 12.sp, color = SLATE_500_D,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SLATE_50_D).border(1.dp, SLATE_300_D, RoundedCornerShape(14.dp)).padding(16.dp))
                } else {
                    Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        mensajes.forEach { m ->
                            val t = m.s("tipoAlerta")
                            val esRoja = t == "BANDERA_ROJA"
                            val colorChip = when (t) {
                                "BANDERA_ROJA" -> ROSE_700_D
                                "PRECAUCION", "BANDERA_AMARILLA" -> AMBAR_700_D
                                "NEUTRALIZACION" -> Color(0xFF7E22CE)
                                else -> Color(0xFF0369A1)
                            }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (esRoja) ROSE_50_D else SLATE_50_D).border(1.dp, SLATE_200_D, RoundedCornerShape(14.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t, fontSize = 10.sp, fontWeight = FontWeight.Black, color = colorChip,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(colorChip.copy(alpha = 0.12f)).border(1.dp, colorChip.copy(alpha = 0.3f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp))
                                Spacer(Modifier.width(10.dp))
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Text(m.s("descripcion"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NEGRO_D, modifier = Modifier.weight(1f, fill = false))
                                    m.i("tramoId")?.let { Spacer(Modifier.width(6.dp)); Text("[Tramo #$it]", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = SLATE_500_D) }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(m.s("horaCorta").ifBlank { horaLocalD(m.s("fechaHora")) }, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SLATE_600_D)
                                Spacer(Modifier.width(8.dp))
                                Text("EMITIDO", fontSize = 9.sp, fontWeight = FontWeight.Black, color = VERDE_EMITIDO,
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(VERDE_EMITIDO.copy(alpha = 0.12f)).border(1.dp, VERDE_EMITIDO.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.Delete, contentDescription = "Borrar el mensaje", tint = ROSE_500_D, modifier = Modifier.size(20.dp).clickable {
                                    confirmar = ConfirmacionD("¿Borrar este mensaje del historial?") {
                                        viewModel.pedir("DELETE", "/api/control-carrera/mensajes/${m.i("id")}", recargarCronograma = false) { ok, _ ->
                                            if (!ok) viewModel.mostrarMensaje("No se pudo borrar el mensaje")
                                            viewModel.cargarMensajes()
                                        }
                                    }
                                })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }

        if (panelEnganche) {
            PanelLateralD(onCerrar = { panelEnganche = false }) {
                FormularioEnganche(inscripciones, itinerario, viewModel, onCerrar = { panelEnganche = false })
            }
        }
    }

    confirmar?.let { cf ->
        AlertDialog(
            onDismissRequest = { confirmar = null },
            text = { Text(cf.texto) },
            confirmButton = { Button(onClick = { confirmar = null; cf.accion() }) { Text("Aceptar", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Cancelar") } }
        )
    }
    mensaje?.let { m ->
        AlertDialog(
            onDismissRequest = { viewModel.cerrarMensaje() },
            text = { Text(m) },
            confirmButton = { TextButton(onClick = { viewModel.cerrarMensaje() }) { Text("Aceptar", fontWeight = FontWeight.Bold) } }
        )
    }
}

// ------------------------------------------------------------------ 3.1 enganches
@Composable
private fun SolicitudesEnganche(lista: List<JsonObject>, viewModel: PlataformaViewModel, onCargar: () -> Unit, onConfirmar: (String, () -> Unit) -> Unit) {
    val c = ColoresPlataforma
    var filtro by remember { mutableStateOf("PENDIENTE") }
    val observaciones = remember { mutableStateMapOf<Int, String>() }
    val n = { e: String -> lista.count { it.s("estado") == e } }
    val pendientes = n("PENDIENTE")

    fun resolver(r: JsonObject, estado: String) {
        val id = r.i("id") ?: return
        val obs = observaciones[id] ?: r.s("observacion")
        viewModel.pedir("PUT", "/api/reenganches/$id/resolver", JsonObject().apply { addProperty("estado", estado); addProperty("observacion", obs) },
            recargarCronograma = false) { _, _ -> observaciones.remove(id); viewModel.cargarReenganches() }
    }

    FlowRowD {
        Column(Modifier.widthIn(max = 620.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SOLICITUDES DE ENGANCHE", fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.texto())
                if (pendientes > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("$pendientes ${if (pendientes == 1) "pendiente" else "pendientes"}".uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(AMBAR_400_D).padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Text("Tripulaciones que abandonaron y piden volver a largar. Se aceptan o rechazan acá y queda registrado.", fontSize = 12.sp, color = SLATE_500_D)
        }
        Text("+ CARGAR SOLICITUD", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(AZUL_D).clickable { onCargar() }.padding(horizontal = 16.dp, vertical = 9.dp))
    }
    Spacer(Modifier.height(12.dp)); HorizontalDivider(color = c.borde()); Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("PENDIENTE" to "Pendientes (${n("PENDIENTE")})", "ACEPTADO" to "Aceptados (${n("ACEPTADO")})",
            "RECHAZADO" to "Rechazados (${n("RECHAZADO")})", "TODAS" to "Todas (${lista.size})").forEach { (clave, rot) ->
            val sel = filtro == clave
            Text(rot.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (sel) Color.White else SLATE_600_D,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) AZUL_D else SLATE_100_D).clickable { filtro = clave }.padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
    Spacer(Modifier.height(12.dp))
    val visibles = if (filtro == "TODAS") lista else lista.filter { it.s("estado") == filtro }
    if (visibles.isEmpty()) Text(if (filtro == "PENDIENTE") "No hay solicitudes pendientes." else "No hay solicitudes acá.", fontSize = 13.sp, color = SLATE_500_D)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        visibles.forEach { r ->
            val id = r.i("id") ?: return@forEach
            val estado = r.s("estado")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SLATE_50_D).border(1.dp, SLATE_200_D, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("N°", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SLATE_500_D)
                        Text(r.s("numeroParticipante"), fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = NEGRO_D)
                        if (r.s("clase").isNotBlank()) Text(r.s("clase").uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = NEGRO_D)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text((r.s("piloto") + if (r.s("navegante").isNotBlank()) " / ${r.s("navegante")}" else "").uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Black, color = NEGRO_D)
                        val abandono = r.s("seccionAbandonoTexto").ifBlank { r.i("seccionAbandono")?.let { "Sección $it" } ?: "—" }
                        Etiquetado("Abandonó en:", abandono)
                        Etiquetado("Motivo:", r.s("motivo"))
                        if (r.s("aclaracion").isNotBlank()) Etiquetado("Aclaración:", r.s("aclaracion"))
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        val (rot, fondo, tinta) = when (estado) {
                            "ACEPTADO" -> Triple("Aceptado", ESMERALDA_D, Color.Black)
                            "RECHAZADO" -> Triple("Rechazado", ROSE_600_D, Color.White)
                            else -> Triple("Pendiente", AMBAR_400_D, Color.Black)
                        }
                        Text(rot.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = tinta,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = 8.dp, vertical = 2.dp))
                        Text("${if (r.s("origen") == "APP") "Desde la app" else "Cargada en la plataforma"} · ${fechaHoraD(r.s("creadoUtc"))}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SLATE_500_D)
                        if (r.s("resueltoUtc").isNotBlank())
                            Text("${if (estado == "ACEPTADO") "Aceptó" else "Rechazó"} ${r.s("resueltoPorNombre").ifBlank { "—" }} · ${fechaHoraD(r.s("resueltoUtc"))}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SLATE_500_D)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("OBSERVACIÓN DE LA ORGANIZACIÓN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SLATE_500_D)
                Spacer(Modifier.height(4.dp))
                val obs = observaciones[id] ?: r.s("observacion")
                FlowRowD {
                    OutlinedTextField(obs, { observaciones[id] = it }, singleLine = true, modifier = Modifier.widthIn(min = 260.dp, max = 560.dp))
                    if (estado == "PENDIENTE") {
                        BotonD("✓ ACEPTAR", ESMERALDA_D, Color.Black) { resolver(r, "ACEPTADO") }
                        BotonD("✕ RECHAZAR", ROSE_600_D, Color.White) {
                            if (obs.isBlank()) onConfirmar("¿Rechazar la solicitud del N° ${r.s("numeroParticipante")} sin escribir el motivo del rechazo?") { resolver(r, "RECHAZADO") }
                            else resolver(r, "RECHAZADO")
                        }
                    } else {
                        if (obs != r.s("observacion")) BotonD("GUARDAR OBSERVACIÓN", AZUL_D, Color.White) { resolver(r, estado) }
                        Text("↺ VOLVER A PENDIENTE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NEGRO_D,
                            modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, SLATE_300_D, RoundedCornerShape(12.dp)).clickable { resolver(r, "PENDIENTE") }.padding(horizontal = 12.dp, vertical = 9.dp))
                    }
                    Box(Modifier.padding(top = 4.dp).size(38.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, SLATE_300_D, RoundedCornerShape(12.dp)).clickable {
                        onConfirmar("¿Borrar la solicitud del N° ${r.s("numeroParticipante")}? No queda registro.") {
                            viewModel.pedir("DELETE", "/api/reenganches/$id", recargarCronograma = false) { _, _ -> viewModel.cargarReenganches() }
                        }
                    }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, contentDescription = "Borrar la solicitud", tint = ROSE_500_D, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Etiquetado(rotulo: String, valor: String) {
    Row {
        Text(rotulo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_500_D)
        Spacer(Modifier.width(4.dp))
        Text(valor, fontSize = 12.sp, color = NEGRO_D)
    }
}

// ----------------------------------------------------- panel "Formulario de enganche"
@Composable
private fun FormularioEnganche(inscripciones: List<JsonObject>, itinerario: JsonObject?, viewModel: PlataformaViewModel, onCerrar: () -> Unit) {
    val c = ColoresPlataforma
    val alcance = rememberCoroutineScope()
    var inscripcionId by remember { mutableStateOf<Int?>(null) }
    var numero by remember { mutableStateOf("") }
    var clase by remember { mutableStateOf("") }
    var piloto by remember { mutableStateOf("") }
    var navegante by remember { mutableStateOf("") }
    var seccion by remember { mutableStateOf<Int?>(null) }
    var seccionTexto by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var aclaracion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    // "Sección {n} · {Día}", desde el cronograma
    val secciones = itinerario?.lista("etapas").orEmpty().flatMap { e ->
        e.lista("filasFia").mapNotNull { it.i("seccion") }.distinct().sorted().map { it to nombreDiaD(e) }
    }

    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("FORMULARIO DE ENGANCHE", fontSize = 18.sp, fontWeight = FontWeight.Black, color = c.texto())
            Text("Los mismos datos que el formulario en papel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_500_D)
        }
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, SLATE_300_D, RoundedCornerShape(10.dp)).clickable { onCerrar() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = SLATE_600_D, modifier = Modifier.size(16.dp))
        }
    }
    Spacer(Modifier.height(16.dp))
    if (inscripciones.isNotEmpty()) {
        RotuloD("Elegir de los inscriptos (completa los datos)")
        val ops = listOf("— Elegí o completá a mano —") + inscripciones.map { "N° ${it.s("numeroAuto")} · ${it.s("pilotoNombre")} / ${it.s("naveganteNombre")}" }
        val actual = inscripciones.indexOfFirst { it.i("id") == inscripcionId }.let { if (it < 0) ops[0] else ops[it + 1] }
        SelectorD(actual, ops) { k ->
            val i = inscripciones.getOrNull(k - 1)
            inscripcionId = i?.i("id")
            if (i != null) { numero = i.s("numeroAuto"); clase = i.s("categoria"); piloto = i.s("pilotoNombre"); navegante = i.s("naveganteNombre") }
        }
        Spacer(Modifier.height(12.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) { RotuloD("Participante N°"); OutlinedTextField(numero, { numero = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        Column(Modifier.weight(1f)) { RotuloD("Clase"); OutlinedTextField(clase, { clase = it.uppercase() }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)) }
    }
    Spacer(Modifier.height(10.dp))
    RotuloD("Piloto (nombre y apellido)")
    OutlinedTextField(piloto, { piloto = it.uppercase() }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
    Spacer(Modifier.height(10.dp))
    RotuloD("Navegante (nombre y apellido)")
    OutlinedTextField(navegante, { navegante = it.uppercase() }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
    Spacer(Modifier.height(10.dp))
    RotuloD("Sección en la que abandonó")
    if (secciones.isEmpty()) {
        OutlinedTextField(seccionTexto, { seccionTexto = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("El cronograma todavía no tiene secciones") })
    } else {
        val ops = listOf("— Elegí —") + secciones.map { (n, d) -> "Sección $n · $d" }
        val k0 = secciones.indexOfFirst { it.first == seccion && "Sección ${it.first} · ${it.second}" == seccionTexto }
        SelectorD(if (k0 < 0) ops[0] else ops[k0 + 1], ops) { k ->
            val s = secciones.getOrNull(k - 1)
            seccion = s?.first; seccionTexto = s?.let { "Sección ${it.first} · ${it.second}" } ?: ""
        }
    }
    Spacer(Modifier.height(10.dp))
    RotuloD("Motivo")
    OutlinedTextField(motivo, { motivo = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    RotuloD("Aclaración (de quien firma)")
    OutlinedTextField(aclaracion, { aclaracion = it.uppercase() }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
    error?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ROSE_600_D).padding(10.dp))
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
        Button(onClick = onCerrar, colors = ButtonDefaults.buttonColors(containerColor = SLATE_700_D, contentColor = Color.White), shape = RoundedCornerShape(12.dp)) { Text("Cancelar", fontWeight = FontWeight.Bold) }
        Button(onClick = {
            error = when {
                numero.isBlank() -> "Falta el número de participante."
                piloto.isBlank() -> "Falta el nombre del piloto."
                motivo.isBlank() -> "Falta el motivo."
                else -> null
            }
            if (error != null) return@Button
            guardando = true
            alcance.launch {
                val rallyId = viewModel.idRally() ?: run { guardando = false; return@launch }
                viewModel.pedir("POST", "/api/rallies/$rallyId/reenganches", JsonObject().apply {
                    if (inscripcionId == null) add("inscripcionId", JsonNull.INSTANCE) else addProperty("inscripcionId", inscripcionId)
                    addProperty("numeroParticipante", numero.trim())
                    addProperty("clase", clase.trim().uppercase())
                    addProperty("piloto", piloto.trim().uppercase())
                    addProperty("navegante", navegante.trim().uppercase())
                    if (seccion == null) add("seccionAbandono", JsonNull.INSTANCE) else addProperty("seccionAbandono", seccion)
                    addProperty("seccionAbandonoTexto", seccionTexto.trim())
                    addProperty("motivo", motivo.trim())
                    addProperty("aclaracion", aclaracion.trim().uppercase())
                }, recargarCronograma = false) { ok, _ -> guardando = false; if (ok) { viewModel.cargarReenganches(); onCerrar() } }
            }
        }, enabled = !guardando, colors = ButtonDefaults.buttonColors(containerColor = AZUL_D, contentColor = Color.White), shape = RoundedCornerShape(12.dp)) {
            Text(if (guardando) "Guardando…" else "GUARDAR SOLICITUD", fontWeight = FontWeight.Black)
        }
    }
}

// ------------------------------------------------------------------------- piezas
@Composable
private fun TarjetaD(contenido: @Composable ColumnScope.() -> Unit) {
    val c = ColoresPlataforma
    Column(
        Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(c.tarjeta())
            .border(1.dp, c.borde(), RoundedCornerShape(24.dp)).padding(20.dp),
        content = contenido
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowD(contenido: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { contenido() }
}

@Composable
private fun RotuloD(texto: String) {
    Text(texto.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = SLATE_600_D, letterSpacing = 0.4.sp)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun BotonD(texto: String, fondo: Color, tinta: Color, onClick: () -> Unit) {
    Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Black, color = tinta,
        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(fondo).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 9.dp))
}

@Composable
private fun SelectorD(actual: String, opciones: List<String>, tinta: Color = NEGRO_D, onElegir: (Int) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, SLATE_300_D, RoundedCornerShape(12.dp))
                .clickable { abierto = true }.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(actual, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tinta, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SLATE_600_D)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEachIndexed { k, o -> DropdownMenuItem(text = { Text(o, fontSize = 13.sp) }, onClick = { abierto = false; onElegir(k) }) }
        }
    }
}

@Composable
private fun CampoHoraD(valor: String, onCambio: (String) -> Unit) {
    Box(Modifier.width(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, SLATE_300_D, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        if (valor.isEmpty()) Text("hh:mm", fontSize = 12.sp, color = SLATE_400_D, fontFamily = FontFamily.Monospace)
        BasicTextField(valor, { onCambio(mascaraD(it)) }, singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NEGRO_D, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), cursorBrush = SolidColor(AZUL_D), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PanelLateralD(onCerrar: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    val c = ColoresPlataforma
    val chico = tamanoPantalla() == TamanoPantalla.CHICO
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onCerrar() }) {
        Column(
            Modifier.align(Alignment.TopEnd).fillMaxHeight().then(if (chico) Modifier.fillMaxWidth() else Modifier.width(420.dp)).shadow(12.dp).background(c.tarjeta())
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { }
                .verticalScroll(rememberScrollState()).padding(20.dp),
            content = contenido
        )
    }
}
