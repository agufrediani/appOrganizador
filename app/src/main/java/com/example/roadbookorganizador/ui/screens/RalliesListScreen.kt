package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.MenuPrincipalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RalliesListScreen(
    viewModel: MenuPrincipalViewModel,
    onVolver: () -> Unit
) {
    val rallies by viewModel.rallies.collectAsState()
    val activeRally by viewModel.activeRally.collectAsState()
    var rallyParaEditar by remember { mutableStateOf<RallyEntity?>(null) }

    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MIS RALLIES ASIGNADOS",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = if (isDark) RallyCyanLight else FredianiNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { ThemeManager.toggleTheme() }) {
                        Text(text = if (isDark) "☀️" else "🌙", fontSize = 18.sp)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Nota informativa
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = if (isDark) RallyCyan else FredianiCyanText, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Los nuevos rallies se dan de alta desde la Plataforma Web Central y se descargan automáticamente al sincronizar.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(rallies, key = { it.id }) { r ->
                    val esActivo = activeRally?.id == r.id
                    RallyCardItem(
                        rally = r,
                        esActivo = esActivo,
                        onSeleccionar = { viewModel.seleccionarRallyActivo(r.id) },
                        onEditar = { rallyParaEditar = r }
                    )
                }
            }
        }
    }

    rallyParaEditar?.let { r ->
        DialogoEditarRally(
            rally = r,
            onDismiss = { rallyParaEditar = null },
            onGuardar = { nombre, campeonato, club, sede, fecha, fechaFin, fiscalizador, estado, areaKm2, desc ->
                viewModel.editarRally(r, nombre, campeonato, club, sede, fecha, fechaFin, fiscalizador, estado, areaKm2, desc)
                rallyParaEditar = null
            }
        )
    }
}

@Composable
fun RallyCardItem(
    rally: RallyEntity,
    esActivo: Boolean,
    onSeleccionar: () -> Unit,
    onEditar: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) (if (esActivo) RallyCardBg else RallySurface.copy(alpha = 0.5f)) else (if (esActivo) Color(0xFFF0FDF4) else Color.White)
    val cardBorder = if (esActivo) FredianiGreen else (if (isDark) RallySurface else Color(0xFFE2E8F0))
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(if (esActivo) 2.dp else 1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (esActivo) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RallyGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "ACTIVO",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = RallyGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "DISPONIBLE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = rally.nombre, fontWeight = FontWeight.Black, fontSize = 18.sp, color = textPrimary)
            Text(text = "${rally.sede} • ${rally.fecha}", fontSize = 13.sp, color = textSecondary)

            if (rally.organizadorClub.isNotEmpty()) {
                Text(text = "Club: ${rally.organizadorClub}", fontSize = 12.sp, color = if (isDark) RallyCyanLight else FredianiCyanText)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!esActivo) {
                Button(
                    onClick = onSeleccionar,
                    colors = ButtonDefaults.buttonColors(containerColor = RallyCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SELECCIONAR COMO RALLY ACTIVO", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
