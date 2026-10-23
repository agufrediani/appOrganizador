package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.ui.components.ColoresPlataforma
import com.example.roadbookorganizador.ui.components.TamanoPantalla
import com.example.roadbookorganizador.ui.components.tamanoPantalla
import com.example.roadbookorganizador.ui.theme.ThemeManager
import com.example.roadbookorganizador.ui.viewmodel.PlataformaViewModel
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

// =============================================================================================
// CRONOGRAMA — copia de la pestaña de la plataforma (ESPECIFICACION_PANTALLAS.md, Parte C):
// encabezado con "+ NUEVO DÍA", largada de PE y totales; pestañas por día; por cada día la
// planilla FIA que se escribe directo en las casillas, el detalle del sector (al tocar el N°),
// la fila para agregar, los anexos, las notas y los avisos; y los paneles laterales "Hora del
// 1° auto", "Emitir anexo", "Editar Etapa Oficial" y "Nueva Etapa Oficial".
// Los cálculos (horas, numeración, totales, apertura y cierre) los hace el servidor.
// =============================================================================================

// --------------------------------------------------------------------------------- colores
private val NEGRO = Color(0xFF0F172A)
private val AZUL = Color(0xFF0284C7)
private val AZUL_OSCURO = Color(0xFF0369A1)
private val CELESTE_ELEGIDO = Color(0xFFE0F2FE)
private val AZUL_PARADA = Color(0xFFDBEAFE)
private val GRIS_PARADA = Color(0xFFF1F5F9)
private val GRIS_PE = Color(0xFFF8FAFC)
private val AMARILLO_ANEXO = Color(0xFFFACC15)
private val AMBAR_600 = Color(0xFFD97706)
private val AMBAR_800 = Color(0xFF92400E)
private val AMBAR_50 = Color(0xFFFFFBEB)
private val ROSE_600 = Color(0xFFE11D48)
private val SLATE_300 = Color(0xFFCBD5E1)
private val SLATE_400 = Color(0xFF94A3B8)
private val SLATE_500 = Color(0xFF64748B)
private val SLATE_600 = Color(0xFF475569)
private val SLATE_700 = Color(0xFF334155)
private val SLATE_800 = Color(0xFF1E293B)
private val SLATE_50 = Color(0xFFF8FAFC)

// ------------------------------------------------------------------------- lectura del JSON
private fun JsonObject.v(k: String): JsonElement? = (get(k) ?: get(k.replaceFirstChar { it.uppercase() }))?.takeIf { !it.isJsonNull }
private fun JsonObject.s(k: String): String = try { v(k)?.asString ?: "" } catch (e: Exception) { "" }
private fun JsonObject.i(k: String): Int? = try { v(k)?.asInt } catch (e: Exception) { null }
private fun JsonObject.d(k: String): Double? = try { v(k)?.asDouble } catch (e: Exception) { null }
private fun JsonObject.b(k: String): Boolean = try { v(k)?.asBoolean ?: false } catch (e: Exception) { false }
private fun JsonObject.o(k: String): JsonObject? = v(k)?.takeIf { it.isJsonObject }?.asJsonObject
private fun JsonObject.lista(k: String): List<JsonObject> =
    v(k)?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }.orEmpty()
private fun JsonObject.textos(k: String): List<String> =
    v(k)?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { try { it.asString } catch (e: Exception) { null } }.orEmpty()

// --------------------------------------------------------------------------------- formatos
private fun km2(v: Double?): String = if (v == null) "" else String.format(java.util.Locale.US, "%.2f", v).replace('.', ',')
private fun horasMin(m: Int?): String = if (m == null) "" else "${m / 60}:${String.format("%02d", m % 60)}"
private fun pct(pe: Double, total: Double): String =
    if (total > 0) String.format(java.util.Locale.US, "%.2f", pe * 100 / total).replace('.', ',') + "%" else ""
private fun entreParentesis(v: Double?): String = if (v == null) "" else "(${km2(v)})"

private val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
private val DIAS = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

private fun fechaLarga(iso: String): String = try {
    val d = java.time.LocalDate.parse(iso.take(10))
    "${DIAS[d.dayOfWeek.value % 7]} ${d.dayOfMonth} de ${MESES[d.monthValue - 1]} de ${d.year}"
} catch (e: Exception) { "" }

private fun fechaCorta(iso: String): String = try {
    val d = java.time.LocalDate.parse(iso.take(10)); String.format("%02d/%02d/%04d", d.dayOfMonth, d.monthValue, d.year)
} catch (e: Exception) { "" }

/** "25", "25'", "0:25" o "1:20" → minutos. null si está vacío o no se entiende. */
private fun minutosDeTexto(t: String): Int? {
    val s = t.trim().trimEnd('\'', '’').trim()
    if (s.isEmpty()) return null
    if (':' in s) {
        val p = s.split(':'); val h = p.getOrNull(0)?.toIntOrNull(); val m = p.getOrNull(1)?.toIntOrNull()
        return if (h != null && m != null && m in 0..59 && h >= 0) h * 60 + m else null
    }
    return s.toIntOrNull()?.takeIf { it >= 0 }
}

/** Número con coma decimal y comilla al final permitida. */
private fun numeroDeTexto(t: String): Double? = t.trim().trimEnd('\'', '’').trim().replace(',', '.').toDoubleOrNull()

/** "930", "9.30", "9:30" o "09:30" → "09:30" (hasta 47:59). null si no es una hora. */
private fun horaDeTexto(t: String): String? {
    val s = t.trim().replace('.', ':').replace(',', ':')
    val (h, m) = when {
        ':' in s -> s.split(':').let { (it.getOrNull(0)?.toIntOrNull()) to (it.getOrNull(1)?.toIntOrNull()) }
        s.length in 3..4 && s.all { it.isDigit() } -> s.dropLast(2).toIntOrNull() to s.takeLast(2).toIntOrNull()
        s.length in 1..2 && s.all { it.isDigit() } -> s.toIntOrNull() to 0
        else -> null to null
    }
    if (h == null || m == null || h !in 0..47 || m !in 0..59) return null
    return String.format("%02d:%02d", h, m)
}

/** Mientras se escribe una hora, los ":" van solos. */
private fun mascaraHora(t: String): String {
    val dig = t.filter { it.isDigit() }.take(4)
    return if (dig.length > 2) dig.dropLast(2) + ":" + dig.takeLast(2) else dig
}

private fun sumarMinutos(hora: String, delta: Int): String? {
    val h = horaDeTexto(hora) ?: return null
    val total = (h.take(2).toInt() * 60 + h.takeLast(2).toInt() + delta).coerceIn(0, 47 * 60 + 59)
    return String.format("%02d:%02d", total / 60, total % 60)
}

private fun minutosDeHora(hora: String): Int? = horaDeTexto(hora)?.let { it.take(2).toInt() * 60 + it.takeLast(2).toInt() }

private fun nombreDia(e: JsonObject) = e.s("nombre").ifBlank { "Día ${e.i("numero") ?: ""}" }

// --------------------------------------------------------------------- cuerpo de un sector
/** El sector completo para `PUT /api/sectores/{id}` (textos "" si no hay, números null), con [cambios]. */
private fun cuerpoSector(s: JsonObject, cambios: JsonObject.() -> Unit = {}): JsonObject = JsonObject().apply {
    fun texto(k: String) = addProperty(k, s.s(k))
    fun numero(k: String) = add(k, s.v(k) ?: JsonNull.INSTANCE)
    numero("tramoId")
    addProperty("nuevaSeccion", s.b("nuevaSeccion"))
    addProperty("tienePe", s.b("tienePe"))
    texto("chSalidaCodigo"); texto("chSalidaLugar"); texto("chLlegadaCodigo"); texto("chLlegadaLugar"); texto("lpeLugar")
    numero("enlaceAntesPeKm"); numero("distanciaPeKm"); numero("distanciaSectorKm")
    numero("tiempoIdealMin"); numero("tiempoMaximoMin"); numero("reagrupamientoMin")
    texto("horaSalida"); texto("horaCierre"); texto("nota")
    add("tipoParada", s.v("tipoParada") ?: JsonNull.INSTANCE)
    numero("minutosLargadaPe")
    addProperty("sinPenalizarAdelanto", s.b("sinPenalizarAdelanto"))
    cambios()
}

private fun JsonObject.ponerNumero(k: String, v: Number?) { if (v == null) add(k, JsonNull.INSTANCE) else addProperty(k, v) }

private val TIPOS_PARADA = listOf(
    "ASISTENCIA" to "Asistencia", "FLEXI" to "Flexi-Service", "REAGRUPAMIENTO" to "Reagrupamiento",
    "ZONA_TECNICA" to "Zona técnica", "PARQUE_CERRADO" to "Parque cerrado"
)

// ------------------------------------------------------------------------- paneles laterales
private sealed class Panel {
    data class Hora(val etapaId: Int, val sectorId: Int, val numero: Int) : Panel()
    data class Anexo(val etapaId: Int, val anexo: JsonObject?) : Panel()
    data class EditarDia(val etapaId: Int) : Panel()
    object NuevoDia : Panel()
}

private data class Confirmacion(val texto: String, val accion: () -> Unit)

