package com.example.roadbookorganizador.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.UsuarioSesion
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.MenuPrincipalViewModel

/**
 * PRIMER PANTALLA AL ENTRAR A LA APP (PUERTA DE ACCESO):
 * Muestra el Rally actualmente seleccionado/asignado (ej. "Rally de Fuentes 2026")
 * o el estado vacío con sincronización si no hay ningún rally asignado todavía.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionRallyScreen(
    viewModel: MenuPrincipalViewModel,
    sesion: UsuarioSesion,
    onIngresarAlRally: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activeRally by viewModel.activeRally.collectAsState()
    val rallies by viewModel.rallies.collectAsState()
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RallyCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = RallyCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FREDIANI ROADBOOK",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${sesion.nombre.ifBlank { "Organizador Oficial" }} • ${sesion.club.ifBlank { "Frediani Competición" }}",
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
                    // Ajustes
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // =========================================================================
            // CASO A: TIENE UN RALLY ASIGNADO / SELECCIONADO
            // =========================================================================
            if (activeRally != null) {
                val rally = activeRally!!

                item {
                    Text(
                        text = "RALLY SELECCIONADO",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = if (isDark) RallyCyan else FredianiCyanText,
                        letterSpacing = 1.sp
                    )
                }

                // TARJETA PRINCIPAL PROTAGONISTA DEL RALLY ASIGNADO
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.5.dp, if (isDark) FredianiGreen.copy(alpha = 0.6f) else FredianiGreen),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Cabecera de la tarjeta con Badges y botón editar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = FredianiGreen.copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(FredianiGreen)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "RALLY ASIGNADO",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = FredianiGreen
                                            )
                                        }
                                    }

                                    if (rally.sincronizado) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = RallyCyan.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "☁️ SINCRONIZADO",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (isDark) RallyCyan else FredianiCyanText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { mostrarModalEditarRally = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFFCBD5E1))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = textPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Detalles", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Nombre del Rally en grande y llamativo
                            Text(
                                text = rally.nombre,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = textPrimary,
                                lineHeight = 28.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Sede, Fecha y Campeonato
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isDark) RallyAccentYellow else FredianiAmberText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = rally.sede.ifBlank { "Sede no especificada" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = textPrimary
                                )
                                Text(text = "•", color = textSecondary)
                                Text(
                                    text = rally.fecha.ifBlank { "Fecha por definir" },
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                            }

                            if (rally.campeonato.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🏆 ${rally.campeonato}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) RallyCyanLight else FredianiCyanText
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = cardBorder)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Estadísticas rápidas del Rally
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Tramos", fontSize = 11.sp, color = textSecondary)
                                    Text("${estadisticas.totalTramos}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = textPrimary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Completados", fontSize = 11.sp, color = textSecondary)
                                    Text("${estadisticas.tramosCompletados}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = FredianiGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Km Relevados", fontSize = 11.sp, color = textSecondary)
                                    Text(String.format("%.1f km", estadisticas.kmRelevados), fontSize = 18.sp, fontWeight = FontWeight.Black, color = textPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // =========================================================
                            // BOTÓN PRINCIPAL DESTACADO: INGRESAR AL RALLY
                            // =========================================================
                            Button(
                                onClick = onIngresarAlRally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "INGRESAR AL RALLY",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Botón secundario: Sincronizar este rally con la web
                            OutlinedButton(
                                onClick = { viewModel.sincronizarConWeb() },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDark) RallyCyan.copy(alpha = 0.5f) else FredianiCyan),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isDark) RallyCyan else FredianiCyanText
                                )
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isDark) RallyCyan else FredianiCyanText
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sincronizando con servidor...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sincronizar eventos con la web (Ida y Vuelta)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // OTROS RALLIES DISPONIBLES EN EL DISPOSITIVO
                // =========================================================================
                val otrosRallies = rallies.filter { it.id != rally.id }
                if (otrosRallies.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "OTROS RALLIES ASIGNADOS (${otrosRallies.size})",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = textSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    items(otrosRallies, key = { it.id }) { otro ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.seleccionarRallyActivo(otro.id) },
                            shape = RoundedCornerShape(14.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = otro.nombre,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "${otro.sede} • ${otro.fecha}",
                                        fontSize = 12.sp,
                                        color = textSecondary
                                    )
                                }

                                Button(
                                    onClick = { viewModel.seleccionarRallyActivo(otro.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "SELECCIONAR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) RallyCyan else FredianiCyanText
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // =========================================================================
                // CASO B: NO TIENE NINGÚN RALLY ASIGNADO O SELECCIONADO
                // =========================================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.5.dp, cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) RallySurface else Color(0xFFF1F5F9),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (isDark) RallyAccentYellow else FredianiAmberText,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Sin Rally Asignado",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "No tienes ningún rally asignado en este dispositivo en este momento.\n\nSincroniza con la plataforma web central para descargar los rallies asignados a tu usuario organizador.",
                                fontSize = 13.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // BOTÓN DE SINCRONIZACIÓN WEB DESTACADO
                            Button(
                                onClick = { viewModel.sincronizarConWeb() },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RallyCyan)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("DESCARGANDO RALLIES...", fontWeight = FontWeight.Black, color = Color.Black)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SINCRONIZAR CON LA WEB AHORA",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Si hay rallies guardados en la base de datos pero ninguno está marcado como activo
                if (rallies.isNotEmpty()) {
                    item {
                        Text(
                            text = "RALLIES GUARDADOS EN EL DISPOSITIVO",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = textSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    items(rallies, key = { it.id }) { r ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.seleccionarRallyActivo(r.id) },
                            shape = RoundedCornerShape(14.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = r.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textPrimary)
                                    Text(text = "${r.sede} • ${r.fecha}", fontSize = 12.sp, color = textSecondary)
                                }

                                Button(
                                    onClick = { viewModel.seleccionarRallyActivo(r.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("ACTIVAR", fontWeight = FontWeight.Black, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
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
