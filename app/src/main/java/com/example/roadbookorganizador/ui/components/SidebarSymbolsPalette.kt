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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.example.roadbookorganizador.vineta.ALTO_DIBUJO_V1
import com.example.roadbookorganizador.vineta.Biblioteca
import com.example.roadbookorganizador.vineta.Capa
import com.example.roadbookorganizador.vineta.Colocar

/** Pestañas de la paleta, en el orden y con los nombres de la plataforma (RoadbookWorkstation). */
private data class TabPaleta(val clave: String, val titulo: String, val icono: ImageVector, val categoria: String? = null)

private val TABS = listOf(
    TabPaleta("flechas", "Flechas", Icons.Default.CallMade),
    TabPaleta("landmarks", "Referencias", Icons.Default.Place, "LANDMARKS"),
    TabPaleta("road_rally", "Caminos", Icons.Default.CallSplit, "ROAD_RALLY"),
    TabPaleta("terrain", "Terreno", Icons.Default.Terrain, "TERRAIN"),
    TabPaleta("signs", "Señales", Icons.Default.Warning, "SIGNS"),
    TabPaleta("speed", "Velocidad", Icons.Default.Speed, "SPEED"),
    TabPaleta("formas", "Formas", Icons.Default.Category, "SHAPES"),
    TabPaleta("note", "Notas", Icons.Default.TextFields)
)

/** Acrónimos FIA de la plataforma (código, nombre, en castellano). */
private val ACRONIMOS = listOf(
    Triple("L", "Left", "Izquierda"), Triple("L/R", "Left then Right", "Izq luego Der"), Triple("kpL", "Keep Left", "Mantener Izq"),
    Triple("onL", "On Left", "A la Izq"), Triple("kpS", "Keep Straight", "Mantener Recto"), Triple("onR", "On Right", "A la Der"),
    Triple("kpR", "Keep Right", "Mantener Der"), Triple("R/L", "Right then Left", "Der luego Izq"), Triple("R", "Right", "Derecha"),
    Triple("P", "Piste", "Pista / Huella"), Triple("MP", "Main Piste", "Pista Principal"), Triple("RO", "Road", "Camino / Ruta"),
    Triple("P//", "Parallel Piste", "Pistas Paralelas"), Triple("OP", "Off-Piste", "Fuera de Pista"), Triple("FMP", "Follow Main Piste", "Seguir Pista Princ."),
    Triple("FRO", "Follow Road", "Seguir Camino"), Triple("±V", "Visible", "Más / Menos Vis."), Triple("+V", "More Visible", "Huella Más Visible"),
    Triple("-V", "Less Visible", "Huella Menos Vis."), Triple("N", "Narrow", "Estrecho / Angosto"), Triple("AT", "At", "A la Altura De"),
    Triple("IN", "In / Into", "Dentro / En"), Triple("B^G", "Bridge", "Puente"), Triple("S^M", "Summit", "Cresta / Cumbre"),
    Triple("A^W", "After Wadi", "Después del Río"), Triple("M^X", "Max", "Máximo"), Triple("SR^X", "Bumpy", "Muy Roto / Salto"),
    Triple("SR", "Rough", "Roto / Ondulado"), Triple("QT", "Quit", "Dejar / Abandonar"), Triple("Q^MP", "Quit Main Piste", "Dejar Pista Princ."),
    Triple("IMP", "Imperative", "Imperativo / Oblig."), Triple("CLP", "Clay / Mud", "Barro / Greda"), Triple("BAD", "Bad Road", "Camino Malo"),
    Triple("RGH", "Rough", "Muy Roto / Piedras"), Triple("GAR", "Gate", "Tranquera / Portón"), Triple("NR", "Narrow", "Paso Angosto"),
    Triple("GV", "Gravel", "Ripio / Grava"), Triple("GT", "Cattle Grid", "Guardaganado"), Triple("+", "Cross", "Cruce en Cruz"),
    Triple("BTW", "Between", "Entre"), Triple("RJ", "Rejoin", "Empalme / Retomar"), Triple("FA", "Fast", "Rápido"),
    Triple("VAL", "Valley", "Valle / Quebrada"), Triple("DN", "Dune", "Duna / Bajada"), Triple("DN^X", "Broken Dune", "Duna Cortada"),
    Triple("SA", "Sand", "Arena Suelta"), Triple("ST", "Straight", "Recto"), Triple("VG", "Vegetation", "Vegetación / Pasto")
)