@Composable
fun CronogramaScreen(viewModel: PlataformaViewModel, onAbrirTramo: (tramoWebId: Int) -> Unit) {
    val c = ColoresPlataforma
    val itinerario by viewModel.itinerario.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()
    val anexos by viewModel.anexos.collectAsState()
    val detalle by viewModel.detalleRally.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val chico = tamanoPantalla() == TamanoPantalla.CHICO
    val alcance = rememberCoroutineScope()

    var filtro by remember { mutableStateOf<Int?>(null) }
    var elegido by remember { mutableStateOf<Int?>(null) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var confirmar by remember { mutableStateOf<Confirmacion?>(null) }

    LaunchedEffect(Unit) { viewModel.cargarCronograma() }
    val etapas = itinerario?.lista("etapas").orEmpty()
    val visibles = if (filtro == null) etapas else etapas.filter { it.i("id") == filtro }
    val tramosRally = detalle?.lista("tramos").orEmpty()

    fun rutaRally(armar: (Int) -> Unit) { alcance.launch { viewModel.idRally()?.let(armar) } }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (chico) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------------- 1. Tarjeta de encabezado ----------------
            TarjetaCrono {
                FlowRowCrono {
                    Column(Modifier.widthIn(max = 520.dp)) {
                        Text("CRONOGRAMA", fontSize = 24.sp, fontWeight = FontWeight.Black, color = c.texto())
                        Text(
                            "Itinerario oficial por día, sección y sector. Todo se escribe en la planilla; las horas de paso, la numeración y los totales se calculan solos.",
                            fontSize = 12.sp, color = SLATE_500
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRowCrono {
                            BotonAzul("+ NUEVO DÍA") { panel = Panel.NuevoDia }
                            itinerario?.let { it0 ->
                                CampoNumero("Largada de PE", it0.i("minutosLargadaPe"), "min después del CH", placeholder = "") { v ->
                                    if (v != null && v != it0.i("minutosLargadaPe")) rutaRally { id ->
                                        viewModel.pedir("PUT", "/api/rallies/$id/itinerario/config", JsonObject().apply { addProperty("minutosLargadaPe", v) })
                                    }
                                }
                            }
                        }
                        itinerario?.o("totales")?.let { t -> Text(totalesRally(t), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                    }
                }
                if (etapas.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        PestanaDia("Todo el rally", "", filtro == null) { filtro = null }
                        etapas.forEach { e -> PestanaDia(nombreDia(e), fechaCorta(e.s("fecha")), filtro == e.i("id")) { filtro = e.i("id") } }
                    }
                    HorizontalDivider(color = c.borde())
                } else if (itinerario != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Todavía no hay días. Creá el primero con “Nuevo día”.", fontSize = 13.sp, color = SLATE_500)
                }
            }

            if (cargando && itinerario == null) Text("Cargando cronograma…", fontSize = 13.sp, color = SLATE_500)
            error?.let { Text(it, fontSize = 13.sp, color = ROSE_600, fontWeight = FontWeight.Bold) }

            // ---------------- 2. Una tarjeta por día ----------------
            visibles.forEach { etapa ->
                val etapaId = etapa.i("id") ?: 0
                val esUltima = etapaId == etapas.lastOrNull()?.i("id")
                TarjetaCrono {
                    // Cabecera del día
                    FlowRowCrono {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(nombreDia(etapa).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = c.texto())
                            Spacer(Modifier.width(8.dp))
                            Text(fechaCorta(etapa.s("fecha")), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = SLATE_500)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BotonChicoCrono("⚠ Emitir anexo", fondo = AMARILLO_ANEXO, tinta = Color.Black, borde = AMARILLO_ANEXO) { panel = Panel.Anexo(etapaId, null) }
                            BotonChicoCrono("✎ Editar día", tinta = SLATE_600) { panel = Panel.EditarDia(etapaId) }
                            BotonChicoCrono("🗑 Borrar día", tinta = ROSE_600) {
                                confirmar = Confirmacion(
                                    "¿Eliminar \"${nombreDia(etapa)}\"?\n\nSe borran los sectores del cronograma de ese día. Los tramos y sus viñetas NO se borran: siguen en \"Tramos\"."
                                ) { viewModel.pedir("DELETE", "/api/etapas/$etapaId", exito = "¡Día eliminado!") }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Controles de horario
                    FlowRowCrono {
                        CampoNumero("Atraso máximo", etapa.i("atrasoMaximoMin"), "min", placeholder = "—") { v ->
                            if (v != etapa.i("atrasoMaximoMin")) viewModel.pedir("PUT", "/api/etapas/$etapaId/itinerario", JsonObject().apply {
                                ponerNumero("atrasoMaximoMin", v)
                                add("notasItinerario", etapa.v("notasItinerario") ?: JsonNull.INSTANCE)
                            })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Demorar todo el día:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SLATE_500)
                            listOf(5, 10, 15, 30).forEach { m ->
                                BotonChicoCrono("+$m", tinta = if (m >= 30) ROSE_600 else AMBAR_600, mono = true) {
                                    confirmar = Confirmacion("¿Demorar $m minutos todo el día?") {
                                        viewModel.pedir("POST", "/api/etapas/$etapaId/itinerario/demora?minutos=$m")
                                    }
                                }
                            }
                            BotonChicoCrono("−5", tinta = SLATE_600, mono = true) { viewModel.pedir("POST", "/api/etapas/$etapaId/itinerario/demora?minutos=-5") }
                        }
                    }
                    // Anexos del día
                    val anexosDia = anexos.filter { it.i("etapaId") == etapaId }
                    if (anexosDia.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            anexosDia.forEach { a ->
                                TarjetaAnexo(
                                    a,
                                    onEditarVinetas = { a.i("tramoId")?.let(onAbrirTramo) },
                                    onCorregir = { panel = Panel.Anexo(etapaId, a) },
                                    onDeshacer = {
                                        val texto = if (a.s("tipo") == "TRAMO")
                                            "¿Deshacer el ${a.s("etiqueta")}? Las ${a.i("cantidadSustituidas") ?: 0} viñetas vuelven a como estaban antes del anexo y se pierde lo que se dibujó en ellas."
                                        else "¿Borrar el ${a.s("etiqueta")} (${a.s("titulo")})?"
                                        confirmar = Confirmacion(texto) { viewModel.pedir("DELETE", "/api/anexos/${a.i("id")}") }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // La planilla FIA
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).horizontalScroll(rememberScrollState())) {
                        Planilla(
                            etapa = etapa,
                            todas = etapas,
                            tramosRally = tramosRally,
                            sectorElegido = elegido,
                            onElegir = { id -> elegido = if (elegido == id) null else id },
                            onHoraSalida = { sectorId, numero -> panel = Panel.Hora(etapaId, sectorId, numero) },
                            viewModel = viewModel,
                            onAviso = { viewModel.mostrarMensaje(it) },
                            largadaRally = itinerario?.i("minutosLargadaPe"),
                            onConfirmar = { t, a -> confirmar = Confirmacion(t, a) },
                            onGenerar = { rutaRally { id -> viewModel.pedir("POST", "/api/rallies/$id/itinerario/generar") } }
                        )
                    }
                    if (esUltima && etapas.size > 1) {
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), contentAlignment = Alignment.Center) { TotalesRally(etapas) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tocá una casilla, escribí y apretá Enter (los tiempos van en minutos o en horas:minutos, ej. 0:25). Tocá el número de un sector para cambiar su trazado, si es una parada, si tiene prueba especial y a cuántos minutos larga, dónde empieza la sección, o para ordenarlo o borrarlo. La hora del 1° auto de cada salida se cambia tocándola. La apertura y el cierre de los CH se pueden calcular solos en Preferencias › Cronograma.",
                        fontSize = 11.sp, color = SLATE_500, lineHeight = 15.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    NotasDia(etapa) { notas ->
                        viewModel.pedir("PUT", "/api/etapas/$etapaId/itinerario", JsonObject().apply {
                            add("atrasoMaximoMin", etapa.v("atrasoMaximoMin") ?: JsonNull.INSTANCE)
                            addProperty("notasItinerario", notas.trim().uppercase())
                        })
                    }
                    val avisos = etapa.textos("avisos")
                    if (avisos.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Color(0x99FBBF24), RoundedCornerShape(16.dp)).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            avisos.forEach { a ->
                                Row {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AMBAR_600, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(a, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AMBAR_600)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }

        // ---------------- Paneles laterales ----------------
        panel?.let { p ->
            PanelLateral(onCerrar = { panel = null }) {
                when (p) {
                    is Panel.Hora -> {
                        val etapa = etapas.firstOrNull { it.i("id") == p.etapaId }
                        val sector = etapa?.lista("sectores")?.firstOrNull { it.i("id") == p.sectorId }
                        if (etapa != null && sector != null) PanelHora(etapa, sector, p.numero, onCerrar = { panel = null }) { nueva ->
                            viewModel.pedir("PUT", "/api/sectores/${p.sectorId}/hora-salida", JsonObject().apply {
                                if (nueva == null) add("horaSalida", JsonNull.INSTANCE) else addProperty("horaSalida", nueva)
                            }) { ok, _ -> if (ok) panel = null }
                        }
                    }
                    is Panel.Anexo -> {
                        val etapa = etapas.firstOrNull { it.i("id") == p.etapaId }
                        val delDia = detalle?.lista("etapas")?.firstOrNull { it.i("id") == p.etapaId }?.lista("tramos").orEmpty()
                        if (etapa != null) PanelAnexo(etapa, p.anexo, delDia, tramosRally, viewModel, onCerrar = { panel = null }) { creado ->
                            panel = null
                            if (creado != null && creado.s("tipo") == "TRAMO") creado.i("tramoId")?.let(onAbrirTramo)
                        }
                    }
                    is Panel.EditarDia -> {
                        val etapa = detalle?.lista("etapas")?.firstOrNull { it.i("id") == p.etapaId }
                            ?: etapas.firstOrNull { it.i("id") == p.etapaId }
                        if (etapa != null) PanelEditarDia(etapa, viewModel, onCerrar = { panel = null })
                    }
                    Panel.NuevoDia -> PanelNuevoDia(etapas.size + 1, viewModel, onCerrar = { panel = null })
                }
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

private fun totalesRally(t: JsonObject): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    fun tramo(texto: String, color: Color) { pushStyle(androidx.compose.ui.text.SpanStyle(color = color)); append(texto); pop() }
    tramo("Rally: ${km2(t.d("km"))} km", AZUL)
    tramo(" · ", SLATE_500)
    tramo("P.E. ${km2(t.d("peKm"))}", if (ThemeManager.isDarkTheme) Color.White else NEGRO)
    tramo(" · ", SLATE_500)
    tramo("Enlace ${km2(t.d("enlaceKm"))}", AMBAR_600)
    tramo(" (${t.i("secciones") ?: 0} secciones · ${t.i("sectores") ?: 0} sectores · ${t.i("pruebasEspeciales") ?: 0} P.E.)", SLATE_500)
}

// ================================================================================ piezas
@Composable
private fun TarjetaCrono(contenido: @Composable ColumnScope.() -> Unit) {
    val c = ColoresPlataforma
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(c.tarjeta())
            .border(1.dp, c.borde(), RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = contenido
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCrono(contenido: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) { contenido() }
}

@Composable
private fun BotonAzul(texto: String, habilitado: Boolean = true, onClick: () -> Unit) {
    Text(
        texto, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (habilitado) AZUL else AZUL.copy(alpha = 0.4f))
            .clickable(enabled = habilitado) { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp)
    )
}

@Composable
private fun BotonChicoCrono(
    texto: String, tinta: Color, fondo: Color = Color.Transparent, borde: Color = SLATE_300, mono: Boolean = false, habilitado: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        texto, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (habilitado) tinta else tinta.copy(alpha = 0.4f),
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .border(1.dp, borde, RoundedCornerShape(12.dp))
            .clickable(enabled = habilitado) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun PestanaDia(nombre: String, fecha: String, sel: Boolean, onClick: () -> Unit) {
    val color = if (sel) AZUL else SLATE_500
    Column(Modifier.width(IntrinsicSize.Max).clickable { onClick() }) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(nombre.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 0.4.sp)
            if (fecha.isNotBlank()) { Spacer(Modifier.width(6.dp)); Text(fecha, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = color.copy(alpha = 0.8f)) }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (sel) AZUL else Color.Transparent))
    }
}

/** Un número que se escribe y se guarda al salir del campo o con Enter (como los de la web). */
@Composable
private fun CampoNumero(titulo: String, valor: Int?, despues: String, placeholder: String, ancho: Dp = 56.dp, onGuardar: (Int?) -> Unit) {
    val c = ColoresPlataforma
    val foco = LocalFocusManager.current
    var texto by remember(valor) { mutableStateOf(valor?.toString() ?: "") }
    var tuvoFoco by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (titulo.isNotBlank()) { Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_500); Spacer(Modifier.width(6.dp)) }
        Box(
            Modifier.width(ancho).clip(RoundedCornerShape(8.dp)).background(c.tarjeta()).border(1.dp, SLATE_300, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (texto.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, fontSize = 13.sp, color = SLATE_400)
            BasicTextField(
                value = texto,
                onValueChange = { n -> texto = n.filter { it.isDigit() }.take(3) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = c.texto(), textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
                cursorBrush = SolidColor(AZUL),
                modifier = Modifier.fillMaxWidth().onFocusChanged { f ->
                    if (f.isFocused) tuvoFoco = true
                    else if (tuvoFoco) { tuvoFoco = false; onGuardar(texto.toIntOrNull()) }
                }
            )
        }
        if (despues.isNotBlank()) { Spacer(Modifier.width(6.dp)); Text(despues, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_500) }
    }
}

@Composable
private fun NotasDia(etapa: JsonObject, onGuardar: (String) -> Unit) {
    val c = ColoresPlataforma
    val actual = etapa.s("notasItinerario")
    var texto by remember(actual) { mutableStateOf(actual) }
    var tuvoFoco by remember { mutableStateOf(false) }
    Text("NOTAS AL PIE DEL DÍA (UNA POR RENGLÓN, EJ: “EN CH 04A NO PENALIZA ADELANTO”)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SLATE_500, letterSpacing = 0.4.sp)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = texto, onValueChange = { texto = it.uppercase() }, minLines = 2,
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = c.texto()),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth().onFocusChanged { f ->
            if (f.isFocused) tuvoFoco = true
            else if (tuvoFoco) { tuvoFoco = false; if (texto.trim() != actual.trim()) onGuardar(texto) }
        }
    )
}

@Composable
private fun TarjetaAnexo(a: JsonObject, onEditarVinetas: () -> Unit, onCorregir: () -> Unit, onDeshacer: () -> Unit) {
    val esTramo = a.s("tipo") == "TRAMO"
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AMBAR_50).border(2.dp, AMARILLO_ANEXO, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.s("etiqueta"), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AMARILLO_ANEXO).padding(horizontal = 8.dp, vertical = 2.dp))
            if (a.s("hora").isNotBlank()) { Spacer(Modifier.width(6.dp)); Text(a.s("hora"), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NEGRO) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(a.s("titulo").uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Black, color = NEGRO)
            val sub = if (!esTramo) "Comunicado" else {
                val n = a.i("cantidadSustituidas") ?: 0
                val tr = a.o("tramo")?.let { "${it.s("identificador")} · ${it.s("nombre")}" } ?: "un tramo borrado"
                val ver = if (a.i("versionAnterior") != null && a.i("versionNueva") != null) " · versión ${a.i("versionAnterior")} → ${a.i("versionNueva")}" else ""
                "Cambia $n ${if (n == 1) "viñeta" else "viñetas"} de $tr$ver"
            }
            Text(sub, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AMBAR_800)
            if (a.s("texto").isNotBlank()) Text(a.s("texto"), fontSize = 12.sp, color = NEGRO)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val borde = Color(0x80F59E0B)
            if (esTramo) BotonChicoCrono("Editar viñetas", tinta = AMBAR_800, borde = borde) { onEditarVinetas() }
            IconoAnexo(Icons.Default.Edit, "Corregir el anexo", borde) { onCorregir() }
            IconoAnexo(Icons.Default.Undo, "Deshacer el anexo", borde) { onDeshacer() }
        }
    }
}

@Composable
private fun IconoAnexo(icono: androidx.compose.ui.graphics.vector.ImageVector, ayuda: String, borde: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, borde, RoundedCornerShape(10.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(icono, contentDescription = ayuda, tint = AMBAR_800, modifier = Modifier.size(15.dp)) }
}

/** Panel lateral fijo a la derecha (en el celular ocupa toda la pantalla). */
@Composable
private fun PanelLateral(onCerrar: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    val c = ColoresPlataforma
    val chico = tamanoPantalla() == TamanoPantalla.CHICO
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onCerrar() }) {
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .then(if (chico) Modifier.fillMaxWidth() else Modifier.width(420.dp))
                .shadow(12.dp)
                .background(c.tarjeta())
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { }
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            content = contenido
        )
    }
}

@Composable
private fun TituloPanel(titulo: String, subtitulo: String, icono: androidx.compose.ui.graphics.vector.ImageVector? = null, onCerrar: () -> Unit) {
    val c = ColoresPlataforma
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) { Icon(icono, contentDescription = null, tint = AMARILLO_ANEXO, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)) }
                Text(titulo.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = c.texto())
            }
            if (subtitulo.isNotBlank()) Text(subtitulo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_500)
        }
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, SLATE_300, RoundedCornerShape(10.dp)).clickable { onCerrar() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = SLATE_600, modifier = Modifier.size(16.dp))
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Rotulo(texto: String) {
    Text(texto.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = SLATE_500, letterSpacing = 0.4.sp)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun BotonesPanel(textoPrincipal: String, fondo: Color = AZUL, tinta: Color = Color.White, habilitado: Boolean = true, onCancelar: () -> Unit, onPrincipal: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
        Button(onClick = onCancelar, colors = ButtonDefaults.buttonColors(containerColor = SLATE_700, contentColor = Color.White), shape = RoundedCornerShape(12.dp)) {
            Text("Cancelar", fontWeight = FontWeight.Bold)
        }
        Button(onClick = onPrincipal, enabled = habilitado, colors = ButtonDefaults.buttonColors(containerColor = fondo, contentColor = tinta), shape = RoundedCornerShape(12.dp)) {
            Text(textoPrincipal, fontWeight = FontWeight.Black)
        }
    }
}

