package com.example.roadbookorganizador.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.roadbookorganizador.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.UsuarioSesion
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.components.CabeceraPlataforma
import com.example.roadbookorganizador.ui.components.PestanaRally
import com.example.roadbookorganizador.ui.components.PestanasRally
import com.example.roadbookorganizador.ui.components.SelectorRallyPlataforma
import com.example.roadbookorganizador.ui.viewmodel.MenuPrincipalViewModel

/**
 * PANEL GENERAL del rally, como la pestaña de la plataforma (App.tsx, tabRally "panel"):
 * portada con el estado, el nombre y la sede; la reseña; y las zonas y puntos de interés
 * (las tres primeras, con "Ver todas"). La cabecera y las pestañas las pone MarcoRally.
 *
 * Lo que la plataforma muestra y la tablet no tiene (inscriptos, recuadro offline, sponsors)
 * no se inventa: se ve en la web.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanelGeneralRally(
    viewModel: MenuPrincipalViewModel,
    onVerZonas: () -> Unit
) {
    val context = LocalContext.current
    val rally by viewModel.activeRally.collectAsState()
    val puntos by viewModel.puntosRallyActivo.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val c = com.example.roadbookorganizador.ui.components.ColoresPlataforma
    val chico = com.example.roadbookorganizador.ui.components.tamanoPantalla() == com.example.roadbookorganizador.ui.components.TamanoPantalla.CHICO

    LaunchedEffect(Unit) {
        viewModel.syncMessage.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(if (chico) 12.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (chico) 14.dp else 24.dp)
    ) {
        val r = rally
        if (r == null) {
            Text("Elegí un rally en \"Mis Rallies\".", color = c.apagado(), fontSize = 14.sp)
            return@Column
        }

        // PORTADA (sin foto: el degradé de la plataforma cuando no hay portada)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(c.tarjeta())
                .border(1.dp, c.borde(), RoundedCornerShape(24.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (chico) 200.dp else 280.dp)
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0B3A5C), Color(0xFF0A0E17), Color(0x660284C7))))
            ) {
                Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x80000000), Color.Transparent, Color(0xFF0A0E17)))))
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        r.estadoRally.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(c.verde.copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    // En la tablet el rally no se edita: en su lugar, bajar lo último de la plataforma
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSyncing) { viewModel.sincronizarConWeb() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        if (isSyncing) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        else Icon(Icons.Default.Sync, contentDescription = "Sincronizar", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isSyncing) "SINCRONIZANDO" else "SINCRONIZAR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    val linea = listOf(r.campeonato.ifBlank { r.fiscalizador }, r.sede).filter { it.isNotBlank() }.joinToString("  •  ")
                    if (linea.isNotBlank()) Text(linea, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.celeste)
                    Text(r.nombre, fontSize = if (chico) 24.sp else 36.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = if (chico) 28.sp else 40.sp)
                    val fechas = if (r.fechaFin.isNotBlank()) "${r.fecha} al ${r.fechaFin}" else r.fecha
                    if (fechas.isNotBlank()) Text(fechas, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                }
            }
            if (r.descripcion.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.superficie())
                        .border(1.dp, c.borde(), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("RESEÑA OFICIAL DEL EVENTO:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = c.celeste, letterSpacing = 0.6.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(r.descripcion, fontSize = 12.sp, color = c.apagado(), lineHeight = 18.sp)
                }
            }
        }

        // ZONAS Y PUNTOS DE INTERÉS (acceso rápido)
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
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ZONAS Y PUNTOS DE INTERÉS", fontSize = if (chico) 14.sp else 18.sp, fontWeight = FontWeight.Black, color = c.texto())
                    }
                    Text("Áreas seguras autorizadas para el público y zonas de exclusión", fontSize = 12.sp, color = c.apagado())
                }
                TextButton(onClick = onVerZonas) {
                    Text("Ver todas (${puntos.size}) →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.celeste)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x66334155))
            Spacer(Modifier.height(16.dp))
            if (puntos.isEmpty()) {
                Text(
                    "No hay zonas de espectadores cargadas aún para este rally.",
                    fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    puntos.take(3).forEach { p ->
                        val (fondo, tinta) = when (p.tipo.uppercase()) {
                            "PUBLICO", "PERMITIDA" -> Color(0x3306D6A0) to Color(0xFF06D6A0)
                            "PELIGRO", "PROHIBIDA" -> Color(0x33F43F5E) to Color(0xFFFB7185)
                            else -> Color(0x330EA5E9) to Color(0xFF38BDF8)
                        }
                        Column(
                            modifier = Modifier
                                .widthIn(min = 220.dp, max = 360.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.superficie())
                                .border(1.dp, c.borde(), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                p.tipo.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = tinta,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(fondo).padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(p.nombre, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.texto())
                            if (p.descripcion.isNotBlank()) Text(p.descripcion, fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EstadisticaItem(titulo: String, valor: String) {
    val isDark = ThemeManager.isDarkTheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = titulo, fontSize = 10.5.sp, color = if (isDark) OdometerLabel else Color(0xFF64748B))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = valor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )
    }
}

@Composable
fun BotonModuloMenu(
    titulo: String,
    subtitulo: String,
    badge: String? = null,
    icono: ImageVector,
    colorIcono: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    Card(
        modifier = modifier
            .height(138.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) RallyCardBg else Color.White),
        border = BorderStroke(1.dp, if (isDark) RallySurface else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorIcono.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(24.dp))
                    }
                }

                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = subtitulo,
                    fontSize = 10.5.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DialogoEditarRally(
    rally: RallyEntity,
    onDismiss: () -> Unit,
    onGuardar: (String, String, String, String, String, String, String, String, Double, String) -> Unit
) {
    var nombre by remember { mutableStateOf(rally.nombre) }
    var campeonato by remember { mutableStateOf(rally.campeonato) }
    var club by remember { mutableStateOf(rally.organizadorClub) }
    var sede by remember { mutableStateOf(rally.sede) }
    var fechaInicio by remember { mutableStateOf(rally.fecha) }
    var fechaFin by remember { mutableStateOf(rally.fechaFin) }
    var fiscalizador by remember { mutableStateOf(rally.fiscalizador) }
    var estado by remember { mutableStateOf(rally.estadoRally) }
    var areaKm2 by remember { mutableStateOf(rally.areaKm2.toString()) }
    var descripcion by remember { mutableStateOf(rally.descripcion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (ThemeManager.isDarkTheme) RallyCardBg else Color.White,
        title = {
            Text(
                "Ficha Oficial del Rally",
                fontWeight = FontWeight.Black,
                color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre Oficial del Rally") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = campeonato,
                    onValueChange = { campeonato = it },
                    label = { Text("Campeonato") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sede,
                    onValueChange = { sede = it },
                    label = { Text("Sede / Localidad") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fechaInicio,
                        onValueChange = { fechaInicio = it },
                        label = { Text("Fecha Inicio") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fechaFin,
                        onValueChange = { fechaFin = it },
                        label = { Text("Fecha Fin") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = club,
                        onValueChange = { club = it },
                        label = { Text("Club Organizador") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fiscalizador,
                        onValueChange = { fiscalizador = it },
                        label = { Text("Fiscalizador") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción / Notas") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val area = areaKm2.toDoubleOrNull() ?: 350.0
                    onGuardar(nombre, campeonato, club, sede, fechaInicio, fechaFin, fiscalizador, estado, area, descripcion)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen)
            ) {
                Text("GUARDAR", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
