package com.example.roadbookorganizador.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.CockpitViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarScreen(
    tramoId: Long,
    viewModel: CockpitViewModel,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val tramo by viewModel.tramoActivo.collectAsState()
    val vinetas by viewModel.vinetas.collectAsState()
    var mensajeExportacion by remember { mutableStateOf<String?>(null) }
    val isDark = ThemeManager.isDarkTheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EXPORTAR RELEVAMIENTO",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = if (isDark) RallyCyanLight else FredianiNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = if (isDark) Color.White else Color(0xFF0F172A))
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
                .padding(16.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumen del Tramo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) RallyCardBg else Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) RallySurface else FredianiBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tramo?.identificador ?: "Tramo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = tramo?.nombre ?: "",
                        fontSize = 14.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = if (isDark) RallySurface else Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Distancia Medida:", fontSize = 12.sp, color = if (isDark) OdometerLabel else Color(0xFF64748B))
                            Text(
                                text = String.format("%.3f km", tramo?.distanciaMedidaReal ?: 0.0),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) RallyAccentYellow else FredianiAmberText
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Indicaciones:", fontSize = 12.sp, color = if (isDark) OdometerLabel else Color(0xFF64748B))
                            Text(
                                text = "${vinetas.size} marcadas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) RallyCyan else FredianiCyanText
                            )
                        }
                    }
                }
            }

            // BOTÓN EXPORTAR GOOGLE EARTH 3D (KML)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) RallySurface else Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, FredianiCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = FredianiCyanText, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Earth 3D (.KML)",
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Visualización 3D sobre relieve satelital en Google Earth. Incluye trazado continuo, pines georreferenciados con colores por nivel de peligro y fichas de cada viñeta con odometría y notas.",
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val file = viewModel.exportarKml()
                            file?.let {
                                mensajeExportacion = "KML generado: ${it.name}"
                                compartirArchivo(context, it, "application/vnd.google-earth.kml+xml")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FredianiCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERAR Y ABRIR EN GOOGLE EARTH (KML)", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }

            // BOTÓN EXPORTAR GPX
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) RallySurface else Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) RallySurface else FredianiBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Archivo GPX Estándar",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Incluye waypoints de cada indicación georreferenciada y track satelital continuo para compartir con autoridades, policía y equipos de rescate.",
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val file = viewModel.exportarGpx()
                            file?.let {
                                mensajeExportacion = "GPX generado: ${it.name}"
                                compartirArchivo(context, it, "application/gpx+xml")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) RallyCyan else FredianiGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERAR Y COMPARTIR GPX", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // BOTÓN EXPORTAR JSON (PLATAFORMA WEB)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) RallySurface else Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) RallySurface else FredianiBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Paquete JSON (Plataforma Web)",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Estructura compatible para importar directamente al Editor Web de Frediani Roadbook o sincronizar vía API.",
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val file = viewModel.exportarJson()
                            file?.let {
                                mensajeExportacion = "JSON generado: ${it.name}"
                                compartirArchivo(context, it, "application/json")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) RallyAccentYellow else FredianiAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERAR JSON PARA PLATAFORMA WEB", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            mensajeExportacion?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RallyGreen.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = RallyGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

private fun compartirArchivo(context: android.content.Context, file: File, mimeType: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir Trazado Oficial"))
    } catch (e: Exception) {
        // Fallback genérico
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, file.readText())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir Contenido"))
    }
}
