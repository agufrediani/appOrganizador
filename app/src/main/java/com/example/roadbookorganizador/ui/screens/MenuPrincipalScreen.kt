package com.example.roadbookorganizador.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.UsuarioSesion
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.MenuPrincipalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuPrincipalScreen(
    viewModel: MenuPrincipalViewModel,
    sesion: UsuarioSesion,
    onNavigateToTramos: () -> Unit,
    onNavigateToRallies: () -> Unit,
    onNavigateToMapaLibre: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RallyCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = RallyCyan, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = sesion.nombre.ifBlank { "Organizador Oficial" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = sesion.club.ifBlank { "Frediani Competición" },
                                fontSize = 11.sp,
                                color = RallyCyanLight
                            )
                        }
                    }
                },
                actions = {
                    // Engranaje de Ajustes (AQUÍ ESTÁ EL CALIBRADOR DISCRETO)
                    IconButton(onClick = onNavigateToAjustes) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
                    }
                    // Cerrar sesión
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RallyDarkBg)
            )
        },
        containerColor = RallyDarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TARJETA RALLY ACTIVO Y DASHBOARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RallyCardBg)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RallyCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "RALLY ACTIVO",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = RallyCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { mostrarModalEditarRally = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Editar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onNavigateToRallies,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RallySurface)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = RallyAccentYellow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cambiar", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = activeRally?.nombre ?: "Sin Rally Seleccionado",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${activeRally?.sede ?: "Sede"} • ${activeRally?.fecha ?: "Fecha"}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = RallySurface)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Estadísticas del Rally
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EstadisticaItem("Tramos", "${estadisticas.totalTramos}")
                        EstadisticaItem("Completados", "${estadisticas.tramosCompletados}")
                        EstadisticaItem("Km Relevados", String.format("%.1f km", estadisticas.kmRelevados))
                    }
                }
            }

            // BOTÓN SINCRONIZAR CON LA PLATAFORMA WEB
            Button(
                onClick = { viewModel.sincronizarConWeb() },
                enabled = !isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RallyCyan)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SINCRONIZANDO CON LA WEB...", fontWeight = FontWeight.Black, color = Color.Black)
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SINCRONIZAR CON PLATAFORMA WEB",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }

            Text(
                text = "MÓDULOS DE OPERACIÓN",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = RallyCyan,
                letterSpacing = 1.sp
            )

            // GRID DE BOTONES DE ACCESO PRINCIPAL
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonModuloMenu(
                    titulo = "Tramos y Trazado",
                    subtitulo = "Relevar PE y Enlaces",
                    icono = Icons.Default.DirectionsCar,
                    colorIcono = RallyCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTramos
                )
                BotonModuloMenu(
                    titulo = "Mapa Libre",
                    subtitulo = "Explorar y POIs",
                    icono = Icons.Default.Map,
                    colorIcono = RallyAccentYellow,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMapaLibre
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonModuloMenu(
                    titulo = "Mis Rallies",
                    subtitulo = "Gestionar eventos",
                    icono = Icons.Default.EmojiEvents,
                    colorIcono = RallyGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToRallies
                )
                BotonModuloMenu(
                    titulo = "Ajustes & Calibrador",
                    subtitulo = "Odómetro y GPS",
                    icono = Icons.Default.Tune,
                    colorIcono = RallyAccentAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAjustes
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = titulo, fontSize = 11.sp, color = OdometerLabel)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = valor, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
fun BotonModuloMenu(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    colorIcono: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RallyCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorIcono.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(24.dp))
                }
            }

            Column {
                Text(text = titulo, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                Text(text = subtitulo, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
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
        containerColor = RallyCardBg,
        title = {
            Text("Editar Ficha del Rally (Oficial)", fontWeight = FontWeight.Bold, color = Color.White)
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RallyCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = campeonato,
                    onValueChange = { campeonato = it },
                    label = { Text("Campeonato (ej: Rally Cordobés, Argentino)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RallyCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sede,
                    onValueChange = { sede = it },
                    label = { Text("Sede / Localidad (ej: Villa Carlos Paz)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RallyCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fechaInicio,
                        onValueChange = { fechaInicio = it },
                        label = { Text("Fecha Inicio (YYYY-MM-DD)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = fechaFin,
                        onValueChange = { fechaFin = it },
                        label = { Text("Fecha Fin (YYYY-MM-DD)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = club,
                        onValueChange = { club = it },
                        label = { Text("Club Organizador") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = fiscalizador,
                        onValueChange = { fiscalizador = it },
                        label = { Text("Fiscalizador (FRADC / CDA)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = estado,
                        onValueChange = { estado = it },
                        label = { Text("Estado (ACTIVO, PLANIFICADO)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = areaKm2,
                        onValueChange = { areaKm2 = it },
                        label = { Text("Área (km²)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción / Observaciones Generales") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RallyCyan
                    ),
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
                colors = ButtonDefaults.buttonColors(containerColor = RallyCyan)
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