/** Campo de hora "hh:mm" con los ":" automáticos. */
@Composable
private fun CampoHora(valor: String, onCambio: (String) -> Unit, grande: Boolean = false, habilitado: Boolean = true, onEnter: () -> Unit = {}, modifier: Modifier = Modifier) {
    val c = ColoresPlataforma
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.tarjeta()).border(1.dp, SLATE_300, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = if (grande) 12.dp else 9.dp),
        contentAlignment = if (grande) Alignment.Center else Alignment.CenterStart
    ) {
        if (valor.isEmpty()) Text("hh:mm", fontSize = if (grande) 18.sp else 13.sp, color = SLATE_400, fontFamily = FontFamily.Monospace)
        BasicTextField(
            value = valor, onValueChange = { onCambio(mascaraHora(it)) }, singleLine = true, enabled = habilitado,
            textStyle = TextStyle(fontSize = if (grande) 18.sp else 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = c.texto(),
                textAlign = if (grande) TextAlign.Center else TextAlign.Start),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEnter() }),
            cursorBrush = SolidColor(AZUL),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ================================================================================ planilla
private val W_NUM_SECTOR = 36.dp
private val W_CH = 78.dp
private val W_LUGAR = 230.dp
private val W_KM = 64.dp
private val W_OTORG = 62.dp
private val W_MAX = 56.dp
private val W_PROM = 66.dp
private val W_HORA = 62.dp
private val W_SECCION = 26.dp

private enum class Campo { CODIGO, LUGAR, PE, TOTAL, OTORGADO, MAXIMO, CIERRE, MINUTOS_PARADA }

