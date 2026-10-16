package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.vineta.ArrastreSimbolo
import com.example.roadbookorganizador.ui.vineta.BibliotecaVineta
import com.example.roadbookorganizador.ui.vineta.DibujanteSvg
import com.example.roadbookorganizador.ui.vineta.DibujanteVineta
import com.example.roadbookorganizador.ui.vineta.ItemPaleta
import com.example.roadbookorganizador.vineta.ALTO_DIBUJO
import com.example.roadbookorganizador.vineta.Biblioteca
import com.example.roadbookorganizador.vineta.Capa

/** Pestaña de la barra: las flechas armadas, cada categoría de simbolos.json y las notas de texto. */
private data class TabPaleta(val clave: String, val titulo: String, val icono: ImageVector)

private const val TAB_FLECHAS = "FLECHAS"
private const val TAB_NOTAS = "NOTAS_TEXTO"

private fun iconoCategoria(codigo: String): ImageVector = when (codigo) {
    "ROAD_RALLY" -> Icons.Default.CallSplit
    "LANDMARKS" -> Icons.Default.Place
    "TERRAIN" -> Icons.Default.Terrain
    "SIGNS" -> Icons.Default.Warning
    "SPEED" -> Icons.Default.Speed
    else -> Icons.Default.Category
}

/**
 * Barra de símbolos: solo los símbolos oficiales de simbolos.json y las flechas armadas
 * de flechas_armadas.json (especificación de la viñeta, puntos 5 y 8).
 *
 * - Tocar un símbolo lo inserta en el centro del recuadro que se está editando.
 * - Arrastrarlo con el dedo hacia un costado lo levanta; al soltarlo dentro de un recuadro
 *   de dibujo o de notas queda en ese lugar. Arriba y abajo se desplaza la lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarSymbolsPalette(
    onInsertar: (ItemPaleta) -> Unit,
    onAppendNoteText: (String) -> Unit,
    onCollapse: () -> Unit,
    vinetaNumeroActiva: Int?,
    arrastre: ArrastreSimbolo,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkTheme
    val bg = if (isDark) RallyCardBg else Color(0xFFF8FAFC)
    val borderCol = if (isDark) RallySurface else Color(0xFFCBD5E1)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF64748B)

    val context = LocalContext.current
    val bib: Biblioteca = remember { BibliotecaVineta.obtener(context) }
    val dibujante = remember(bib) { DibujanteVineta(bib) }

    val tabs = remember(bib) {
        listOf(TabPaleta(TAB_FLECHAS, "FLECHAS", Icons.Default.CallMade)) +
            bib.categorias.map { TabPaleta(it.codigo, it.nombre.uppercase(), iconoCategoria(it.codigo)) } +
            TabPaleta(TAB_NOTAS, "NOTAS", Icons.Default.EditNote)
    }

    val armadas: List<ItemPaleta> = remember(bib) {
        bib.flechasArmadas.values.map { ItemPaleta.ArmadaItem(it.codigo, it.nombre, it.flecha) }
    }
    val simbolos: List<Pair<String, ItemPaleta>> = remember(bib) {
        bib.listaSimbolos.map { it.categoria to ItemPaleta.SimboloItem(it.codigo, it.nombre) }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(TAB_FLECHAS) }

    // Abreviaturas de texto para la nota escrita de la viñeta (no son símbolos del dibujo)
    val fiaNoteShortcuts = remember {
        listOf(
            "L" to "Left", "R" to "Right", "kpL" to "Keep Left", "kpR" to "Keep Right",
            "onL" to "On Left", "onR" to "On Right", "kpS" to "Keep Straight", "R/L" to "Right/Left",
            "+V" to "Faster (+V)", "-V" to "Slower (-V)", "IMP" to "Important", "BAD" to "Bad Ground",
            "RGH" to "Rough", "GAR" to "Gravel", "NR" to "Narrow Road", "GV" to "Grave",
            "BTW" to "Between", "RJ" to "Rejoin Road", "FA" to "Fast", "VAL" to "Valley",
            "DN" to "Downhill", "UP" to "Uphill", "CAP" to "Follow CAP", "PP" to "Main Track",
            "VADO" to "Water Cross", "WADI" to "Dry Riverbed", "HP" to "Hors Piste", "NBX" to "Many",
            "CH" to "Time Control", "DZ" to "Danger Zone", "FZ" to "End Zone", "CP" to "Check Point",
            "DSS" to "Start SS", "ASS" to "Finish SS", "MODIF" to "Opening Car Modif", "SA" to "Sandy", "ET" to "And / Next"
        )
    }

    val itemsVisibles: List<ItemPaleta> = remember(searchQuery, selectedTab, bib) {
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            fun coincide(nombre: String, codigo: String) =
                nombre.contains(q, ignoreCase = true) || codigo.contains(q, ignoreCase = true)
            armadas.filter { val a = it as ItemPaleta.ArmadaItem; coincide(a.nombre, a.codigo) } +
                simbolos.map { it.second }.filter { val s = it as ItemPaleta.SimboloItem; coincide(s.nombre, s.codigo) }
        } else when (selectedTab) {
            TAB_FLECHAS -> armadas
            TAB_NOTAS -> emptyList()
            else -> simbolos.filter { it.first == selectedTab }.map { it.second }
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderCol)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. CABECERA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) RallySurface else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = if (isDark) RallyCyan else FredianiCyanText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "SÍMBOLOS", fontWeight = FontWeight.Black, fontSize = 11.sp, color = textPrimary)
                    if (vinetaNumeroActiva != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = FredianiCyan.copy(alpha = 0.2f)) {
                            Text(
                                text = "#$vinetaNumeroActiva",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (isDark) RallyCyan else FredianiCyanText,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onCollapse, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar panel", tint = textSecondary, modifier = Modifier.size(16.dp))
                }
            }

            // 2. BUSCADOR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar símbolo...", fontSize = 10.5.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(12.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
            )

            // 3. PESTAÑAS
            if (searchQuery.isEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = tabs.indexOfFirst { it.clave == selectedTab }.coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    divider = {},
                    edgePadding = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEach { tab ->
                        val sel = selectedTab == tab.clave
                        val col = if (sel) (if (isDark) RallyCyan else FredianiCyanText) else textSecondary
                        Tab(
                            selected = sel,
                            onClick = { selectedTab = tab.clave },
                            icon = { Icon(imageVector = tab.icono, contentDescription = tab.titulo, tint = col, modifier = Modifier.size(19.dp)) },
                            text = {
                                Text(
                                    text = tab.titulo,
                                    fontSize = 10.sp,
                                    fontWeight = if (sel) FontWeight.Black else FontWeight.Bold,
                                    color = col,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }

            // 4. GRILLA
            if (searchQuery.isEmpty() && selectedTab == TAB_NOTAS) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp)) {
                    Text(
                        text = "Toque para insertar texto en la nota:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 64.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(fiaNoteShortcuts) { (abr, desc) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) RallySurface else Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppendNoteText("$abr ") }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = abr, fontWeight = FontWeight.Black, fontSize = 11.sp, color = if (isDark) RallyCyan else FredianiCyanText)
                                    Text(text = desc, fontSize = 7.5.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Toque para insertar · arrastre hacia el costado y suelte en el recuadro",
                    fontSize = 8.5.sp,
                    color = textSecondary,
                    maxLines = 2
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 78.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(itemsVisibles, key = { it.clave }) { item ->
                        CeldaPaleta(
                            item = item,
                            arrastre = arrastre,
                            bib = bib,
                            dibujante = dibujante,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            onTocar = { onInsertar(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CeldaPaleta(
    item: ItemPaleta,
    arrastre: ArrastreSimbolo,
    bib: Biblioteca,
    dibujante: DibujanteVineta,
    borderCol: Color,
    textPrimary: Color,
    onTocar: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val nombre = when (item) {
        is ItemPaleta.SimboloItem -> item.nombre
        is ItemPaleta.ArmadaItem -> item.nombre
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) RallySurface else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderCol),
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .onGloballyPositioned { coords[0] = it }
            .pointerInput(item.clave) {
                // Solo el movimiento hacia los costados levanta el símbolo; arriba/abajo desplaza la lista
                detectHorizontalDragGestures(
                    onDragStart = { pos ->
                        coords[0]?.takeIf { it.isAttached }?.let { arrastre.iniciar(item, it.localToWindow(pos)) }
                    },
                    onDragEnd = { arrastre.soltar() },
                    onDragCancel = { arrastre.cancelar() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        coords[0]?.takeIf { it.isAttached }?.let { arrastre.mover(it.localToWindow(change.position)) }
                    }
                )
            }
            .clickable { onTocar() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                MuestraItemPaleta(item, bib, dibujante, Modifier.fillMaxSize().padding(2.dp))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = nombre,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Dibujo de muestra de un símbolo (viewBox 100) o de una flecha armada, sin WebView. */
@Composable
fun MuestraItemPaleta(item: ItemPaleta, bib: Biblioteca, dibujante: DibujanteVineta, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val lado = minOf(size.width, size.height)
        val left = (size.width - lado) / 2f
        val top = (size.height - lado) / 2f
        drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            when (item) {
                is ItemPaleta.SimboloItem -> bib.simbolo(item.codigo)?.let { sim ->
                    nc.save()
                    nc.translate(left, top)
                    nc.scale(lado / 100f, lado / 100f)
                    DibujanteSvg.dibujar(nc, sim.ops)
                    nc.restore()
                }
                is ItemPaleta.ArmadaItem ->
                    dibujante.dibujarCapa(nc, Capa(arrows = listOf(item.flecha)), left, top, lado, ALTO_DIBUJO, conPalito = true)
            }
        }
    }
}
