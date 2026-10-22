package com.example.roadbookorganizador.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.UsuarioSesion
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.ui.components.CabeceraPlataforma
import com.example.roadbookorganizador.ui.components.ColoresPlataforma
import com.example.roadbookorganizador.ui.theme.ThemeManager
import com.example.roadbookorganizador.ui.viewmodel.MenuPrincipalViewModel

/**
 * "MIS RALLIES": el panel de rallies de la plataforma (App.tsx, nivel 1).
 * Cabecera, cartel "Panel de Rallies & Campeonatos" con los números, buscador, filtro por estado
 * y una tarjeta por rally con "INGRESAR AL RALLY".
 *
 * Los rallies vienen del servidor (`GET /api/rallies`); se bajan al entrar y con "SINCRONIZAR".
 * En la tablet no se crean ni se editan rallies: se hacen en la plataforma (acordado el 21/10/2026).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeleccionRallyScreen(
    viewModel: MenuPrincipalViewModel,
    sesion: UsuarioSesion,
    onIngresarAlRally: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val rallies by viewModel.rallies.collectAsState()
    val activeRally by viewModel.activeRally.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val c = ColoresPlataforma
    val isDark = ThemeManager.isDarkTheme
    // En el celular: todo en una columna y un poco más chico
    val chico = com.example.roadbookorganizador.ui.components.tamanoPantalla() == com.example.roadbookorganizador.ui.components.TamanoPantalla.CHICO

    var busqueda by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableStateOf("TODOS") }

    LaunchedEffect(Unit) {
        viewModel.sincronizarAlEntrar()
        viewModel.syncMessage.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    val filtrados = rallies.filter { r ->
        val q = busqueda.trim()
        (q.isEmpty() || r.nombre.contains(q, true) || r.sede.contains(q, true)) &&
            (filtroEstado == "TODOS" || r.estadoRally.equals(filtroEstado, true))
    }

    Column(modifier = Modifier.fillMaxSize().background(c.fondo())) {
        CabeceraPlataforma(
            sesion = sesion,
            subtitulo = "Panel de Control • Mis Rallies Habilitados",
            onPreferencias = onNavigateToAjustes,
            onLogout = onLogout
        ) {
            Text(
                "● ${rallies.size} RALLIES HABILITADOS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = c.verde,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.verde.copy(alpha = 0.2f))
                    .border(1.dp, c.verde.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 380.dp),
            contentPadding = PaddingValues(if (chico) 12.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(if (chico) 14.dp else 24.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding()
        ) {
            // Cartel principal y números
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.tarjeta())
                        .border(1.dp, c.borde(), RoundedCornerShape(24.dp))
                        .padding(if (chico) 18.dp else 28.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.widthIn(min = 260.dp).weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "ECOSISTEMA FREDIANI ROADBOOK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = c.celeste,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(c.celeste.copy(alpha = 0.2f))
                                        .border(1.dp, c.celeste.copy(alpha = 0.4f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Panel de Rallies & Campeonatos", fontSize = if (chico) 24.sp else 32.sp, fontWeight = FontWeight.Black, color = c.texto())
                            Text(
                                "Accedé a cada competencia habilitada para relevar sus tramos en el terreno con la tablet.",
                                fontSize = 14.sp, color = c.apagado()
                            )
                        }
                        // En la tablet, en lugar de "Nuevo rally": bajar los rallies de la plataforma
                        Button(
                            onClick = { viewModel.sincronizarConWeb() },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.celeste, contentColor = c.fondoOscuro),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            if (isSyncing) CircularProgressIndicator(color = c.fondoOscuro, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            else Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isSyncing) "SINCRONIZANDO" else "SINCRONIZAR", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
                        }
                    }
                }
            }

            // Buscador y filtros
            item(span = { GridItemSpan(maxLineSpan) }) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = busqueda, onValueChange = { busqueda = it },
                        placeholder = { Text("Buscar rally por nombre, ciudad o provincia...", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true, shape = RoundedCornerShape(16.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.texto()),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = c.tarjeta(), unfocusedContainerColor = c.tarjeta(),
                            focusedBorderColor = c.celeste, unfocusedBorderColor = c.borde()
                        ),
                        modifier = if (chico) Modifier.fillMaxWidth() else Modifier.widthIn(min = 260.dp, max = 448.dp).weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.CenterVertically).horizontalScroll(rememberScrollState())
                    ) {
                        listOf("TODOS", "EN CURSO", "PROXIMO").forEach { st ->
                            val sel = filtroEstado == st
                            Text(
                                st, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Bold,
                                color = if (sel) (if (isDark) Color.Black else Color.White) else c.apagado(),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) (if (isDark) c.amarillo else Color(0xFFF59E0B)) else c.tarjeta())
                                    .border(1.dp, if (sel) Color.Transparent else c.borde(), RoundedCornerShape(12.dp))
                                    .clickable { filtroEstado = st }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            when {
                rallies.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(c.tarjeta())
                            .border(1.dp, c.borde(), RoundedCornerShape(24.dp))
                            .padding(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(c.celeste.copy(alpha = 0.1f))
                                .border(1.dp, c.celeste.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        ) { Icon(Icons.Default.Flag, contentDescription = null, tint = c.celeste, modifier = Modifier.size(40.dp)) }
                        Spacer(Modifier.height(20.dp))
                        Text("No hay rallies en la tablet todavía", fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.texto())
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tocá SINCRONIZAR para bajar los rallies que tenés en la plataforma.",
                            fontSize = 12.sp, color = c.apagado(), textAlign = TextAlign.Center
                        )
                    }
                }
                filtrados.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.tarjeta())
                            .border(1.dp, c.borde(), RoundedCornerShape(24.dp)).padding(40.dp)
                    ) {
                        Text("No se encontraron rallies con los filtros aplicados.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.apagado())
                        TextButton(onClick = { busqueda = ""; filtroEstado = "TODOS" }) {
                            Text("Limpiar filtros de búsqueda", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.celeste)
                        }
                    }
                }
                else -> items(filtrados, key = { it.id }) { r ->
                    TarjetaRally(
                        rally = r,
                        esActivo = r.id == activeRally?.id,
                        viewModel = viewModel,
                        onIngresar = {
                            viewModel.seleccionarRallyActivo(r.id)
                            onIngresarAlRally()
                        }
                    )
                }
            }
        }
    }

}

/** Tarjeta de un rally, como la de la plataforma (sin foto: el degradé de cuando no hay portada). */
@Composable
private fun TarjetaRally(
    rally: RallyEntity,
    esActivo: Boolean,
    viewModel: MenuPrincipalViewModel,
    onIngresar: () -> Unit
) {
    val c = ColoresPlataforma
    val tramos by remember(rally.id) { viewModel.tramosDe(rally.id) }.collectAsState(initial = emptyList())
    val km = tramos.sumOf { it.distanciaTotalEstimada }
    val enCurso = rally.estadoRally.equals("EN CURSO", true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.tarjeta())
            .border(1.dp, if (esActivo) c.celeste.copy(alpha = 0.6f) else c.borde(), RoundedCornerShape(24.dp))
    ) {
        // Portada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF0B3A5C), Color(0xFF0A0E17), Color(0x660284C7))))
        ) {
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Color(0xFF0A0E17)))))
            Text(
                rally.estadoRally.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = c.fondoOscuro, letterSpacing = 0.6.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background((if (enCurso) c.verde else c.celeste).copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rally.sede.isNotBlank()) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = c.celeste, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(rally.sede, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                } else Spacer(Modifier.weight(1f))
                if (rally.fecha.isNotBlank()) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = c.amarillo, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (rally.fechaFin.isNotBlank()) "${rally.fecha} al ${rally.fechaFin}" else rally.fecha,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (rally.campeonato.ifBlank { rally.fiscalizador }.ifBlank { "—" }).uppercase(),
                    fontSize = 10.sp, fontWeight = FontWeight.Black, color = c.celeste, letterSpacing = 0.6.sp
                )
                Text("  •  ", fontSize = 10.sp, color = Color(0xFF64748B))
                Text(rally.organizadorClub.ifBlank { "Sin organizador cargado" }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.apagado(), maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            Text(rally.nombre, fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.texto(), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (rally.descripcion.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(rally.descripcion, fontSize = 12.sp, color = c.apagado(), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0x4D334155))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                DatoTarjeta("${tramos.size}", "Tramos", c.celeste, Modifier.weight(1f))
                DatoTarjeta(String.format(java.util.Locale.US, "%.1f", km), "Km totales", c.verde, Modifier.weight(1f))
            }
            HorizontalDivider(color = Color(0x4D334155))
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onIngresar,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.celeste, contentColor = c.fondoOscuro),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("INGRESAR AL RALLY", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DatoTarjeta(valor: String, titulo: String, color: Color, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(valor, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = color)
        Text(titulo.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
    }
}
