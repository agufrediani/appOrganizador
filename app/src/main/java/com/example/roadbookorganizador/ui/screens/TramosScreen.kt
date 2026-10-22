package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.TramosViewModel

@OptIn(ExperimentalMaterial3Api::class)
/**
 * TRAMOS del rally, como la pestaña de la plataforma (ListaTramos.tsx): un cartel con el
 * título, "Crear tramo" y el resumen (cantidad y km de P.E. y de enlaces), y la lista con
 * Tipo · Identificador · Nombre · Km · Acciones (Editar, Datos, Eliminar).
 * En el celular cada tramo va en una tarjeta en lugar de la tabla.
 *
 * [conCabeceraPropia] = false cuando va adentro de MarcoRally (la cabecera y las pestañas
 * las pone el marco).
 */
@Composable
fun TramosScreen(
    viewModel: TramosViewModel,
    onNavigateToCockpit: (Long) -> Unit,
    onVolver: () -> Unit,
    conCabeceraPropia: Boolean = true
) {
    val tramos by viewModel.tramos.collectAsState()
    val contexto = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.avisos.collect { android.widget.Toast.makeText(contexto, it, android.widget.Toast.LENGTH_LONG).show() }
    }

    var mostrarModalNuevoTramo by remember { mutableStateOf(false) }
    var tramoParaEditar by remember { mutableStateOf<TramoEntity?>(null) }
    var tramoParaBorrar by remember { mutableStateOf<TramoEntity?>(null) }
    val paqueteImportado by viewModel.paqueteImportado.collectAsState()
    // Elegir el archivo del tramo (de otra tablet, de la SD, de Descargas, de WhatsApp…)
    val elegirArchivo = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.leerArchivo(uri) }

    val c = com.example.roadbookorganizador.ui.components.ColoresPlataforma
    val isDark = ThemeManager.isDarkTheme
    val chico = com.example.roadbookorganizador.ui.components.tamanoPantalla() == com.example.roadbookorganizador.ui.components.TamanoPantalla.CHICO
    fun kmDe(t: TramoEntity) = if (t.distanciaMedidaReal > 0.0) t.distanciaMedidaReal else t.distanciaTotalEstimada
    fun km(v: Double) = String.format(java.util.Locale.US, "%.2f", v).replace('.', ',')
    val pe = tramos.filter { it.tipo == "PE" }
    val enlaces = tramos.filter { it.tipo != "PE" }

    Column(modifier = Modifier.fillMaxSize().background(c.fondo())) {
        if (conCabeceraPropia) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = c.texto())
                }
                Spacer(Modifier.weight(1f))
                com.example.roadbookorganizador.ui.components.BotonTema()
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(if (chico) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (chico) 14.dp else 24.dp)
        ) {
            // Cartel: título, botón y resumen
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.tarjeta())
                        .border(1.dp, c.borde(), RoundedCornerShape(24.dp))
                        .padding(if (chico) 16.dp else 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = c.celeste, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("TRAMOS", fontSize = if (chico) 18.sp else 24.sp, fontWeight = FontWeight.Black, color = c.texto(), letterSpacing = 0.6.sp)
                            }
                            Text(
                                "Cada trazado se carga una sola vez. Tocá Editar para trazarlo o taquearlo en el terreno.",
                                fontSize = 12.sp, color = c.apagado()
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        // Importar tramo, como la plataforma (archivo .json de otra tablet o de la web)
                        OutlinedButton(
                            onClick = { elegirArchivo.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, c.borde()),
                            contentPadding = PaddingValues(horizontal = if (chico) 10.dp else 16.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Importar tramo", tint = c.texto(), modifier = Modifier.size(16.dp))
                            if (!chico) {
                                Spacer(Modifier.width(6.dp))
                                Text("IMPORTAR TRAMO", fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.texto(), letterSpacing = 0.6.sp)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { mostrarModalNuevoTramo = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.celeste, contentColor = c.fondoOscuro),
                            contentPadding = PaddingValues(horizontal = if (chico) 14.dp else 20.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (chico) "CREAR" else "CREAR TRAMO", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("${pe.size} P.E. · ${km(pe.sumOf { kmDe(it) })} km", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = c.celeste)
                        Text("${enlaces.size} enlaces · ${km(enlaces.sumOf { kmDe(it) })} km", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = c.amarillo)
                    }
                }
            }

            if (tramos.isEmpty()) {
                item {
                    Text(
                        "Todavía no hay tramos. Creá el primero con \u201cCrear tramo\u201d.",
                        fontSize = 14.sp, color = c.apagado(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(c.tarjeta())
                            .border(1.dp, c.borde(), RoundedCornerShape(24.dp))
                            .padding(32.dp)
                    )
                }
            } else if (!chico) {
                // Tabla, como la plataforma
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(c.tarjeta())
                            .border(1.dp, c.borde(), RoundedCornerShape(24.dp))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("TIPO" to 0.12f, "IDENTIFICADOR" to 0.18f, "NOMBRE" to 0.34f, "KM" to 0.12f, "ACCIONES" to 0.24f).forEach { (t, w) ->
                                Text(t, fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.apagado(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(w))
                            }
                        }
                        tramos.forEach { t ->
                            HorizontalDivider(color = c.borde())
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(0.12f), contentAlignment = Alignment.Center) { ChipTipoTramo(t.tipo) }
                                Text(t.identificador, fontSize = 13.sp, fontWeight = FontWeight.Black, color = c.texto(),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(0.18f))
                                Text(t.nombre, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.texto(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(0.34f))
                                Text("${km(kmDe(t))} km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.texto(),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(0.12f))
                                Row(Modifier.weight(0.24f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    AccionesTramo(
                                        onEditar = { onNavigateToCockpit(t.id) },
                                        onDatos = { tramoParaEditar = t },
                                        onEliminar = { tramoParaBorrar = t }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Celular: una tarjeta por tramo
                items(tramos, key = { it.id }) { t ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.tarjeta())
                            .border(1.dp, c.borde(), RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChipTipoTramo(t.tipo)
                            Spacer(Modifier.width(8.dp))
                            Text(t.identificador, fontSize = 13.sp, fontWeight = FontWeight.Black, color = c.texto(),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Spacer(Modifier.weight(1f))
                            Text("${km(kmDe(t))} km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.texto(),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(t.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.texto())
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccionesTramo(
                                onEditar = { onNavigateToCockpit(t.id) },
                                onDatos = { tramoParaEditar = t },
                                onEliminar = { tramoParaBorrar = t }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarModalNuevoTramo) {
        DialogoNuevoTramo(
            onDismiss = { mostrarModalNuevoTramo = false },
            onConfirmar = { tipo, ident, nombre, sector, chIni, chFin, dist, tOtor, atraso, hora ->
                viewModel.crearTramo(tipo, ident, nombre, sector, chIni, chFin, dist, tOtor, atraso, hora)
                mostrarModalNuevoTramo = false
            }
        )
    }

    tramoParaEditar?.let { tramo ->
        DialogoEditarTramo(
            tramo = tramo,
            onDismiss = { tramoParaEditar = null },
            onConfirmar = { tipo, ident, nombre, sector, chIni, chFin, dist, tOtor, atraso, hora ->
                viewModel.actualizarTramo(tramo, tipo, ident, nombre, sector, chIni, chFin, dist, tOtor, atraso, hora)
                tramoParaEditar = null
            }
        )
    }

    paqueteImportado?.let { paquete ->
        DialogoImportarTramo(
            paquete = paquete,
            repetido = { tipo, ident, nombre -> viewModel.tramoRepetido(tipo, ident, nombre) != null },
            onCancelar = { viewModel.cancelarImportacion() },
            onImportar = { tipo, ident, nombre -> viewModel.confirmarImportacion(tipo, ident, nombre) }
        )
    }

    tramoParaBorrar?.let { t ->
        AlertDialog(
            onDismissRequest = { tramoParaBorrar = null },
            title = { Text("¿Eliminar el tramo \"${t.nombre}\"?", fontWeight = FontWeight.Black) },
            text = { Text("Se borran también todas sus viñetas de esta tablet.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.eliminarTramo(t); tramoParaBorrar = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) { Text("ELIMINAR", fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { tramoParaBorrar = null }) { Text("CANCELAR") } }
        )
    }
}

/**
 * Importar tramo (como el panel "Importar tramo" de la plataforma): muestra cuántas viñetas
 * trae y deja elegir tipo, identificador y nombre; avisa si ya hay uno igual en el rally.
 */
@Composable
private fun DialogoImportarTramo(
    paquete: com.example.roadbookorganizador.util.PaqueteTramo,
    repetido: (String, String, String) -> Boolean,
    onCancelar: () -> Unit,
    onImportar: (String, String, String) -> Unit
) {
    var tipo by remember { mutableStateOf(paquete.tramo.tipo) }
    var identificador by remember { mutableStateOf(paquete.tramo.identificador.uppercase()) }
    var nombre by remember { mutableStateOf(paquete.tramo.nombre.uppercase()) }
    val yaEsta = repetido(tipo, identificador, nombre)
    val c = com.example.roadbookorganizador.ui.components.ColoresPlataforma
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = c.celeste)
                Spacer(Modifier.width(8.dp))
                Text("IMPORTAR TRAMO", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${paquete.vinetas.size} viñetas. Se guarda como una copia: el original no se toca.", fontSize = 12.sp, color = c.apagado())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PE", "ENLACE").forEach { t ->
                        val sel = tipo == t
                        Text(
                            t, fontSize = 12.sp, fontWeight = FontWeight.Black,
                            color = if (sel) c.fondoOscuro else c.apagado(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) c.celeste else Color.Transparent)
                                .border(1.dp, if (sel) c.celeste else c.borde(), RoundedCornerShape(12.dp))
                                .clickable { tipo = t }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = identificador, onValueChange = { identificador = it.uppercase() },
                    label = { Text("Identificador") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it.uppercase() },
                    label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (yaEsta) {
                    Text("Ya hay un tramo $identificador · $nombre en este rally. Cambiale el identificador o el nombre.",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImportar(tipo, identificador, nombre) },
                enabled = !yaEsta && (identificador.isNotBlank() || nombre.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = c.celeste, contentColor = c.fondoOscuro)
            ) { Text("IMPORTAR", fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("CANCELAR") } }
    )
}

/** "PE" en celeste y "ENLACE" en amarillo, como la plataforma. */
@Composable
private fun ChipTipoTramo(tipo: String) {
    val esPe = tipo == "PE"
    val color = if (esPe) Color(0xFF0284C7) else Color(0xFFD97706)
    val borde = if (esPe) Color(0x6600B4D8) else Color(0x66FFB703)
    val fondo = if (esPe) Color(0x3300B4D8) else Color(0x33FFB703)
    Text(
        if (esPe) "PE" else "ENLACE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = color,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(fondo).border(1.dp, borde, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Editar (abre el editor), Datos (los datos del tramo) y Eliminar, como la plataforma. */
@Composable
private fun AccionesTramo(onEditar: () -> Unit, onDatos: () -> Unit, onEliminar: () -> Unit) {
    val c = com.example.roadbookorganizador.ui.components.ColoresPlataforma
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.celeste).clickable { onEditar() }.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = c.fondoOscuro, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Editar", fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.fondoOscuro)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, c.borde(), RoundedCornerShape(12.dp)).clickable { onDatos() }
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = c.apagado(), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Datos", fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.apagado())
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0x66F43F5E), RoundedCornerShape(12.dp)).clickable { onEliminar() }
        ) { Icon(Icons.Default.Delete, contentDescription = "Eliminar el tramo", tint = Color(0xFFF43F5E), modifier = Modifier.size(15.dp)) }
    }
}

/**
 * Tarjeta de tramo diseñada fielmente según la Plataforma Web Oficial (media_1789650471010.png)
 */
@Composable
fun TramoCardWebStyle(
    tramo: TramoEntity,
    onIniciarTrazado: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onIniciarTrazado() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FredianiLightCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // COLUMNA 1: HORA 1° AUTO (Pill oscuro idéntico a web)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "1° AUTO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = FredianiCyan,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = tramo.horaPrimerAuto.ifEmpty { "--:--" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }

            // COLUMNA 2: INFORMACIÓN DEL TRAMO Y METADATOS
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Fila de Badges: ID + PE/ENLACE + Sector & CH
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Badge Identificador (Negro)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = tramo.identificador.ifEmpty { if (tramo.tipo == "PE") "P.E. 1" else "E 1" },
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Badge Tipo Oficial (Cyan o Amber)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (tramo.tipo == "PE") FredianiCyanBg else FredianiAmberBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (tramo.tipo == "PE") FredianiCyan.copy(alpha = 0.3f) else FredianiAmber.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = tramo.tipo,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = if (tramo.tipo == "PE") FredianiCyanText else FredianiAmberText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Sector y CH
                    Text(
                        text = "Sector ${String.format("%02d", tramo.numeroSector)} • ${tramo.chInicio} ➔ ${tramo.chFin}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )

                    // Versión
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "v1.0",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Nombre del Tramo (con ícono de lápiz para edición rápida)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onEditar() }
                ) {
                    Text(
                        text = tramo.nombre.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar tramo",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Fila de Metadatos: Distancia, T. Otorgado, Atraso Máx
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Distancia: ${String.format("%.2f", if (tramo.distanciaMedidaReal > 0.0) tramo.distanciaMedidaReal else tramo.distanciaTotalEstimada)} Km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "T. Otorgado: ${tramo.tiempoOtorgado}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    if (tramo.tipo == "PE" && tramo.atrasoMaximo.isNotEmpty()) {
                        Text(
                            text = "Atraso Máx: ${tramo.atrasoMaximo}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Organizador
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = FredianiCyanText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Organizador: ",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Augusto Frediani",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FredianiCyanText
                    )
                }
            }

            // COLUMNA 3: BOTONES DE ACCIÓN OFICIALES
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Principal de Trazado
                if (tramo.tipo == "PE") {
                    Button(
                        onClick = onIniciarTrazado,
                        colors = ButtonDefaults.buttonColors(containerColor = FredianiCyan),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tramo.estadoTrazado == "EN_TRAZADO") "CONTINUAR" else "VER INDICACIONES",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                } else {
                    Button(
                        onClick = onIniciarTrazado,
                        colors = ButtonDefaults.buttonColors(containerColor = FredianiAmberBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FredianiAmber.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            tint = FredianiAmberText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HOJA ENLACE",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = FredianiAmberText
                        )
                    }
                }

                // Botón Editar Info
                OutlinedButton(
                    onClick = onEditar,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF334155)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Editar Info",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Editar Info",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF334155)
                    )
                }

                // Botón Eliminar
                IconButton(onClick = onEliminar) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Diálogo para crear un nuevo tramo con todos los campos oficiales del ecosistema web
 */
@Composable
fun DialogoNuevoTramo(
    onDismiss: () -> Unit,
    onConfirmar: (String, String, String, Int, String, String, Double, String, String, String) -> Unit
) {
    var tipo by remember { mutableStateOf("PE") }
    var identificador by remember { mutableStateOf("P.E. 1") }
    var nombre by remember { mutableStateOf("") }
    var numeroSector by remember { mutableStateOf("1") }
    var chInicio by remember { mutableStateOf("CH 01") }
    var chFin by remember { mutableStateOf("CH 02") }
    var distanciaEstimada by remember { mutableStateOf("10.0") }
    var tiempoOtorgado by remember { mutableStateOf("25'") }
    var atrasoMaximo by remember { mutableStateOf("10'") }
    var horaPrimerAuto by remember { mutableStateOf("09:00") }

    val isDark = ThemeManager.isDarkTheme

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) RallyCardBg else Color.White,
        title = {
            Text("Nuevo Tramo Oficial (Itinerario)", color = if (isDark) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector PE / ENLACE
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { tipo = "PE"; if (identificador.startsWith("E")) identificador = "P.E. 1" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipo == "PE") FredianiCyan else (if (isDark) RallySurface else Color(0xFFF1F5F9))
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PRUEBA ESPECIAL (PE)", color = if (tipo == "PE") Color.Black else (if (isDark) Color.White else Color(0xFF0F172A)), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { tipo = "ENLACE"; if (identificador.startsWith("P.E.")) identificador = "E 1" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipo == "ENLACE") FredianiAmber else (if (isDark) RallySurface else Color(0xFFF1F5F9))
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ENLACE", color = if (tipo == "ENLACE") Color.Black else (if (isDark) Color.White else Color(0xFF0F172A)), fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = identificador,
                    onValueChange = { identificador = it },
                    label = { Text("Identificador (ej: P.E. 1, E 1)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = FredianiCyanText,
                        unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Tramo (ej: PARQUE GIORGI)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = FredianiCyanText,
                        unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numeroSector,
                        onValueChange = { numeroSector = it },
                        label = { Text("Sector N°") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = distanciaEstimada,
                        onValueChange = { distanciaEstimada = it },
                        label = { Text("Distancia Estimada (km)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chInicio,
                        onValueChange = { chInicio = it },
                        label = { Text("CH Inicio") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = chFin,
                        onValueChange = { chFin = it },
                        label = { Text("CH Fin") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tiempoOtorgado,
                        onValueChange = { tiempoOtorgado = it },
                        label = { Text("Tiempo Otorgado (ej: 25')") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = atrasoMaximo,
                        onValueChange = { atrasoMaximo = it },
                        label = { Text("Atraso Máximo (ej: 10')") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = horaPrimerAuto,
                    onValueChange = { horaPrimerAuto = it },
                    label = { Text("Hora Paso 1° Auto (ej: 09:00)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = FredianiCyanText,
                        unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dist = distanciaEstimada.toDoubleOrNull() ?: 0.0
                    val sector = numeroSector.toIntOrNull() ?: 1
                    onConfirmar(tipo, identificador, nombre, sector, chInicio, chFin, dist, tiempoOtorgado, atrasoMaximo, horaPrimerAuto)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FredianiCyan)
            ) {
                Text("CREAR TRAMO", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

/**
 * Diálogo para editar información completa de un tramo existente
 */
@Composable
fun DialogoEditarTramo(
    tramo: TramoEntity,
    onDismiss: () -> Unit,
    onConfirmar: (String, String, String, Int, String, String, Double, String, String, String) -> Unit
) {
    var tipo by remember { mutableStateOf(tramo.tipo) }
    var identificador by remember { mutableStateOf(tramo.identificador) }
    var nombre by remember { mutableStateOf(tramo.nombre) }
    var numeroSector by remember { mutableStateOf(tramo.numeroSector.toString()) }
    var chInicio by remember { mutableStateOf(tramo.chInicio) }
    var chFin by remember { mutableStateOf(tramo.chFin) }
    var distanciaEstimada by remember { mutableStateOf(tramo.distanciaTotalEstimada.toString()) }
    var tiempoOtorgado by remember { mutableStateOf(tramo.tiempoOtorgado) }
    var atrasoMaximo by remember { mutableStateOf(tramo.atrasoMaximo) }
    var horaPrimerAuto by remember { mutableStateOf(tramo.horaPrimerAuto) }

    val isDark = ThemeManager.isDarkTheme

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) RallyCardBg else Color.White,
        title = {
            Text("Editar Información del Tramo", color = if (isDark) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector PE / ENLACE
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { tipo = "PE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipo == "PE") FredianiCyan else (if (isDark) RallySurface else Color(0xFFF1F5F9))
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PRUEBA ESPECIAL (PE)", color = if (tipo == "PE") Color.Black else (if (isDark) Color.White else Color(0xFF0F172A)), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { tipo = "ENLACE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipo == "ENLACE") FredianiAmber else (if (isDark) RallySurface else Color(0xFFF1F5F9))
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ENLACE", color = if (tipo == "ENLACE") Color.Black else (if (isDark) Color.White else Color(0xFF0F172A)), fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = identificador,
                    onValueChange = { identificador = it },
                    label = { Text("Identificador") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = FredianiCyanText,
                        unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Tramo") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = FredianiCyanText,
                        unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numeroSector,
                        onValueChange = { numeroSector = it },
                        label = { Text("Sector N°") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = distanciaEstimada,
                        onValueChange = { distanciaEstimada = it },
                        label = { Text("Distancia Estimada (km)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chInicio,
                        onValueChange = { chInicio = it },
                        label = { Text("CH Inicio") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = chFin,
                        onValueChange = { chFin = it },
                        label = { Text("CH Fin") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tiempoOtorgado,
                        onValueChange = { tiempoOtorgado = it },
                        label = { Text("Tiempo Otorgado") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = atrasoMaximo,
                        onValueChange = { atrasoMaximo = it },
                        label = { Text("Atraso Máximo") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = FredianiCyanText,
                            unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = horaPrimerAuto,
                    onValueChange = { horaPrimerAuto = it },
                    label = { Text("Hora Paso 1° Auto") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = FredianiCyanText,
                        unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dist = distanciaEstimada.toDoubleOrNull() ?: tramo.distanciaTotalEstimada
                    val sector = numeroSector.toIntOrNull() ?: tramo.numeroSector
                    onConfirmar(tipo, identificador, nombre, sector, chInicio, chFin, dist, tiempoOtorgado, atrasoMaximo, horaPrimerAuto)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FredianiCyan)
            ) {
                Text("GUARDAR CAMBIOS", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
