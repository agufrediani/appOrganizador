package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.ui.theme.*

enum class SidebarTab(val titulo: String, val iconName: String) {
    CROSS_COUNTRY("TULIPAS", "Directions"),
    SIGNS("SIGNOS", "Warning"),
    LANDMARKS("LANDMARKS", "LocationOn"),
    TERRAIN("TERRENO", "Terrain"),
    NOTES_FIA("NOTAS FIA", "EditNote")
}

data class SymbolItem(
    val code: String,
    val label: String,
    val category: SidebarTab,
    val isArrow: Boolean = false,
    val isNoteText: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarSymbolsPalette(
    onInsertStamp: (String) -> Unit,
    onAppendNoteText: (String) -> Unit,
    onCollapse: () -> Unit,
    vinetaNumeroActiva: Int?,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkTheme
    val bg = if (isDark) RallyCardBg else Color(0xFFF8FAFC)
    val borderCol = if (isDark) RallySurface else Color(0xFFCBD5E1)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF64748B)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(SidebarTab.CROSS_COUNTRY) }

    // Catálogo completo idéntico a Rally Navigator 2.0 (Maniobras, Signos, Landmarks, Terreno, y Lexicon FIA)
    val allSymbols = remember {
        listOf(
            // --- CROSS COUNTRY / TULIPAS ---
            SymbolItem("FLECHA_MANIOBRA", "CURVA FLEX", SidebarTab.CROSS_COUNTRY, isArrow = true),
            SymbolItem("RECTA", "RECTA", SidebarTab.CROSS_COUNTRY, isArrow = true),
            SymbolItem("CURVA_DER", "CURVA DER", SidebarTab.CROSS_COUNTRY),
            SymbolItem("CURVA_IZQ", "CURVA IZQ", SidebarTab.CROSS_COUNTRY),
            SymbolItem("DERECHA_90", "90° DER", SidebarTab.CROSS_COUNTRY),
            SymbolItem("IZQUIERDA_90", "90° IZQ", SidebarTab.CROSS_COUNTRY),
            SymbolItem("RETOME_DER", "RETOME DER", SidebarTab.CROSS_COUNTRY),
            SymbolItem("RETOME_IZQ", "RETOME IZQ", SidebarTab.CROSS_COUNTRY),
            SymbolItem("CRUCE_X", "CRUCE +", SidebarTab.CROSS_COUNTRY),
            SymbolItem("CRUCE_T", "EMPALME T", SidebarTab.CROSS_COUNTRY),
            SymbolItem("BIFURCACION_Y", "DESVÍO Y", SidebarTab.CROSS_COUNTRY),
            SymbolItem("ROTONDA", "ROTONDA", SidebarTab.CROSS_COUNTRY),

            // --- SIGNOS / PELIGROS ---
            SymbolItem("PELIGRO_1", "! ATENCIÓN", SidebarTab.SIGNS),
            SymbolItem("PELIGRO_2", "!! PELIGRO", SidebarTab.SIGNS),
            SymbolItem("PELIGRO_3", "!!! GRAVE", SidebarTab.SIGNS),
            SymbolItem("PRECAUCION", "PRECAUCIÓN", SidebarTab.SIGNS),
            SymbolItem("STOP", "STOP", SidebarTab.SIGNS),
            SymbolItem("RADAR_DZ", "DZ RADAR", SidebarTab.SIGNS),
            SymbolItem("RADAR_FZ", "FZ FIN", SidebarTab.SIGNS),
            SymbolItem("RESET_0000", "0000 RESET", SidebarTab.SIGNS),
            SymbolItem("GPS_POINT", "WPV GPS", SidebarTab.SIGNS),
            SymbolItem("TC", "TC CONTROL", SidebarTab.SIGNS),
            SymbolItem("LARGADA", "LARGADA", SidebarTab.SIGNS),
            SymbolItem("LLEGADA", "LLEGADA", SidebarTab.SIGNS),
            SymbolItem("SURTIDOR", "COMBUST.", SidebarTab.SIGNS),
            SymbolItem("ASISTENCIA", "ASISTENCIA", SidebarTab.SIGNS),
            SymbolItem("MEDICO", "MÉDICO", SidebarTab.SIGNS),

            // --- LANDMARKS ---
            SymbolItem("TRANQUERA", "TRANQUERA", SidebarTab.LANDMARKS),
            SymbolItem("GUARDAGANADO", "GUARDAG.", SidebarTab.LANDMARKS),
            SymbolItem("PUENTE", "PUENTE", SidebarTab.LANDMARKS),
            SymbolItem("ALCANTARILLA", "ALCANT.", SidebarTab.LANDMARKS),
            SymbolItem("VIAS_TREN", "VÍAS TREN", SidebarTab.LANDMARKS),
            SymbolItem("ANTENA", "ANTENA", SidebarTab.LANDMARKS),
            SymbolItem("MOLINO", "MOLINO", SidebarTab.LANDMARKS),
            SymbolItem("CASA", "POBLADO", SidebarTab.LANDMARKS),
            SymbolItem("IGLESIA", "IGLESIA", SidebarTab.LANDMARKS),
            SymbolItem("CIRCULO", "MOJÓN", SidebarTab.LANDMARKS),

            // --- TERRENO ---
            SymbolItem("VADO", "VADO", SidebarTab.TERRAIN),
            SymbolItem("SALTO", "SALTO", SidebarTab.TERRAIN),
            SymbolItem("ZANJA", "ZANJA", SidebarTab.TERRAIN),
            SymbolItem("DUNAS", "DUNAS", SidebarTab.TERRAIN),
            SymbolItem("PIEDRAS", "PIEDRAS", SidebarTab.TERRAIN),
            SymbolItem("BARRO", "HUELLAS", SidebarTab.TERRAIN)
        )
    }

    // Abreviaturas oficiales de Notas estilo Rally Navigator (Note box)
    val fiaNoteShortcuts = remember {
        listOf(
            "L" to "Left", "R" to "Right", "kpL" to "Keep Left", "kpR" to "Keep Right",
            "onL" to "On Left", "onR" to "On Right", "kpS" to "Keep Straight", "R/L" to "Right/Left",
            "+V" to "Faster (+V)", "-V" to "Slower (-V)", "IMP" to "Important", "BAD" to "Bad Ground",
            "RGH" to "Rough", "GAR" to "Gravel", "NR" to "Narrow Road", "GV" to "Grave",
            "BTW" to "Between", "RJ" to "Rejoin Road", "FA" to "Fast", "VAL" to "Valley",
            "DN" to "Downhill", "UP" to "Uphill", "CAP" to "Follow CAP", "PP" to "Main Track",
            "VADO" to "Water Cross", "CH" to "Time Control", "DZ" to "Danger Zone", "FZ" to "End Zone"
        )
    }

    val filteredSymbols = remember(searchQuery, selectedTab) {
        if (searchQuery.isNotBlank()) {
            allSymbols.filter {
                it.label.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true)
            }
        } else {
            allSymbols.filter { it.category == selectedTab }
        }
    }

    Card(
        modifier = modifier.fillMaxHeight(),
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
            // 1. CABECERA: TÍTULO + INDICACIÓN ACTIVA + BOTÓN COLAPSAR (▶)
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
                    Text(
                        text = "SÍMBOLOS",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = textPrimary
                    )
                    if (vinetaNumeroActiva != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FredianiCyan.copy(alpha = 0.2f)
                        ) {
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

                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Minimizar panel",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. BUSCADOR RÁPIDO (RALLY NAVIGATOR STYLE)
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

            // 3. TABS VERTICALES / HORIZONTALES COMPACTOS
            if (searchQuery.isEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = SidebarTab.values().indexOf(selectedTab),
                    containerColor = Color.Transparent,
                    divider = {},
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SidebarTab.values().forEach { tab ->
                        val sel = selectedTab == tab
                        Tab(
                            selected = sel,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.titulo,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (sel) FontWeight.Black else FontWeight.Bold,
                                    color = if (sel) (if (isDark) RallyCyan else FredianiCyanText) else textSecondary
                                )
                            },
                            modifier = Modifier.height(34.dp)
                        )
                    }
                }
            }

            // 4. GRILLA DE SÍMBOLOS O LISTA DE NOTAS FIA
            if (searchQuery.isEmpty() && selectedTab == SidebarTab.NOTES_FIA) {
                // SECCIÓN ESPECIAL: ABREVIATURAS Y TEXTOS FIA RALLY NAVIGATOR
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = "Toque para insertar texto en la nota:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
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
                                    Text(
                                        text = abr,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = if (isDark) RallyCyan else FredianiCyanText
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 7.5.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // GRILLA REGULAR DE SÍMBOLOS GRÁFICOS
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSymbols, key = { it.code }) { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) RallySurface else Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { onInsertStamp(item.code) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                                        DiagramaCanvasRenderer.renderStampPreview(
                                            drawScope = this,
                                            type = item.code,
                                            primaryColor = Color.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
