package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.data.model.DrawingData
import com.example.roadbookorganizador.data.model.PointData
import com.example.roadbookorganizador.data.model.StampData
import com.example.roadbookorganizador.data.model.StrokeData
import com.example.roadbookorganizador.ui.theme.*

enum class ModoPestanaNotas {
    TEXTO,
    DIBUJO_CROQUIS
}

@Composable
fun RoadbookNotesEditor(
    vineta: VinetaEntity,
    onGuardar: (nota: String, peligro: String, distTotal: Double, distParcial: Double, notasSvgData: String) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pestanaActiva by remember { mutableStateOf(ModoPestanaNotas.TEXTO) }

    var nota by remember(vineta) { mutableStateOf(vineta.informacion) }
    var peligro by remember(vineta) { mutableStateOf(vineta.peligro) }
    var distTotalStr by remember(vineta) { mutableStateOf(String.format("%.3f", vineta.distanciaTotal)) }
    var distParcialStr by remember(vineta) { mutableStateOf(String.format("%.3f", vineta.distanciaParcial)) }

    // Estado del dibujo / croquis para notas
    val initialDrawing = remember(vineta.notasSvgData) {
        DrawingData.fromJson(vineta.notasSvgData)
    }
    var strokes by remember(initialDrawing) { mutableStateOf(initialDrawing.strokes.toMutableList()) }
    var stamps by remember(initialDrawing) { mutableStateOf(initialDrawing.stamps.toMutableList()) }
    var currentStrokePoints by remember { mutableStateOf<List<PointData>>(emptyList()) }

    var selectedStampIndex by remember { mutableStateOf<Int?>(null) }
    var dragTarget by remember { mutableStateOf(DragTarget.NONE) }

    var selectedColorHex by remember { mutableStateOf("#000000") }
    var selectedStrokeWidth by remember { mutableStateOf(6f) }
    var selectedCategory by remember { mutableStateOf(CategoriaSellos.CONTROLES) }
    var selectedStampType by remember { mutableStateOf("TC") }
    var stampScale by remember { mutableStateOf(1.0f) }

    val stampsByCategory = remember {
        mapOf(
            CategoriaSellos.CONTROLES to listOf(
                "TC" to "TC Reloj ⏰",
                "LARGADA" to "Largada 🏁",
                "LLEGADA" to "Llegada 🏁",
                "STOP" to "STOP 🛑",
                "RESET_0000" to "0000 Reset 🔢",
                "GPS_POINT" to "Punto GPS 📡",
                "SURTIDOR" to "Combustible ⛽",
                "ASISTENCIA" to "Taller 🔧",
                "MEDICO" to "Médico ✚",
                "RADAR_DZ" to "Radar DZ 🔵",
                "RADAR_FZ" to "Fin FZ ⚪"
            ),
            CategoriaSellos.PELIGROS to listOf(
                "PELIGRO_1" to "Peligro ! ⚠️",
                "PELIGRO_2" to "Peligro !! ⚠️⚠️",
                "PELIGRO_3" to "Peligro !!! 🚨",
                "PRECAUCION" to "Triángulo △"
            ),
            CategoriaSellos.LANDMARKS to listOf(
                "TRANQUERA" to "Tranquera 🚪",
                "GUARDAGANADO" to "Guardaganado 柵",
                "PUENTE" to "Puente 🌉",
                "ALCANTARILLA" to "Alcantarilla 🕳️",
                "VIAS_TREN" to "Vías Tren 🚂",
                "ANTENA" to "Antena 🗼",
                "MOLINO" to "Molino 🌀",
                "CASA" to "Poblado 🏠",
                "IGLESIA" to "Capilla ⛪",
                "CIRCULO" to "Mojón ⚪"
            ),
            CategoriaSellos.TERRENO to listOf(
                "VADO" to "Vado / Agua 💧",
                "SALTO" to "Salto / Lomo 🏎️💨",
                "ZANJA" to "Zanja ⚡",
                "DUNAS" to "Dunas / Arena 🏜️",
                "PIEDRAS" to "Piedras 🪨",
                "BARRO" to "Barro / Huellas 🚜"
            ),
            CategoriaSellos.DIAGRAMAS to listOf(
                "RECTA" to "Recta ↑",
                "CURVA_DER" to "Curva Der ↗",
                "CURVA_IZQ" to "Curva Izq ↖",
                "DERECHA_90" to "Der 90° ↱",
                "IZQUIERDA_90" to "Izq 90° ↰",
                "RETOME_DER" to "Horquilla Der ↷",
                "RETOME_IZQ" to "Horquilla Izq ↶",
                "CRUCE_X" to "Cruce +",
                "CRUCE_T" to "Cruce T"
            )
        )
    }

    val isDark = ThemeManager.isDarkTheme

    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) RallyCardBg else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isDark) RallySurface else FredianiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // CABECERA DEL EDITOR CON PESTAÑAS Y ACCIONES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ANOTACIONES #${vineta.numero}",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = if (isDark) RallyCyan else FredianiCyanText
                    )

                    // Pestañas: [Texto] | [Dibujar Croquis]
                    TabRow(
                        selectedTabIndex = if (pestanaActiva == ModoPestanaNotas.TEXTO) 0 else 1,
                        modifier = Modifier.width(260.dp),
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = pestanaActiva == ModoPestanaNotas.TEXTO,
                            onClick = { pestanaActiva = ModoPestanaNotas.TEXTO },
                            text = { Text("✍️ TEXTO", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        )
                        Tab(
                            selected = pestanaActiva == ModoPestanaNotas.DIBUJO_CROQUIS,
                            onClick = { pestanaActiva = ModoPestanaNotas.DIBUJO_CROQUIS },
                            text = { Text("🎨 DIBUJAR", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onCancelar,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Cerrar", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val tot = distTotalStr.replace(',', '.').toDoubleOrNull() ?: vineta.distanciaTotal
                            val par = distParcialStr.replace(',', '.').toDoubleOrNull() ?: vineta.distanciaParcial
                            val svgJson = if (strokes.isNotEmpty() || stamps.isNotEmpty()) DrawingData(strokes = strokes, stamps = stamps).toJson() else ""
                            onGuardar(nota, peligro, tot, par, svgJson)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GUARDAR", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            if (pestanaActiva == ModoPestanaNotas.TEXTO) {
                // PESTAÑA 1: TEXTO COLOQUIAL Y DATOS
                // GRADO DE PELIGRO
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Peligro:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val opciones = listOf("NORMAL" to "", "! ATENCIÓN" to "! ATENCIÓN", "!! PELIGRO" to "!! PELIGRO", "LARGADA" to "LARGADA")
                    for ((label, valor) in opciones) {
                        val sel = peligro == valor
                        FilterChip(
                            selected = sel,
                            onClick = { peligro = valor },
                            label = { Text(label, fontWeight = if (sel) FontWeight.Black else FontWeight.Normal, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (label) {
                                    "! ATENCIÓN" -> FredianiAmber
                                    "!! PELIGRO" -> RallyRed
                                    "LARGADA" -> FredianiGreen
                                    else -> FredianiCyan
                                },
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // CAMPO DE TEXTO COLOQUIAL
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Texto Coloquial / Referencia de Navegación") },
                    placeholder = { Text("Ej: Atención zanja profunda a la derecha, frenar antes de tranquera...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                // CALIBRACIÓN DE KILOMETRAJES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = distTotalStr,
                        onValueChange = { distTotalStr = it },
                        label = { Text("Km Total") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = distParcialStr,
                        onValueChange = { distParcialStr = it },
                        label = { Text("Km Parcial") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            } else {
                // PESTAÑA 2: LIENZO DE DIBUJO / CROQUIS A MANO ALZADA EN ANOTACIONES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf("#000000", "#EF4444", "#00B4D8", "#22C55E", "#F59E0B")
                        for (cHex in colors) {
                            val c = Color(android.graphics.Color.parseColor(cHex))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (selectedColorHex == cHex) 3.dp else 1.dp,
                                        color = if (selectedColorHex == cHex) FredianiAmber else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = cHex }
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                selectedStrokeWidth = when (selectedStrokeWidth) {
                                    4f -> 7f
                                    7f -> 12f
                                    else -> 4f
                                }
                            }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size((selectedStrokeWidth * 1.5f).dp.coerceIn(5.dp, 16.dp))
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (currentStrokePoints.isNotEmpty()) {
                                    currentStrokePoints = emptyList()
                                } else if (strokes.isNotEmpty()) {
                                    val nl = strokes.toMutableList()
                                    nl.removeAt(nl.lastIndex)
                                    strokes = nl
                                } else if (stamps.isNotEmpty()) {
                                    val nst = stamps.toMutableList()
                                    nst.removeAt(nst.lastIndex)
                                    stamps = nst
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Deshacer", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                strokes = mutableListOf()
                                stamps = mutableListOf()
                                currentStrokePoints = emptyList()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RallyRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Limpiar", fontSize = 10.sp, color = RallyRed)
                        }
                    }
                }

                val selIdx = selectedStampIndex
                val selStamp = if (selIdx != null && selIdx in stamps.indices) stamps[selIdx] else null

                if (selStamp != null && selIdx != null) {
                    // BARRA CONTEXTUAL DEL SELLO SELECCIONADO (ESTILO PC - TAMAÑO EN VIVO Y CONTROL)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) RallySurface else Color(0xFFE0F2FE), RoundedCornerShape(8.dp))
                            .border(1.5.dp, if (isDark) RallyCyan else FredianiCyan, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDark) RallyCyan else FredianiCyan
                        ) {
                            Text(
                                text = "📍 ${selStamp.type}",
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Tamaño:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val nst = stamps.toMutableList()
                                val cur = nst[selIdx]
                                nst[selIdx] = cur.copy(scale = (cur.scale - 0.1f).coerceIn(0.4f, 3.5f))
                                stamps = nst
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }

                        Slider(
                            value = selStamp.scale,
                            onValueChange = { newScale ->
                                val nst = stamps.toMutableList()
                                nst[selIdx] = nst[selIdx].copy(scale = newScale)
                                stamps = nst
                            },
                            valueRange = 0.4f..3.5f,
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val nst = stamps.toMutableList()
                                val cur = nst[selIdx]
                                nst[selIdx] = cur.copy(scale = (cur.scale + 0.1f).coerceIn(0.4f, 3.5f))
                                stamps = nst
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }

                        Text(
                            text = "${(selStamp.scale * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) RallyCyan else FredianiCyanText
                        )

                        OutlinedButton(
                            onClick = {
                                val nst = stamps.toMutableList()
                                if (selIdx in nst.indices) {
                                    nst.removeAt(selIdx)
                                    stamps = nst
                                }
                                selectedStampIndex = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RallyRed),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Eliminar", fontSize = 9.sp, color = RallyRed)
                        }

                        Button(
                            onClick = { selectedStampIndex = null },
                            colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Listo", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // CATÁLOGO DE SELLOS CATEGORIZADO (ESTILO RALLY NAVIGATOR) PARA ANOTACIONES
                    ScrollableTabRow(
                        selectedTabIndex = CategoriaSellos.values().indexOf(selectedCategory),
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.Transparent,
                        divider = {},
                        edgePadding = 0.dp
                    ) {
                        CategoriaSellos.values().forEach { cat ->
                            Tab(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    stampsByCategory[cat]?.firstOrNull()?.let { (cod, _) ->
                                        selectedStampType = cod
                                    }
                                },
                                text = {
                                    Text(
                                        text = cat.titulo,
                                        fontWeight = if (selectedCategory == cat) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 10.sp,
                                        color = if (selectedCategory == cat) (if (isDark) RallyCyan else FredianiCyanText) else Color.Gray
                                    )
                                }
                            )
                        }
                    }

                    // BANDEJA DE SELLOS Y BOTÓN INSERTAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) RallySurface else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val activeCatalog = stampsByCategory[selectedCategory] ?: emptyList()
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(activeCatalog) { (codigo, nombre) ->
                                val sel = selectedStampType == codigo
                                FilterChip(
                                    selected = sel,
                                    onClick = { selectedStampType = codigo },
                                    label = { Text(nombre, fontSize = 9.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FredianiCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val nst = stamps.toMutableList()
                                nst.add(
                                    StampData(
                                        type = selectedStampType,
                                        x = 0.5f,
                                        y = 0.5f,
                                        scale = 1.0f,
                                        rotation = 0.0f
                                    )
                                )
                                stamps = nst
                                selectedStampIndex = nst.lastIndex // Selección inmediata para mover o redimensionar
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) FredianiCyan else FredianiNavy),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = if (isDark) Color.Black else Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("+ Insertar", fontSize = 10.sp, color = if (isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // LIENZO TÁCTIL PARA EL CROQUIS DE NOTAS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                        .pointerInput(stamps, selectedStampIndex) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    if (w <= 0 || h <= 0) return@detectDragGestures

                                    // 1. ¿Toca el manipulador de redimensionar del sello seleccionado?
                                    val curIdx = selectedStampIndex
                                    if (curIdx != null && curIdx in stamps.indices) {
                                        val sel = stamps[curIdx]
                                        val cx = sel.x * w
                                        val cy = sel.y * h
                                        val boxRadius = (45f * sel.scale).coerceAtLeast(35f)
                                        val handleX = cx + boxRadius
                                        val handleY = cy + boxRadius
                                        val distHandle = Math.hypot((offset.x - handleX).toDouble(), (offset.y - handleY).toDouble()).toFloat()
                                        if (distHandle <= 40f) {
                                            dragTarget = DragTarget.RESIZE_STAMP
                                            return@detectDragGestures
                                        }
                                    }

                                    // 2. ¿Toca algún sello existente para seleccionarlo o moverlo?
                                    var hitIndex: Int? = null
                                    for (i in stamps.indices.reversed()) {
                                        val s = stamps[i]
                                        val cx = s.x * w
                                        val cy = s.y * h
                                        val hitRadius = (45f * s.scale).coerceAtLeast(35f)
                                        val dist = Math.hypot((offset.x - cx).toDouble(), (offset.y - cy).toDouble()).toFloat()
                                        if (dist <= hitRadius) {
                                            hitIndex = i
                                            break
                                        }
                                    }

                                    if (hitIndex != null) {
                                        selectedStampIndex = hitIndex
                                        dragTarget = DragTarget.MOVE_STAMP
                                        return@detectDragGestures
                                    }

                                    // 3. Toca lienzo vacío: deselecciona sello e inicia trazo libre
                                    selectedStampIndex = null
                                    dragTarget = DragTarget.DRAW_STROKE
                                    currentStrokePoints = listOf(PointData(offset.x / w, offset.y / h))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    if (w <= 0 || h <= 0) return@detectDragGestures

                                    when (dragTarget) {
                                        DragTarget.MOVE_STAMP -> {
                                            val idx = selectedStampIndex
                                            if (idx != null && idx in stamps.indices) {
                                                val nst = stamps.toMutableList()
                                                val newX = (change.position.x / w).coerceIn(0.05f, 0.95f)
                                                val newY = (change.position.y / h).coerceIn(0.05f, 0.95f)
                                                nst[idx] = nst[idx].copy(x = newX, y = newY)
                                                stamps = nst
                                            }
                                        }
                                        DragTarget.RESIZE_STAMP -> {
                                            val idx = selectedStampIndex
                                            if (idx != null && idx in stamps.indices) {
                                                val cur = stamps[idx]
                                                val cx = cur.x * w
                                                val cy = cur.y * h
                                                val dist = Math.hypot((change.position.x - cx).toDouble(), (change.position.y - cy).toDouble()).toFloat()
                                                val newScale = (dist / 45f).coerceIn(0.4f, 3.5f)
                                                val nst = stamps.toMutableList()
                                                nst[idx] = cur.copy(scale = newScale)
                                                stamps = nst
                                            }
                                        }
                                        DragTarget.DRAW_STROKE -> {
                                            currentStrokePoints = currentStrokePoints + PointData(
                                                (change.position.x / w).coerceIn(0f, 1f),
                                                (change.position.y / h).coerceIn(0f, 1f)
                                            )
                                        }
                                        DragTarget.NONE,
                                        DragTarget.ARROW_P0,
                                        DragTarget.ARROW_P1,
                                        DragTarget.ARROW_P2,
                                        DragTarget.ARROW_P3,
                                        DragTarget.ARROW_PIN,
                                        DragTarget.MOVE_ARROW,
                                        DragTarget.RESIZE_ARROW -> {}
                                    }
                                },
                                onDragEnd = {
                                    if (dragTarget == DragTarget.DRAW_STROKE && currentStrokePoints.size >= 2) {
                                        val nl = strokes.toMutableList()
                                        nl.add(
                                            StrokeData(
                                                points = currentStrokePoints,
                                                colorHex = selectedColorHex,
                                                strokeWidth = selectedStrokeWidth
                                            )
                                        )
                                        strokes = nl
                                    }
                                    currentStrokePoints = emptyList()
                                    dragTarget = DragTarget.NONE
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        TulipaCanvasRenderer.render(
                            drawScope = this,
                            drawingData = DrawingData(strokes = strokes, stamps = stamps),
                            fallbackTulip = "",
                            primaryColor = Color.Black
                        )

                        if (currentStrokePoints.size >= 2) {
                            val strokeColor = try {
                                Color(android.graphics.Color.parseColor(selectedColorHex))
                            } catch (e: Exception) {
                                Color.Black
                            }
                            val path = androidx.compose.ui.graphics.Path()
                            val first = currentStrokePoints[0]
                            path.moveTo(first.x * size.width, first.y * size.height)
                            for (i in 1 until currentStrokePoints.size) {
                                val pt = currentStrokePoints[i]
                                path.lineTo(pt.x * size.width, pt.y * size.height)
                            }
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = selectedStrokeWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }

                        // MARCO DE SELECCIÓN Y MANIPULADOR INTERACTIVO ESTILO PC
                        val currentSel = selectedStampIndex
                        if (currentSel != null && currentSel in stamps.indices) {
                            val sel = stamps[currentSel]
                            val cx = sel.x * size.width
                            val cy = sel.y * size.height
                            val boxRadius = (45f * sel.scale).coerceAtLeast(35f)
                            val left = cx - boxRadius
                            val top = cy - boxRadius
                            val right = cx + boxRadius
                            val bottom = cy + boxRadius

                            drawRect(
                                color = Color(0xFF0284C7),
                                topLeft = Offset(left, top),
                                size = Size(boxRadius * 2, boxRadius * 2),
                                style = Stroke(
                                    width = 3f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                )
                            )

                            val cornerOffsets = listOf(
                                Offset(left, top),
                                Offset(right, top),
                                Offset(left, bottom),
                                Offset(right, bottom)
                            )
                            for (pt in cornerOffsets) {
                                drawCircle(color = Color.White, radius = 5.5f, center = pt)
                                drawCircle(color = Color(0xFF0284C7), radius = 5.5f, center = pt, style = Stroke(width = 2.5f))
                            }

                            drawCircle(color = Color(0xFF0284C7), radius = 11f, center = Offset(right, bottom))
                            drawCircle(color = Color.White, radius = 11f, center = Offset(right, bottom), style = Stroke(width = 2f))
                            drawCircle(color = Color.White, radius = 4f, center = Offset(right, bottom))
                        }
                    }
                }
            }
        }
    }
}
