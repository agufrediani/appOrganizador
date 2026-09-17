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
@Composable
fun TramosScreen(
    viewModel: TramosViewModel,
    onNavigateToCockpit: (Long) -> Unit,
    onVolver: () -> Unit
) {
    val tramos by viewModel.tramos.collectAsState()
    val rallyActivo by viewModel.rallyActivo.collectAsState()

    var mostrarModalNuevoTramo by remember { mutableStateOf(false) }
    var tramoParaEditar by remember { mutableStateOf<TramoEntity?>(null) }

    // Calcular estadísticas de tramos del rally
    val kmPe = tramos.filter { it.tipo == "PE" }.sumOf { if (it.distanciaMedidaReal > 0.0) it.distanciaMedidaReal else it.distanciaTotalEstimada }
    val kmEnlace = tramos.filter { it.tipo == "ENLACE" }.sumOf { if (it.distanciaMedidaReal > 0.0) it.distanciaMedidaReal else it.distanciaTotalEstimada }
    val kmTotal = kmPe + kmEnlace

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CRONOGRAMA & ITINERARIO OFICIAL",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = FredianiCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Secuencia cronológica oficial: pruebas especiales y enlaces",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al Menú",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RallyDarkBg)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarModalNuevoTramo = true },
                containerColor = FredianiCyan,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null) },
                text = { Text("+ NUEVO TRAMO", fontWeight = FontWeight.Black) }
            )
        },
        containerColor = RallyDarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header con información del Rally y Resumen Kilométrico FIA
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RallyCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = rallyActivo?.nombre ?: "Rally Seleccionado",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (rallyActivo != null) "${rallyActivo?.organizadorClub.orEmpty()} • ${rallyActivo?.sede.orEmpty()} • ${rallyActivo?.fecha.orEmpty()}" else "Tramos disponibles para relevamiento",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        // Badges métricas FIA oficiales (idéntico a la web)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FredianiCyanBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, FredianiCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "PE: ${String.format("%.2f", kmPe)} Km",
                                    color = FredianiCyanText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FredianiAmberBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, FredianiAmber.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Enlaces: ${String.format("%.2f", kmEnlace)} Km",
                                    color = FredianiAmberText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FredianiGreenBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, FredianiGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Total: ${String.format("%.2f", kmTotal)} Km",
                                    color = FredianiGreenText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SECUENCIA DE TRAMOS Y PRUEBAS ESPECIALES (${tramos.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = FredianiCyan,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (tramos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay tramos registrados en este rally. Pulsa '+ Nuevo Tramo' para comenzar.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tramos, key = { it.id }) { tramo ->
                        TramoCardWebStyle(
                            tramo = tramo,
                            onIniciarTrazado = { onNavigateToCockpit(tramo.id) },
                            onEditar = { tramoParaEditar = tramo },
                            onEliminar = { viewModel.eliminarTramo(tramo) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
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
        modifier = Modifier.fillMaxWidth(),
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RallyCardBg,
        title = {
            Text("Nuevo Tramo Oficial (Itinerario)", color = Color.White, fontWeight = FontWeight.Bold)
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
                            containerColor = if (tipo == "PE") FredianiCyan else RallySurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PRUEBA ESPECIAL (PE)", color = if (tipo == "PE") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { tipo = "ENLACE"; if (identificador.startsWith("P.E.")) identificador = "E 1" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipo == "ENLACE") FredianiAmber else RallySurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ENLACE", color = if (tipo == "ENLACE") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = identificador,
                    onValueChange = { identificador = it },
                    label = { Text("Identificador (ej: P.E. 1, E 1)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = FredianiCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Tramo (ej: PARQUE GIORGI)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = FredianiCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numeroSector,
                        onValueChange = { numeroSector = it },
                        label = { Text("Sector N°") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = distanciaEstimada,
                        onValueChange = { distanciaEstimada = it },
                        label = { Text("Distancia Estimada (km)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
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
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = chFin,
                        onValueChange = { chFin = it },
                        label = { Text("CH Fin") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
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
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = atrasoMaximo,
                        onValueChange = { atrasoMaximo = it },
                        label = { Text("Atraso Máximo (ej: 10')") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = horaPrimerAuto,
                    onValueChange = { horaPrimerAuto = it },
                    label = { Text("Hora Paso 1° Auto (ej: 09:00)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = FredianiCyan
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RallyCardBg,
        title = {
            Text("Editar Información del Tramo", color = Color.White, fontWeight = FontWeight.Bold)
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
                            containerColor = if (tipo == "PE") FredianiCyan else RallySurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PRUEBA ESPECIAL (PE)", color = if (tipo == "PE") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { tipo = "ENLACE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipo == "ENLACE") FredianiAmber else RallySurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ENLACE", color = if (tipo == "ENLACE") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = identificador,
                    onValueChange = { identificador = it },
                    label = { Text("Identificador") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = FredianiCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Tramo") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = FredianiCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numeroSector,
                        onValueChange = { numeroSector = it },
                        label = { Text("Sector N°") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = distanciaEstimada,
                        onValueChange = { distanciaEstimada = it },
                        label = { Text("Distancia Estimada (km)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
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
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = chFin,
                        onValueChange = { chFin = it },
                        label = { Text("CH Fin") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
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
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = atrasoMaximo,
                        onValueChange = { atrasoMaximo = it },
                        label = { Text("Atraso Máximo") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = FredianiCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = horaPrimerAuto,
                    onValueChange = { horaPrimerAuto = it },
                    label = { Text("Hora Paso 1° Auto") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = FredianiCyan
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
