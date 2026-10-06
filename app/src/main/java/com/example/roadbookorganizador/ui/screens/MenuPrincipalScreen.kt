package com.example.roadbookorganizador.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.roadbookorganizador.ui.viewmodel.MenuPrincipalViewModel

/**
 * PANTALLA DASHBOARD DE OPERACIONES DEL RALLY ACTIVO:
 * Muestra el rally actual chiquito arriba, el botón cuadrado de sincronismo (ida y vuelta)
 * y las ventanas/módulos de trabajo (Tramos, Itinerario, Mapa Libre, Calibración, Ajustes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuPrincipalScreen(
    viewModel: MenuPrincipalViewModel,
    sesion: UsuarioSesion,
    onCambiarRally: () -> Unit,
    onNavigateToTramos: () -> Unit,
    onNavigateToMapaLibre: () -> Unit,
    onNavigateToCalibracion: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activeRally by viewModel.activeRally.collectAsState()
    val estadisticas by viewModel.estadisticas.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var mostrarModalEditarRally by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.syncMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCambiarRally) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cambiar Rally",
                            tint = textPrimary
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_frediani_roadbook),
                            contentDescription = "Frediani Roadbook Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FREDIANI ROADBOOK",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${sesion.nombre.ifBlank { "Organizador" }} • ${sesion.club.ifBlank { "Frediani Competición" }}",
                                fontSize = 11.sp,
                                color = if (isDark) RallyCyanLight else FredianiCyanText
                            )
                        }
                    }
                },
                actions = {
                    // Toggle Tema Claro / Oscuro
                    IconButton(onClick = { ThemeManager.toggleTheme() }) {
                        Text(text = if (isDark) "☀️" else "🌙", fontSize = 18.sp)
                    }
                    // Engranaje de Ajustes
                    IconButton(onClick = onNavigateToAjustes) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    // Cerrar sesión
                    IconButton(onClick = onLogout) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =========================================================================
            // 1. IDENTIFICADOR DEL RALLY ARRIBA CHIQUITO (EXACTO COMO PIDIÓ EL USUARIO)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) RallySurface else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) FredianiGreen.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FredianiGreen.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "RALLY ACTUAL",
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = FredianiGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeRally?.nombre ?: "Sin Rally Asignado",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeRally?.sede ?: "Sede no definida"} • ${activeRally?.fecha ?: "Fecha"}",
                                fontSize = 10.5.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { mostrarModalEditarRally = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(15.dp), tint = textSecondary)
                        }

                        OutlinedButton(
                            onClick = onCambiarRally,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, if (isDark) RallyCyan.copy(alpha = 0.5f) else FredianiCyan)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (isDark) RallyCyan else FredianiCyanText)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Cambiar", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (isDark) RallyCyan else FredianiCyanText)
                        }
                    }
                }
            }

            // =========================================================================
            // 2. MÓDULOS DE OPERACIÓN EN CUADRÍCULA (INCLUYE EL BOTÓN CUADRADO DE SYNC)
            // =========================================================================
            Text(
                text = "PANEL DE CONTROL & MÓDULOS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = if (isDark) RallyCyan else FredianiCyanText,
                letterSpacing = 1.sp
            )

            // FILA 1: BOTÓN CUADRADO DE SINCRONISMO (IDA Y VUELTA) + TRAMOS Y TRAZADO
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // BOTÓN CUADRADO DE SINCRONISMO
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(138.dp)
                        .clickable(enabled = !isSyncing) { viewModel.sincronizarConWeb() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0F2236) else Color(0xFFF0F9FF)
                    ),
                    border = BorderStroke(1.5.dp, if (isSyncing) FredianiGreen else (if (isDark) RallyCyan else Color(0xFF0284C7))),
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
                                color = (if (isDark) RallyCyan else Color(0xFF0284C7)).copy(alpha = 0.18f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.5.dp,
                                            color = if (isDark) RallyCyan else Color(0xFF0284C7)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CloudSync,
                                            contentDescription = "Sincronizar",
                                            tint = if (isDark) RallyCyan else Color(0xFF0284C7),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSyncing) FredianiGreen.copy(alpha = 0.2f) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = if (isSyncing) "SINCRONIZANDO" else "IDA Y VUELTA",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSyncing) FredianiGreen else textSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Sincronizar Web",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.5.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = if (isSyncing) "Enviando & bajando..." else "Enviar y bajar datos nube",
                                fontSize = 10.5.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // TRAMOS Y TRAZADO
                BotonModuloMenu(
                    titulo = "Tramos y Trazado",
                    subtitulo = "Relevar PE y Enlaces",
                    badge = "${estadisticas.totalTramos} tramos",
                    icono = Icons.Default.DirectionsCar,
                    colorIcono = FredianiGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTramos
                )
            }

            // FILA 2: ITINERARIO OFICIAL + MAPA LIBRE
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonModuloMenu(
                    titulo = "Itinerario Oficial",
                    subtitulo = "Cronograma y horarios",
                    badge = "Secuencia",
                    icono = Icons.Default.Schedule,
                    colorIcono = RallyAccentYellow,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTramos
                )

                BotonModuloMenu(
                    titulo = "Mapa Libre & POIs",
                    subtitulo = "Explorar y marcar puntos",
                    badge = "Satélite",
                    icono = Icons.Default.Map,
                    colorIcono = RallyCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMapaLibre
                )
            }

            // FILA 3: AJUSTES Y SENSORES (Integra Calibrador 1.000m, RaceBox 25Hz y Servidor)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAjustes() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = (if (isDark) RallyCyan else FredianiCyanText).copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Ajustes y Sensores",
                                    tint = if (isDark) RallyCyan else FredianiCyanText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ajustes y Sensores",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.5.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RallyAccentAmber.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "CALIBRADOR & GNSS",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = RallyAccentAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Calibrador 1.000m • RaceBox 25Hz • Servidor Web y Mapbox",
                                fontSize = 11.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ir a Ajustes y Sensores",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // =========================================================================
            // 3. RESUMEN DE PROGRESO DEL RALLY
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EstadisticaItem("Tramos Registrados", "${estadisticas.totalTramos}")
                    VerticalDivider(modifier = Modifier.height(30.dp), color = cardBorder)
                    EstadisticaItem("Tramos Completados", "${estadisticas.tramosCompletados}")
                    VerticalDivider(modifier = Modifier.height(30.dp), color = cardBorder)
                    EstadisticaItem("Km Relevados", String.format("%.1f km", estadisticas.kmRelevados))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (mostrarModalEditarRally && activeRally != null) {
        DialogoEditarRally(
            rally = activeRally!!,
            onDismiss = { mostrarModalEditarRally = false },
            onGuardar = { nom, camp, club, sede, fIni, fFin, fisc, est, area, desc ->
                viewModel.editarRally(activeRally!!, nom, camp, club, sede, fIni, fFin, fisc, est, area, desc)
                mostrarModalEditarRally = false
            }
        )
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