/**
 * PALETA DE SÍMBOLOS Y ACRÓNIMOS, igual que la de la plataforma: buscador, pestañas
 * (Flechas, Referencias, Caminos, Terreno, Señales, Velocidad, Formas, Notas) y una grilla de
 * 3 columnas con cada dibujo ocupando toda su casilla.
 *
 * - Tocar un símbolo lo pone en la viñeta elegida.
 * - Arrastrarlo con el dedo hacia un costado lo levanta; al soltarlo dentro de un recuadro de
 *   dibujo o de notas queda en ese lugar. Arriba y abajo se desplaza la lista.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val fondo = if (isDark) RallyCardBg else Color.White
    val borde = if (isDark) Color(0xFF2A374D) else Color(0xFFCBD5E1)
    val suave = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val context = LocalContext.current
    val bib: Biblioteca = remember { BibliotecaVineta.obtener(context) }
    val dibujante = remember(bib) { DibujanteVineta(bib) }

    var busqueda by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("flechas") }
    val q = busqueda.trim()
    fun coincide(vararg textos: String) = q.isEmpty() || textos.any { it.contains(q, ignoreCase = true) }

    val armadas = remember(bib) { bib.flechasArmadas.values.map { ItemPaleta.ArmadaItem(it.codigo, it.nombre, it.flecha) } }

    Column(
        modifier = modifier
            .background(fondo)
            .border(1.dp, borde)
    ) {
        // Buscador + pestañas
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar símbolo o acrónimo...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f).height(46.dp)
                )
                IconButton(onClick = onCollapse, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar paleta", tint = suave, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            // Pestañas: botones chicos que se acomodan en varias filas, como en la web
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TABS.forEach { t ->
                    val sel = tab == t.clave
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) Color(0xFF00B4D8) else (if (isDark) RallySurface else Color(0xFFE2E8F0)))
                            .clickable { tab = t.clave }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(t.icono, contentDescription = null, tint = if (sel) Color.Black else suave, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t.titulo.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black,
                            color = if (sel) Color.Black else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)))
                    }
                }
            }
        }
        HorizontalDivider(color = borde)

        val actual = TABS.first { it.clave == tab }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            fun titulo(texto: String, color: Color = suave) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(texto.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = color,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            when {
                actual.clave == "flechas" -> {
                    titulo("Flechas armadas (tocala para ponerla en la viñeta elegida, o arrastrala a cualquier viñeta):")
                    // Una sola lista: primero las flechas solas, después con 1 y con 2 caminos prohibidos
                    for (n in 0..2) {
                        val lista = armadas.filter { Colocar.prohibidosDelCodigo(it.codigo) == n && coincide(it.nombre, it.codigo) }
                        if (lista.isEmpty()) continue
                        if (n > 0) titulo(if (n == 1) "Con un camino prohibido" else "Con dos caminos prohibidos", Color(0xFF64748B))
                        items(lista, key = { it.clave }) { item ->
                            CeldaPaleta(item, arrastre, bib, dibujante, borde, isDark) { onInsertar(item) }
                        }
                    }
                }
                actual.clave == "note" -> {
                    val lista = ACRONIMOS.filter { (c, l, d) -> coincide(c, l, d) }
                    titulo("Acrónimos (tocá para insertar):")
                    items(lista, key = { "A:" + it.first }) { (codigo, nombre, desc) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) RallyCardBg else Color.White)
                                .border(1.dp, borde, RoundedCornerShape(16.dp))
                                .clickable { onAppendNoteText("$codigo ") }
                                .padding(6.dp)
                        ) {
                            Text(codigo, fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (isDark) Color.White else Color(0xFF0F172A))
                            Text(nombre, fontSize = 8.5.sp, color = suave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(desc, fontSize = 8.sp, color = suave, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                }
                else -> {
                    val lista = bib.listaSimbolos
                        .filter { (q.isNotEmpty() || it.categoria == actual.categoria) && coincide(it.nombre, it.codigo) }
                        .map { ItemPaleta.SimboloItem(it.codigo, it.nombre) }
                    titulo("${actual.titulo} (${lista.size}):")
                    items(lista, key = { it.clave }) { item ->
                        CeldaPaleta(item, arrastre, bib, dibujante, borde, isDark) { onInsertar(item) }
                    }
                }
            }
        }
    }
}

/** Casilla cuadrada de la paleta: el dibujo ocupa toda la casilla (sin recuadro adentro). */
@Composable
private fun CeldaPaleta(
    item: ItemPaleta,
    arrastre: ArrastreSimbolo,
    bib: Biblioteca,
    dibujante: DibujanteVineta,
    borde: Color,
    isDark: Boolean,
    onTocar: () -> Unit
) {
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, borde, RoundedCornerShape(16.dp))
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
            .padding(6.dp)
    ) {
        MuestraItemPaleta(item, bib, dibujante, Modifier.fillMaxSize())
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
                    dibujante.dibujarCapa(nc, Capa(arrows = listOf(item.flecha)), left, top, lado, ALTO_DIBUJO_V1, conPalito = false) // las armadas están hechas en un cuadrado
            }
        }
    }
}