@Composable
private fun Planilla(
    etapa: JsonObject,
    todas: List<JsonObject>,
    tramosRally: List<JsonObject>,
    sectorElegido: Int?,
    onElegir: (Int) -> Unit,
    onHoraSalida: (sectorId: Int, numero: Int) -> Unit,
    viewModel: PlataformaViewModel,
    onAviso: (String) -> Unit,
    onConfirmar: (String, () -> Unit) -> Unit,
    onGenerar: () -> Unit,
    largadaRally: Int?
) {
    val filas = etapa.lista("filasFia")
    val sectores = etapa.lista("sectores")
    val conMaximo = filas.any { it.i("maximoMin") != null }
    val conApertura = filas.any { it.s("apertura").isNotBlank() }
    val anchoAntes = W_NUM_SECTOR + W_CH + W_LUGAR + W_KM * 3 + W_OTORG + (if (conMaximo) W_MAX else 0.dp) + W_PROM + W_HORA +
        (if (conApertura) W_HORA else 0.dp) + W_HORA
    val anchoTotal = anchoAntes + W_SECCION
    val ultimaFilaDeSector = HashMap<Int, Int>().apply { filas.forEachIndexed { k, f -> f.i("sectorId")?.let { put(it, k) } } }

    fun editable(f: JsonObject, campo: Campo): Boolean {
        val tipo = f.s("tipo")
        val sector = sectores.firstOrNull { it.i("id") == f.i("sectorId") }
        val esParadaSector = sector?.s("tipoParada")?.isNotBlank() == true
        val llegadaCh = tipo == "CH" && f.b("esLlegada")
        return when (campo) {
            Campo.CODIGO -> tipo == "CH"
            Campo.LUGAR -> tipo == "CH" || tipo == "PE"
            Campo.PE -> tipo == "PE"
            Campo.TOTAL, Campo.OTORGADO, Campo.MAXIMO -> llegadaCh && !esParadaSector
            Campo.CIERRE -> llegadaCh
            Campo.MINUTOS_PARADA -> tipo == "PARADA"
        }
    }

    fun guardar(f: JsonObject, campo: Campo, valor: String) {
        val sector = sectores.firstOrNull { it.i("id") == f.i("sectorId") } ?: return
        val id = sector.i("id") ?: return
        val v = valor.trim()
        val cuerpo = when (campo) {
            Campo.CODIGO -> {
                val t = v.uppercase()
                if (f.b("esLlegada")) { if (t.isEmpty()) return; cuerpoSector(sector) { addProperty("chLlegadaCodigo", t) } }
                else cuerpoSector(sector) { addProperty("chSalidaCodigo", t) }
            }
            Campo.LUGAR -> {
                val t = v.uppercase()
                when {
                    f.s("tipo") == "PE" -> cuerpoSector(sector) { addProperty("lpeLugar", t) }
                    f.b("esLlegada") -> cuerpoSector(sector) { addProperty("chLlegadaLugar", t) }
                    else -> cuerpoSector(sector) { addProperty("chSalidaLugar", t) }
                }
            }
            Campo.PE, Campo.TOTAL -> {
                val n = if (v.isEmpty()) null else (numeroDeTexto(v) ?: return)
                cuerpoSector(sector) { ponerNumero(if (campo == Campo.PE) "distanciaPeKm" else "distanciaSectorKm", n) }
            }
            Campo.OTORGADO, Campo.MAXIMO -> {
                val m = if (v.isEmpty()) null else (minutosDeTexto(v) ?: return)
                cuerpoSector(sector) { ponerNumero(if (campo == Campo.OTORGADO) "tiempoIdealMin" else "tiempoMaximoMin", m) }
            }
            Campo.CIERRE -> {
                val h = if (v.isEmpty()) "" else (horaDeTexto(v) ?: v)
                cuerpoSector(sector) { addProperty("horaCierre", h) }
            }
            Campo.MINUTOS_PARADA -> {
                val m = if (v.isEmpty()) null else (minutosDeTexto(v) ?: return)
                cuerpoSector(sector) { ponerNumero(if (f.b("reagrupamientoAntesDeSalir")) "reagrupamientoMin" else "tiempoIdealMin", m) }
            }
        }
        viewModel.pedir("PUT", "/api/sectores/$id", cuerpo)
    }

    Column(Modifier.background(Color.White).border(2.dp, NEGRO)) {
        // 4.1 Encabezado
        Row(Modifier.width(anchoTotal).background(NEGRO).padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(nombreDia(etapa).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(fechaLarga(etapa.s("fecha")), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Row(Modifier.height(IntrinsicSize.Min)) {
            Enc("N°", W_NUM_SECTOR); Enc("CH / P.E.", W_CH); Enc("LUGAR", W_LUGAR, izquierda = true)
            Enc("DIST.\nP.E.", W_KM); Enc("DIST.\nENLACE", W_KM); Enc("DIST.\nTOTAL", W_KM); Enc("TIEMPO\nOTORGADO", W_OTORG)
            if (conMaximo) Enc("TIEMPO\nMÁXIMO", W_MAX)
            Enc("PROMEDIO\nKM/H", W_PROM); Enc("PASO\n1° AUTO", W_HORA)
            if (conApertura) Enc("APER-\nTURA", W_HORA)
            Enc("CIERRE", W_HORA)
            Box(Modifier.width(W_SECCION))
        }
        Box(Modifier.width(anchoTotal).height(2.dp).background(NEGRO))

        // Filas agrupadas por sección (la banda negra ocupa el grupo)
        val grupos = mutableListOf<Pair<Int, MutableList<Pair<Int, JsonObject>>>>()
        filas.forEachIndexed { k, f ->
            val s = f.i("seccion") ?: 0
            if (grupos.lastOrNull()?.first == s) grupos.last().second.add(k to f) else grupos.add(s to mutableListOf(k to f))
        }
        var sectorAnt = -1
        grupos.forEachIndexed { gi, (seccion, lista) ->
            if (gi > 0) Box(Modifier.width(anchoTotal).height(3.dp).background(NEGRO))
            Row(Modifier.height(IntrinsicSize.Min)) {
                Column(Modifier.width(anchoAntes)) {
                    lista.forEachIndexed { j, (k, f) ->
                        val sector = f.i("sector") ?: 0
                        val nuevoSector = sector != sectorAnt
                        if (j > 0 && nuevoSector) Box(Modifier.width(anchoAntes).height(1.dp).background(NEGRO))
                        sectorAnt = sector
                        FilaPlanilla(
                            f, nuevoSector, conMaximo, conApertura,
                            elegido = sectorElegido != null && sectorElegido == f.i("sectorId"),
                            editable = { campo -> editable(f, campo) },
                            onGuardar = { campo, v -> guardar(f, campo, v) },
                            onElegir = { f.i("sectorId")?.let(onElegir) },
                            onHora = { f.i("sectorId")?.let { onHoraSalida(it, sector) } }
                        )
                        val sid = f.i("sectorId")
                        if (sid != null && sid == sectorElegido && ultimaFilaDeSector[sid] == k) {
                            val s = sectores.firstOrNull { it.i("id") == sid }
                            if (s != null) DetalleSector(
                                s, sectores, tramosRally, largadaRally, anchoAntes, viewModel, onConfirmar,
                                onCerrar = { onElegir(sid) }
                            )
                        }
                    }
                }
                Column(
                    Modifier.width(W_SECCION).fillMaxHeight().background(NEGRO).padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    "SECCIÓN".forEach { l -> Text(l.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, lineHeight = 11.sp) }
                    Spacer(Modifier.height(6.dp))
                    Text("$seccion", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }

        // 4.11 Pie: fila Agregar, totales y notas
        Box(Modifier.width(anchoTotal).height(2.dp).background(NEGRO))
        FilaAgregar(etapa, sectores.isEmpty(), tramosRally, anchoTotal, viewModel, onAviso, onGenerar)
        val t = etapa.o("totales")
        Box(Modifier.width(anchoTotal).height(2.dp).background(NEGRO))
        Row(Modifier.width(anchoTotal).background(GRIS_PARADA).height(IntrinsicSize.Min)) {
            CeldaT("TOTALES DEL DÍA", W_NUM_SECTOR + W_CH + W_LUGAR, negrita = true)
            CeldaN(km2(t?.d("peKm")), W_KM, negrita = true)
            CeldaN(km2(t?.d("enlaceKm")), W_KM, negrita = true)
            CeldaN(km2(t?.d("km")), W_KM, negrita = true)
            CeldaT("P.E.: ${pct(t?.d("peKm") ?: 0.0, t?.d("km") ?: 0.0)} del recorrido", anchoTotal - (W_NUM_SECTOR + W_CH + W_LUGAR + W_KM * 3), negrita = false)
        }
        val notas = etapa.s("notasItinerario").lines().filter { it.isNotBlank() }
        val atraso = etapa.i("atrasoMaximoMin")
        if (notas.isNotEmpty() || atraso != null) {
            Column(Modifier.width(anchoTotal).background(NEGRO).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                notas.forEach { Text(it.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                if (atraso != null) Text("ATRASO MÁXIMO $atraso MINUTOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FilaPlanilla(
    f: JsonObject, nuevoSector: Boolean, conMaximo: Boolean, conApertura: Boolean, elegido: Boolean,
    editable: (Campo) -> Boolean, onGuardar: (Campo, String) -> Unit, onElegir: () -> Unit, onHora: () -> Unit
) {
    val tipo = f.s("tipo")
    val esParada = tipo == "PARADA"
    val esPe = tipo == "PE"
    val asistencia = esParada && (f.s("parada") == "ASISTENCIA" || f.s("parada") == "FLEXI")
    val fondo = when {
        elegido -> CELESTE_ELEGIDO
        esParada -> if (asistencia) AZUL_PARADA else GRIS_PARADA
        esPe -> GRIS_PE
        else -> Color.White
    }
    val italica = if (esPe) FontStyle.Italic else FontStyle.Normal
    val celdaNumero: @Composable () -> Unit = {
        Box(Modifier.width(W_NUM_SECTOR).fillMaxHeight().bordesX(), contentAlignment = Alignment.Center) {
            if (nuevoSector) {
                Text(
                    "${f.i("sector") ?: ""}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = NEGRO,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (elegido) CELESTE_ELEGIDO else Color.Transparent)
                        .bordePunteado(if (elegido) AZUL else SLATE_400)
                        .clickable { onElegir() }
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
    if (esParada) {
        Row(Modifier.height(IntrinsicSize.Min).background(fondo).border(2.dp, NEGRO)) {
            celdaNumero()
            CeldaT(f.s("etiqueta").uppercase(), W_CH + W_LUGAR, negrita = true)
            CeldaN(entreParentesis(f.d("desdePeKm")), W_KM)
            CeldaN(entreParentesis(f.d("desdeEnlaceKm")), W_KM)
            CeldaN(entreParentesis(f.d("desdeTotalKm")), W_KM)
            Row(Modifier.width(W_OTORG).fillMaxHeight().bordesX(), verticalAlignment = Alignment.CenterVertically) {
                if (f.b("tiempoMaximo")) Text("máx.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NEGRO, modifier = Modifier.padding(start = 3.dp))
                Casilla(horasMin(f.i("minutos")), editable(Campo.MINUTOS_PARADA), alinear = TextAlign.End, negrita = true, numerico = true, modifier = Modifier.weight(1f)) {
                    onGuardar(Campo.MINUTOS_PARADA, it)
                }
            }
            if (conMaximo) CeldaN("", W_MAX)
            CeldaN("", W_PROM); CeldaN("", W_HORA)
            if (conApertura) CeldaN("", W_HORA)
            CeldaN("", W_HORA)
        }
        return
    }
    Row(Modifier.height(IntrinsicSize.Min).background(fondo)) {
        celdaNumero()
        Box(Modifier.width(W_CH).fillMaxHeight().bordesX(), contentAlignment = Alignment.CenterStart) {
            if (tipo == "CH") Casilla(f.s("codigo"), editable(Campo.CODIGO), negrita = true, mayusculas = true) { onGuardar(Campo.CODIGO, it) }
            else Text(f.s("codigo"), fontSize = 13.sp, fontWeight = FontWeight.Black, fontStyle = italica, color = NEGRO, modifier = Modifier.padding(horizontal = 6.dp))
        }
        Column(Modifier.width(W_LUGAR).fillMaxHeight().bordesX(), verticalArrangement = Arrangement.Center) {
            Casilla(f.s("lugar").uppercase(), editable(Campo.LUGAR), negrita = true, italica = esPe, mayusculas = true) { onGuardar(Campo.LUGAR, it) }
            if (f.b("sinPenalizarAdelanto")) Text("(${f.s("codigo")} no penaliza adelanto)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NEGRO, modifier = Modifier.padding(horizontal = 6.dp))
        }
        Box(Modifier.width(W_KM).fillMaxHeight().bordesX(), contentAlignment = Alignment.CenterEnd) {
            if (esPe) Casilla(km2(f.d("peKm")), editable(Campo.PE), alinear = TextAlign.End, negrita = true, italica = true, numerico = true) { onGuardar(Campo.PE, it) }
        }
        CeldaN(km2(f.d("enlaceKm")), W_KM)
        Box(Modifier.width(W_KM).fillMaxHeight().bordesX(), contentAlignment = Alignment.CenterEnd) {
            Casilla(km2(f.d("totalKm")), editable(Campo.TOTAL), alinear = TextAlign.End, numerico = true) { onGuardar(Campo.TOTAL, it) }
        }
        Box(Modifier.width(W_OTORG).fillMaxHeight().bordesX(), contentAlignment = Alignment.CenterEnd) {
            Casilla(horasMin(f.i("otorgadoMin")), editable(Campo.OTORGADO), alinear = TextAlign.End, negrita = true, numerico = true) { onGuardar(Campo.OTORGADO, it) }
        }
        if (conMaximo) Box(Modifier.width(W_MAX).fillMaxHeight().bordesX(), contentAlignment = Alignment.CenterEnd) {
            Casilla(horasMin(f.i("maximoMin")), editable(Campo.MAXIMO), alinear = TextAlign.End, numerico = true) { onGuardar(Campo.MAXIMO, it) }
        }
        CeldaN(f.d("promedioKmH")?.let { km2(it) } ?: "", W_PROM)
        Box(Modifier.width(W_HORA).fillMaxHeight().bordesX().padding(horizontal = 6.dp), contentAlignment = Alignment.CenterEnd) {
            val horaSalida = f.b("esSalida") && tipo == "CH"
            if (horaSalida) {
                Text(
                    f.s("hora").ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.Black, color = AZUL_OSCURO,
                    modifier = Modifier.subrayadoPunteado(AZUL).clickable { onHora() }
                )
            } else Text(f.s("hora"), fontSize = 13.sp, fontWeight = FontWeight.Black, fontStyle = italica, color = NEGRO)
        }
        if (conApertura) CeldaN(f.s("apertura"), W_HORA)
        Box(Modifier.width(W_HORA).fillMaxHeight().bordesX(), contentAlignment = Alignment.CenterEnd) {
            if (f.b("esLlegada") && tipo == "CH") {
                // Solo se precarga si el cierre es manual; si no, arranca vacía (muestra el calculado)
                Casilla(
                    if (f.b("cierreManual")) f.s("cierre") else "", editable(Campo.CIERRE), alinear = TextAlign.End, hora = true,
                    mostrarSiVacio = f.s("cierre")
                ) { onGuardar(Campo.CIERRE, it) }
            } else Text(f.s("cierre"), fontSize = 13.sp, color = NEGRO, modifier = Modifier.padding(horizontal = 6.dp))
        }
    }
}

/**
 * Casilla que se edita escribiendo directo en la planilla: guarda con Enter (o al salir) solo si
 * cambió. [mostrarSiVacio] = lo que se ve cuando no hay valor cargado (el cierre calculado).
 */
@Composable
private fun Casilla(
    valor: String, editable: Boolean, alinear: TextAlign = TextAlign.Start, negrita: Boolean = false, italica: Boolean = false,
    mayusculas: Boolean = false, numerico: Boolean = false, hora: Boolean = false, mostrarSiVacio: String = "",
    modifier: Modifier = Modifier, onGuardar: (String) -> Unit
) {
    val estilo = TextStyle(
        fontSize = 13.sp, color = NEGRO, textAlign = alinear,
        fontWeight = if (negrita) FontWeight.Black else FontWeight.Normal,
        fontStyle = if (italica) FontStyle.Italic else FontStyle.Normal
    )
    if (!editable) {
        Text((valor.ifEmpty { mostrarSiVacio }), style = estilo, maxLines = 2, modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp))
        return
    }
    val foco = LocalFocusManager.current
    var texto by remember(valor) { mutableStateOf(valor) }
    var conFoco by remember { mutableStateOf(false) }
    var tuvoFoco by remember { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxWidth()
            .background(if (conFoco) Color.White else Color.Transparent)
            .then(if (conFoco) Modifier.border(2.dp, AZUL) else Modifier)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (texto.isEmpty() && !conFoco && mostrarSiVacio.isNotEmpty()) Text(mostrarSiVacio, style = estilo, modifier = Modifier.fillMaxWidth())
        BasicTextField(
            value = texto,
            onValueChange = { n -> texto = if (hora) mascaraHora(n) else if (mayusculas) n.uppercase() else n },
            singleLine = true,
            textStyle = estilo,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numerico || hora) KeyboardType.Text else KeyboardType.Text,
                capitalization = if (mayusculas) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
            cursorBrush = SolidColor(AZUL),
            modifier = Modifier.fillMaxWidth().onFocusChanged { f ->
                conFoco = f.isFocused
                if (f.isFocused) tuvoFoco = true
                else if (tuvoFoco) {
                    tuvoFoco = false
                    val nuevo = if (mayusculas) texto.uppercase() else texto
                    if (nuevo.trim() != valor.trim()) onGuardar(nuevo.trim())
                }
            }
        )
    }
}

private fun Modifier.bordesX(): Modifier = this.drawBehind {
    val w = 1.dp.toPx()
    drawLine(NEGRO, Offset(0f, 0f), Offset(0f, size.height), w)
    drawLine(NEGRO, Offset(size.width, 0f), Offset(size.width, size.height), w)
}

private fun Modifier.bordePunteado(color: Color): Modifier = this.drawBehind {
    val w = 1.dp.toPx()
    drawRoundRect(
        color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
    )
}

private fun Modifier.subrayadoPunteado(color: Color): Modifier = this.drawBehind {
    val w = 1.dp.toPx()
    drawLine(color, Offset(0f, size.height - w), Offset(size.width, size.height - w), w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)))
}

@Composable
private fun Enc(texto: String, ancho: Dp, izquierda: Boolean = false) {
    Box(Modifier.width(ancho).fillMaxHeight().bordesX().padding(horizontal = 4.dp, vertical = 4.dp), contentAlignment = if (izquierda) Alignment.CenterStart else Alignment.Center) {
        Text(texto, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = NEGRO, textAlign = if (izquierda) TextAlign.Start else TextAlign.Center)
    }
}

@Composable
private fun CeldaN(texto: String, ancho: Dp, negrita: Boolean = false) {
    Box(Modifier.width(ancho).fillMaxHeight().bordesX().padding(horizontal = 6.dp, vertical = 3.dp), contentAlignment = Alignment.CenterEnd) {
        Text(texto, fontSize = 13.sp, fontWeight = if (negrita) FontWeight.Black else FontWeight.Normal, color = NEGRO, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun CeldaT(texto: String, ancho: Dp, negrita: Boolean) {
    Box(Modifier.width(ancho).fillMaxHeight().bordesX().padding(horizontal = 6.dp, vertical = 3.dp), contentAlignment = Alignment.CenterStart) {
        Text(texto, fontSize = 13.sp, fontWeight = if (negrita) FontWeight.Black else FontWeight.Bold, color = NEGRO)
    }
}

// ------------------------------------------------------------- selector simple (desplegable)
@Composable
private fun Desplegable(actual: String, opciones: List<String>, ancho: Dp, habilitado: Boolean = true, onElegir: (Int) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(ancho)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(1.dp, SLATE_400, RoundedCornerShape(6.dp))
                .clickable(enabled = habilitado) { abierto = true }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(actual, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SLATE_600, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEachIndexed { k, o -> DropdownMenuItem(text = { Text(o, fontSize = 13.sp) }, onClick = { abierto = false; onElegir(k) }) }
        }
    }
}

@Composable
private fun CampoChico(valor: String, placeholder: String, ancho: Dp, mayusculas: Boolean = false, numerico: Boolean = false, onCambio: (String) -> Unit, onListo: () -> Unit = {}) {
    val foco = LocalFocusManager.current
    var tuvoFoco by remember { mutableStateOf(false) }
    Box(
        Modifier.width(ancho).clip(RoundedCornerShape(6.dp)).background(Color.White).border(1.dp, SLATE_400, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (valor.isEmpty()) Text(placeholder, fontSize = 12.sp, color = SLATE_400, maxLines = 1)
        BasicTextField(
            value = valor, onValueChange = { onCambio(if (mayusculas) it.uppercase() else it) }, singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numerico) KeyboardType.Number else KeyboardType.Text,
                capitalization = if (mayusculas) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
            cursorBrush = SolidColor(AZUL),
            modifier = Modifier.fillMaxWidth().onFocusChanged { f -> if (f.isFocused) tuvoFoco = true else if (tuvoFoco) { tuvoFoco = false; onListo() } }
        )
    }
}

@Composable
private fun Tilde(texto: String, marcado: Boolean, habilitado: Boolean = true, onCambio: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.alphaSi(habilitado).clickable(enabled = habilitado) { onCambio(!marcado) }
    ) {
        Checkbox(checked = marcado, onCheckedChange = if (habilitado) onCambio else null, modifier = Modifier.size(28.dp),
            colors = CheckboxDefaults.colors(checkedColor = AZUL))
        Spacer(Modifier.width(4.dp))
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800)
    }
}

private fun Modifier.alphaSi(habilitado: Boolean): Modifier = if (habilitado) this else this.alpha(0.5f)

private fun opcionTramo(t: JsonObject, conKm: Boolean): String =
    "${t.s("identificador")} · ${t.s("nombre")}" + if (conKm) " (${km2(t.d("distanciaTotalKm"))} km)" else ""

// ------------------------------------------------------------------- 6. fila "Agregar"
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaAgregar(
    etapa: JsonObject, diaVacio: Boolean, tramosRally: List<JsonObject>, ancho: Dp, viewModel: PlataformaViewModel,
    onAviso: (String) -> Unit, onGenerar: () -> Unit
) {
    val etapaId = etapa.i("id") ?: 0
    var tipo by remember(etapaId) { mutableStateOf<String?>(null) } // null = recorrido
    var tramoId by remember(etapaId) { mutableStateOf<Int?>(null) }
    var ch by remember(etapaId) { mutableStateOf("") }
    var pe by remember(etapaId) { mutableStateOf(false) }
    var duracion by remember(etapaId) { mutableStateOf("") }

    fun agregar() {
        val codigo = ch.trim().uppercase()
        if (codigo.isEmpty()) {
            onAviso(if (tipo == null) "Escribí el CH de llegada del sector (ej: CH 03)." else "Escribí el CH de salida de la parada (ej: CH 3C).")
            return
        }
        val minutos = if (tipo != null) (minutosDeTexto(duracion) ?: run { onAviso("Escribí cuánto dura la parada (25 o 1:20)."); return }) else null
        val cuerpo = JsonObject().apply {
            ponerNumero("tramoId", if (tipo == null) tramoId else null)
            addProperty("nuevaSeccion", false)
            addProperty("tienePe", tipo == null && pe)
            addProperty("chSalidaCodigo", ""); addProperty("chSalidaLugar", "")
            addProperty("chLlegadaCodigo", codigo); addProperty("chLlegadaLugar", ""); addProperty("lpeLugar", "")
            add("enlaceAntesPeKm", JsonNull.INSTANCE); add("distanciaPeKm", JsonNull.INSTANCE); add("distanciaSectorKm", JsonNull.INSTANCE)
            ponerNumero("tiempoIdealMin", minutos)
            add("tiempoMaximoMin", JsonNull.INSTANCE); add("reagrupamientoMin", JsonNull.INSTANCE)
            addProperty("horaSalida", ""); addProperty("horaCierre", ""); addProperty("nota", "")
            if (tipo == null) add("tipoParada", JsonNull.INSTANCE) else addProperty("tipoParada", tipo)
            add("minutosLargadaPe", JsonNull.INSTANCE)
            addProperty("sinPenalizarAdelanto", false)
        }
        viewModel.pedir("POST", "/api/etapas/$etapaId/sectores", cuerpo) { ok, _ ->
            if (ok) { ch = ""; duracion = ""; tramoId = null; pe = false }
        }
    }

    Column(Modifier.width(ancho).background(SLATE_50).padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("+ AGREGAR", fontSize = 12.sp, fontWeight = FontWeight.Black, color = SLATE_800, modifier = Modifier.padding(top = 6.dp))
            val opcionesTipo = listOf("Sector (recorrido)") + TIPOS_PARADA.map { "Parada: ${it.second}" }
            Desplegable(if (tipo == null) opcionesTipo[0] else "Parada: ${TIPOS_PARADA.first { it.first == tipo }.second}", opcionesTipo, 190.dp) { k ->
                tipo = if (k == 0) null else TIPOS_PARADA[k - 1].first
            }
            if (tipo == null) {
                val sel = tramosRally.firstOrNull { it.i("id") == tramoId }
                Desplegable(sel?.let { opcionTramo(it, false) } ?: "— Trazado —", listOf("— Trazado —") + tramosRally.map { opcionTramo(it, false) }, 190.dp) { k ->
                    val t = tramosRally.getOrNull(k - 1)
                    tramoId = t?.i("id")
                    if (t != null && t.s("tipo").equals("PE", true)) pe = true
                }
                CampoChico(ch, "CH DE LLEGADA", 120.dp, mayusculas = true, onCambio = { ch = it })
                Tilde("Prueba especial", pe) { pe = it }
            } else {
                CampoChico(duracion, "0:30", 70.dp, onCambio = { duracion = it })
                CampoChico(ch, "CH DE SALIDA", 120.dp, mayusculas = true, onCambio = { ch = it })
            }
            BotonAzul("AGREGAR") { agregar() }
        }
        if (diaVacio && tramosRally.isNotEmpty()) {
            Text(
                "✨ ARMAR DESDE LOS TRAMOS CARGADOS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AZUL,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, AZUL, RoundedCornerShape(6.dp)).clickable { onGenerar() }.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

// ----------------------------------------------------- 8. detalle del sector (al tocar el N°)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetalleSector(
    s: JsonObject, sectores: List<JsonObject>, tramosRally: List<JsonObject>, largadaRally: Int?, ancho: Dp,
    viewModel: PlataformaViewModel, onConfirmar: (String, () -> Unit) -> Unit, onCerrar: () -> Unit
) {
    val id = s.i("id") ?: return
    val numero = s.i("numero") ?: 0
    val esParada = s.s("tipoParada").isNotBlank()
    val esPrimero = sectores.firstOrNull()?.i("id") == id
    val esUltimo = sectores.lastOrNull()?.i("id") == id
    fun guardar(cambios: JsonObject.() -> Unit) = viewModel.pedir("PUT", "/api/sectores/$id", cuerpoSector(s, cambios))

    var largada by remember(s) { mutableStateOf(s.i("minutosLargadaPe")?.toString() ?: "") }
    var reagr by remember(s) { mutableStateOf(s.i("reagrupamientoMin")?.toString() ?: "") }
    var nota by remember(s) { mutableStateOf(s.s("nota")) }

    Column(
        Modifier.width(ancho).background(Color(0xFFF0F9FF)).drawBehind {
            drawLine(AZUL, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
        }.padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("SECTOR $numero", fontSize = 12.sp, fontWeight = FontWeight.Black, color = SLATE_800, modifier = Modifier.padding(top = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tipo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800); Spacer(Modifier.width(6.dp))
                val opciones = listOf("Recorrido") + TIPOS_PARADA.map { "Parada: ${it.second}" }
                Desplegable(if (!esParada) "Recorrido" else "Parada: ${TIPOS_PARADA.firstOrNull { it.first == s.s("tipoParada") }?.second ?: s.s("tipoParada")}", opciones, 180.dp) { k ->
                    if (k == 0) guardar { add("tipoParada", JsonNull.INSTANCE) }
                    else guardar { addProperty("tipoParada", TIPOS_PARADA[k - 1].first); add("tramoId", JsonNull.INSTANCE); addProperty("tienePe", false) }
                }
            }
            if (!esParada) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trazado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800); Spacer(Modifier.width(6.dp))
                    val actual = tramosRally.firstOrNull { it.i("id") == s.i("tramoId") }
                    Desplegable(actual?.let { opcionTramo(it, true) } ?: "— Sin trazado —", listOf("— Sin trazado —") + tramosRally.map { opcionTramo(it, true) }, 230.dp) { k ->
                        val t = tramosRally.getOrNull(k - 1)
                        guardar { ponerNumero("tramoId", t?.i("id")); if (t != null) addProperty("tienePe", t.s("tipo").equals("PE", true)) }
                    }
                }
                s.i("idemDeSector")?.let { Text("pasada del sector $it", fontSize = 12.sp, color = SLATE_500, modifier = Modifier.padding(top = 6.dp)) }
                Tilde("Prueba especial", s.b("tienePe")) { v -> guardar { addProperty("tienePe", v) } }
                if (s.b("tienePe")) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Largada", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800); Spacer(Modifier.width(6.dp))
                    CampoChico(largada, largadaRally?.toString() ?: "", 50.dp, numerico = true, onCambio = { largada = it.filter { c -> c.isDigit() }.take(2) }, onListo = {
                        val v = largada.toIntOrNull()
                        if (v != s.i("minutosLargadaPe") && (v == null || v in 0..60)) guardar { ponerNumero("minutosLargadaPe", v) }
                    })
                    Spacer(Modifier.width(6.dp)); Text("min después del CH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800)
                }
            }
            Tilde("En ${s.s("chLlegadaCodigo")} no penaliza adelanto", s.b("sinPenalizarAdelanto")) { v -> guardar { addProperty("sinPenalizarAdelanto", v) } }
            Tilde("Empieza sección nueva", esPrimero || s.b("nuevaSeccion"), habilitado = !esPrimero) { v -> guardar { addProperty("nuevaSeccion", v) } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reagrupamiento antes de salir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800); Spacer(Modifier.width(6.dp))
                CampoChico(reagr, "", 56.dp, onCambio = { reagr = it }, onListo = {
                    val v = if (reagr.isBlank()) null else minutosDeTexto(reagr)
                    if (reagr.isBlank() || v != null) { if (v != s.i("reagrupamientoMin")) guardar { ponerNumero("reagrupamientoMin", v) } }
                })
                Spacer(Modifier.width(6.dp)); Text("min", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Nota", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800); Spacer(Modifier.width(6.dp))
                CampoChico(nota, "Uso interno", 220.dp, onCambio = { nota = it }, onListo = { if (nota.trim() != s.s("nota").trim()) guardar { addProperty("nota", nota.trim()) } })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconoDetalle(Icons.Default.KeyboardArrowUp, "Subir", habilitado = !esPrimero) { viewModel.pedir("POST", "/api/sectores/$id/mover?direccion=-1") }
                IconoDetalle(Icons.Default.KeyboardArrowDown, "Bajar", habilitado = !esUltimo) { viewModel.pedir("POST", "/api/sectores/$id/mover?direccion=1") }
                IconoDetalle(Icons.Default.Delete, "Borrar", tinta = ROSE_600) {
                    onConfirmar("¿Eliminar el sector $numero? (el trazado y sus viñetas no se borran)") { viewModel.pedir("DELETE", "/api/sectores/$id") { ok, _ -> if (ok) onCerrar() } }
                }
                IconoDetalle(Icons.Default.Close, "Cerrar") { onCerrar() }
            }
        }
    }
}

@Composable
private fun IconoDetalle(icono: androidx.compose.ui.graphics.vector.ImageVector, ayuda: String, tinta: Color = SLATE_600, habilitado: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).alphaSi(habilitado).clip(RoundedCornerShape(6.dp)).background(Color.White).border(1.dp, SLATE_400, RoundedCornerShape(6.dp)).clickable(enabled = habilitado) { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(icono, contentDescription = ayuda, tint = tinta, modifier = Modifier.size(16.dp)) }
}

// ----------------------------------------------------------------- 13. totales del rally
@Composable
private fun TotalesRally(etapas: List<JsonObject>) {
    val dias = etapas.map { e ->
        val t = e.o("totales")
        Triple(e, e.lista("filasFia").mapNotNull { it.i("seccion") }.distinct().sorted(), Triple(t?.d("peKm") ?: 0.0, t?.d("enlaceKm") ?: 0.0, t?.d("km") ?: 0.0))
    }
    val pe = dias.sumOf { it.third.first }; val en = dias.sumOf { it.third.second }; val km = dias.sumOf { it.third.third }
    val wDia = 300.dp; val wN = 64.dp; val ancho = wDia + wN * 4
    Column(Modifier.width(ancho).background(Color.White).border(2.dp, NEGRO)) {
        Box(Modifier.width(ancho).background(NEGRO).padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
            Text("TOTALES DEL RALLY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Row(Modifier.height(IntrinsicSize.Min)) { Enc("DÍA", wDia, izquierda = true); Enc("P.E.", wN); Enc("ENLACE", wN); Enc("TOTAL", wN); Enc("% P.E.", wN) }
        Box(Modifier.width(ancho).height(2.dp).background(NEGRO))
        dias.forEachIndexed { k, (e, secciones, t) ->
            if (k > 0) Box(Modifier.width(ancho).height(1.dp).background(NEGRO))
            Row(Modifier.height(IntrinsicSize.Min)) {
                Column(Modifier.width(wDia).fillMaxHeight().bordesX().padding(horizontal = 6.dp, vertical = 3.dp)) {
                    val f = fechaLarga(e.s("fecha"))
                    Text((nombreDia(e) + if (f.isNotBlank()) " · $f" else "").uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NEGRO)
                    if (secciones.isNotEmpty()) Text((if (secciones.size > 1) "Secciones " else "Sección ") + secciones.joinToString(", "), fontSize = 11.sp, color = NEGRO)
                }
                CeldaN(km2(t.first), wN); CeldaN(km2(t.second), wN); CeldaN(km2(t.third), wN, negrita = true); CeldaN(pct(t.first, t.third), wN)
            }
        }
        Box(Modifier.width(ancho).height(2.dp).background(NEGRO))
        Row(Modifier.height(IntrinsicSize.Min).background(GRIS_PARADA)) {
            CeldaT("TOTAL DEL RALLY", wDia, negrita = true)
            CeldaN(km2(pe), wN, negrita = true); CeldaN(km2(en), wN, negrita = true); CeldaN(km2(km), wN, negrita = true); CeldaN(pct(pe, km), wN, negrita = true)
        }
    }
}

// --------------------------------------------------------------- 7. panel "Hora del 1° auto"
@Composable
private fun PanelHora(etapa: JsonObject, sector: JsonObject, numero: Int, onCerrar: () -> Unit, onGuardar: (String?) -> Unit) {
    val c = ColoresPlataforma
    val actual = sector.s("horaSalidaCalculada").ifBlank { sector.s("horaSalida") }
    var nueva by remember(sector) { mutableStateOf(actual) }
    val esPrimero = etapa.lista("sectores").firstOrNull()?.i("id") == sector.i("id")
    val subtitulo = buildString {
        append("Sector $numero")
        sector.i("peNumero")?.let { append(" · P.E. $it") }
        sector.o("tramo")?.let { append(" · ${it.s("identificador")} · ${it.s("nombre")}") }
        sector.i("idemDeSector")?.let { append(" · pasada del sector $it") }
    }
    TituloPanel("Hora del 1° auto", subtitulo, onCerrar = onCerrar)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, SLATE_300, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("AHORA LARGA", fontSize = 12.sp, fontWeight = FontWeight.Black, color = SLATE_600, modifier = Modifier.weight(1f))
        Text(actual.ifBlank { "—" }, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = c.texto())
    }
    Spacer(Modifier.height(14.dp))
    Rotulo("Correr la largada")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(-10, -5, -1, 1, 5, 10).forEach { d ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).border(1.dp, SLATE_300, RoundedCornerShape(10.dp))
                    .clickable { sumarMinutos(nueva.ifBlank { actual }, d)?.let { nueva = it } }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) { Text(if (d > 0) "+$d" else "−${-d}", fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = if (d > 0) AMBAR_600 else SLATE_600) }
        }
    }
    Spacer(Modifier.height(14.dp))
    Rotulo("Nueva hora (o escribila)")
    CampoHora(nueva, { nueva = it }, grande = true, onEnter = { horaDeTexto(nueva)?.let { onGuardar(it) } }, modifier = Modifier.fillMaxWidth())
    val dif = (minutosDeHora(nueva) ?: 0) - (minutosDeHora(actual) ?: 0)
    if (horaDeTexto(nueva) != null && minutosDeHora(actual) != null && dif != 0) {
        Spacer(Modifier.height(8.dp))
        Text(
            (if (dif > 0) "Se demora $dif min." else "Se adelanta ${-dif} min.") + " Lo que viene después se corre solo.",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (dif > 0) AMBAR_600 else SLATE_500
        )
    }
    Spacer(Modifier.height(16.dp))
    if (sector.s("horaSalida").isNotBlank() && !esPrimero) {
        TextButton(onClick = { onGuardar(null) }) { Text("Volver a la calculada", fontWeight = FontWeight.Bold) }
    }
    BotonesPanel("GUARDAR HORA", habilitado = horaDeTexto(nueva) != null, onCancelar = onCerrar) { horaDeTexto(nueva)?.let { onGuardar(it) } }
}

// ----------------------------------------------------------- 10.2 panel "Emitir anexo"
@Composable
private fun PanelAnexo(
    etapa: JsonObject, anexo: JsonObject?, tramosDelDia: List<JsonObject>, tramosRally: List<JsonObject>,
    viewModel: PlataformaViewModel, onCerrar: () -> Unit, onListo: (creado: JsonObject?) -> Unit
) {
    val c = ColoresPlataforma
    val corrigiendo = anexo != null
    val etapaId = etapa.i("id") ?: 0
    var tipo by remember { mutableStateOf(anexo?.s("tipo")?.ifBlank { null } ?: "COMUNICADO") }
    var titulo by remember { mutableStateOf(anexo?.s("titulo") ?: "") }
    var hora by remember { mutableStateOf(anexo?.s("hora") ?: "") }
    var texto by remember { mutableStateOf(anexo?.s("texto") ?: "") }
    var tramoId by remember { mutableStateOf<Int?>(null) }
    var vinetas by remember { mutableStateOf<List<JsonObject>?>(null) }
    var leyendo by remember { mutableStateOf(false) }
    var marcadas by remember { mutableStateOf(setOf<Int>()) }
    var desde by remember { mutableStateOf("") }
    var hasta by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    // Primero los tramos de ese día, después los demás
    val opciones = remember(tramosDelDia, tramosRally) {
        val ids = tramosDelDia.mapNotNull { it.i("id") }.toSet()
        tramosDelDia + tramosRally.filter { it.i("id") !in ids }
    }

    TituloPanel(if (corrigiendo) "Corregir ${anexo!!.s("etiqueta")}" else "Emitir anexo", nombreDia(etapa).uppercase(), icono = Icons.Default.Warning, onCerrar = onCerrar)
    if (!corrigiendo) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("COMUNICADO" to ("COMUNICADO" to "Solo texto"), "TRAMO" to ("CAMBIO DE VIÑETAS" to "De un tramo o enlace")).forEach { (clave, t) ->
                val sel = tipo == clave
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (sel) AMARILLO_ANEXO else Color.Transparent)
                        .border(1.dp, if (sel) AMARILLO_ANEXO else c.borde(), RoundedCornerShape(14.dp)).clickable { tipo = clave }.padding(12.dp)
                ) {
                    Text(t.first, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (sel) Color.Black else c.texto())
                    Text(t.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.Black else SLATE_500)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(2f)) {
            Rotulo("Título")
            OutlinedTextField(titulo, { titulo = it.uppercase() }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
        }
        Column(Modifier.weight(1f)) {
            Rotulo("Hora")
            CampoHora(hora, { hora = it }, modifier = Modifier.fillMaxWidth().height(56.dp))
        }
    }
    Spacer(Modifier.height(12.dp))
    Rotulo(if (tipo == "COMUNICADO") "Texto del comunicado" else "Motivo o detalle (opcional)")
    OutlinedTextField(texto, { texto = it }, minLines = if (tipo == "COMUNICADO") 7 else 3, modifier = Modifier.fillMaxWidth())

    if (tipo == "TRAMO" && !corrigiendo) {
        Spacer(Modifier.height(12.dp))
        Rotulo("Tramo o enlace que cambia")
        val sel = opciones.firstOrNull { it.i("id") == tramoId }
        Desplegable(sel?.let { opcionTramo(it, false) } ?: "— Elegí —", listOf("— Elegí —") + opciones.map { opcionTramo(it, false) }, 380.dp) { k ->
            val t = opciones.getOrNull(k - 1)
            tramoId = t?.i("id"); marcadas = emptySet(); vinetas = null
            if (t != null) {
                leyendo = true
                alcance.launch {
                    val d = viewModel.leer("/api/tramos/${t.i("id")}")?.takeIf { it.isJsonObject }?.asJsonObject
                    vinetas = d?.lista("indicaciones")?.sortedBy { it.i("orden") ?: 0 }.orEmpty()
                    leyendo = false
                }
            }
        }
        if (leyendo) Text("Leyendo viñetas…", fontSize = 12.sp, color = SLATE_500, modifier = Modifier.padding(top = 6.dp))
        vinetas?.let { lista ->
            Spacer(Modifier.height(10.dp))
            if (lista.isEmpty()) Text("Este tramo no tiene viñetas.", fontSize = 12.sp, color = SLATE_500)
            else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Desde viñeta", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.texto())
                    CampoChico(desde, "", 50.dp, numerico = true, onCambio = { desde = it.filter { ch -> ch.isDigit() } })
                    Text("Hasta", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.texto())
                    CampoChico(hasta, "", 50.dp, numerico = true, onCambio = { hasta = it.filter { ch -> ch.isDigit() } })
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonChicoCrono("Marcar rango", tinta = AMBAR_800, borde = AMARILLO_ANEXO) {
                        val a = desde.toIntOrNull(); val b = hasta.toIntOrNull() ?: a
                        if (a != null && b != null) {
                            val (x, y) = if (a <= b) a to b else b to a
                            marcadas = marcadas + lista.mapIndexedNotNull { k, v -> if (k + 1 in x..y) v.i("id") else null }
                        }
                    }
                    BotonChicoCrono("Desmarcar todo", tinta = SLATE_600) { marcadas = emptySet() }
                }
                Spacer(Modifier.height(6.dp))
                Text(if (marcadas.isEmpty()) "Tocá las viñetas que se sustituyen" else "${marcadas.size} viñetas marcadas para sustituir",
                    fontSize = 12.sp, fontWeight = FontWeight.Black, color = AMBAR_800)
                Spacer(Modifier.height(6.dp))
                Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, c.borde(), RoundedCornerShape(12.dp)).verticalScroll(rememberScrollState())) {
                    lista.forEachIndexed { k, v ->
                        val vid = v.i("id") ?: return@forEachIndexed
                        val m = vid in marcadas
                        Row(
                            Modifier.fillMaxWidth().background(if (m) AMBAR_50 else Color.Transparent).clickable { marcadas = if (m) marcadas - vid else marcadas + vid }.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = m, onCheckedChange = { marcadas = if (m) marcadas - vid else marcadas + vid }, modifier = Modifier.size(28.dp))
                            Text("${k + 1}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.texto(), modifier = Modifier.width(28.dp))
                            Text(km2(v.d("distanciaAcumuladaKm")), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = c.texto(), modifier = Modifier.width(56.dp))
                            val txt = listOf(v.s("descripcion"), v.s("notas")).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "sin texto" }
                            Text(txt, fontSize = 12.sp, color = SLATE_500, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            v.s("etiquetaAnexo").takeIf { it.isNotBlank() }?.let {
                                Text(it, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AMARILLO_ANEXO).padding(horizontal = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    error?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ROSE_600).padding(10.dp))
    }
    Spacer(Modifier.height(16.dp))
    val principal = when {
        guardando -> "Guardando…"
        corrigiendo -> "Guardar"
        tipo == "TRAMO" -> "Emitir y editar viñetas"
        else -> "Emitir comunicado"
    }
    BotonesPanel(principal, fondo = AMARILLO_ANEXO, tinta = Color.Black, habilitado = !guardando, onCancelar = onCerrar) {
        val horaOk = if (hora.isBlank()) "" else (horaDeTexto(hora) ?: hora)
        guardando = true; error = null
        if (corrigiendo) {
            viewModel.pedir("PUT", "/api/anexos/${anexo!!.i("id")}", JsonObject().apply {
                addProperty("titulo", titulo.trim().uppercase()); addProperty("texto", texto); addProperty("hora", horaOk)
            }) { ok, _ -> guardando = false; if (ok) onListo(null) }
        } else {
            viewModel.pedir("POST", "/api/etapas/$etapaId/anexos", JsonObject().apply {
                addProperty("tipo", tipo); addProperty("titulo", titulo.trim().uppercase()); addProperty("texto", texto); addProperty("hora", horaOk)
                if (tipo == "TRAMO") {
                    ponerNumero("tramoId", tramoId)
                    add("indicacionIds", JsonArray().apply { marcadas.forEach { add(it) } })
                } else { add("tramoId", JsonNull.INSTANCE); add("indicacionIds", JsonNull.INSTANCE) }
            }) { ok, resp ->
                guardando = false
                if (ok) onListo(resp?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject().apply { addProperty("tipo", tipo); tramoId?.let { addProperty("tramoId", it) } })
            }
        }
    }
}

// ------------------------------------------------------- panel "Editar Etapa Oficial"
private const val ALFABETO_CLAVE = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

@Composable
private fun CampoFecha(fecha: String, onCambio: (String) -> Unit) {
    val contexto = LocalContext.current
    val c = ColoresPlataforma
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, SLATE_300, RoundedCornerShape(12.dp)).clickable {
            val d = runCatching { java.time.LocalDate.parse(fecha.take(10)) }.getOrNull() ?: java.time.LocalDate.now()
            android.app.DatePickerDialog(contexto, { _, y, m, dia -> onCambio(String.format("%04d-%02d-%02d", y, m + 1, dia)) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
        }.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(fechaCorta(fecha).ifBlank { "dd/mm/aaaa" }, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = if (fecha.isBlank()) SLATE_400 else c.texto(), modifier = Modifier.weight(1f))
        Icon(Icons.Default.CalendarMonth, contentDescription = "Elegir fecha", tint = SLATE_500, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PanelEditarDia(etapa: JsonObject, viewModel: PlataformaViewModel, onCerrar: () -> Unit) {
    val id = etapa.i("id") ?: 0
    var numero by remember { mutableStateOf(etapa.i("numero")?.toString() ?: "") }
    var nombre by remember { mutableStateOf(etapa.s("nombre")) }
    var fecha by remember { mutableStateOf(etapa.s("fecha").take(10)) }
    var clave by remember { mutableStateOf(etapa.s("claveLibro")) }
    var entrega by remember { mutableStateOf(etapa.s("horaEntregaLibro").take(5)) }
    TituloPanel("Editar Etapa Oficial", "", onCerrar = onCerrar)
    Rotulo("Número de Etapa:")
    OutlinedTextField(numero, { numero = it.filter { c -> c.isDigit() } }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    Spacer(Modifier.height(10.dp))
    Rotulo("Nombre Oficial de la Etapa:")
    OutlinedTextField(nombre, { nombre = it.uppercase() }, singleLine = true, placeholder = { Text("Ej: Etapa 1") }, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
    Spacer(Modifier.height(10.dp))
    Rotulo("Fecha de la Etapa:")
    CampoFecha(fecha) { fecha = it }
    Spacer(Modifier.height(14.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SLATE_50).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text("LIBRO DE RUTA CON CONTRASEÑA (OPCIONAL)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SLATE_800)
        Spacer(Modifier.height(4.dp))
        Text("Si le ponés contraseña, en la app esta etapa llega cerrada y se abre recién cuando la tripulación escribe la contraseña, que se publica a la hora de entrega. Vacía: la etapa se ve siempre.",
            fontSize = 11.sp, color = SLATE_500)
        Spacer(Modifier.height(8.dp))
        Text("Contraseña:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                clave, { clave = it.uppercase().replace(" ", "").take(30) }, singleLine = true, placeholder = { Text("Sin contraseña") },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace), modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )
            OutlinedButton(onClick = { clave = (1..6).map { ALFABETO_CLAVE.random() }.joinToString("") }) { Text("Generar", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Hora de entrega:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SLATE_800, modifier = Modifier.alphaSi(clave.isNotBlank()))
        CampoHora(entrega, { entrega = it }, habilitado = clave.isNotBlank(), modifier = Modifier.width(140.dp).alphaSi(clave.isNotBlank()))
    }
    Spacer(Modifier.height(16.dp))
    BotonesPanel("Guardar Cambios", habilitado = numero.isNotBlank() && nombre.isNotBlank() && fecha.isNotBlank(), onCancelar = onCerrar) {
        viewModel.pedir("PUT", "/api/etapas/$id", JsonObject().apply {
            addProperty("numero", numero.toIntOrNull() ?: 0); addProperty("nombre", nombre.trim().uppercase()); addProperty("fecha", fecha)
        }) { ok, _ ->
            if (ok) viewModel.pedir("PUT", "/api/etapas/$id/clave-libro", JsonObject().apply {
                addProperty("clave", clave.trim())
                if (clave.isBlank() || entrega.isBlank()) add("horaEntrega", JsonNull.INSTANCE) else addProperty("horaEntrega", horaDeTexto(entrega) ?: entrega)
            }, exito = "¡Día actualizado con éxito!") { ok2, _ -> if (ok2) onCerrar() }
        }
    }
}

@Composable
private fun PanelNuevoDia(numeroInicial: Int, viewModel: PlataformaViewModel, onCerrar: () -> Unit) {
    val alcance = rememberCoroutineScope()
    var numero by remember { mutableStateOf(numeroInicial.toString()) }
    var nombre by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    TituloPanel("Nueva Etapa Oficial", "", onCerrar = onCerrar)
    Rotulo("Número de Etapa:")
    OutlinedTextField(numero, { numero = it.filter { c -> c.isDigit() } }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    Spacer(Modifier.height(10.dp))
    Rotulo("Nombre de la Etapa:")
    OutlinedTextField(nombre, { nombre = it }, singleLine = true, placeholder = { Text("Ej: Etapa 2") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    Rotulo("Fecha:")
    CampoFecha(fecha) { fecha = it }
    Spacer(Modifier.height(16.dp))
    BotonesPanel("Crear Etapa", habilitado = numero.isNotBlank() && nombre.isNotBlank(), onCancelar = onCerrar) {
        val n = nombre.trim().uppercase()
        alcance.launch {
            val id = viewModel.idRally() ?: return@launch
            viewModel.pedir("POST", "/api/rallies/$id/etapas", JsonObject().apply {
                addProperty("numero", numero.toIntOrNull() ?: numeroInicial); addProperty("nombre", n)
                if (fecha.isBlank()) add("fecha", JsonNull.INSTANCE) else addProperty("fecha", fecha)
                add("tramosIdsAClonar", JsonArray())
            }, exito = "¡Etapa \"$n\" creada con éxito!") { ok, _ -> if (ok) onCerrar() }
        }
    }
}
