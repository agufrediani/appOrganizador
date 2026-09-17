package com.example.roadbookorganizador.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EXPORTAR RELEVAMIENTO",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = RallyCyanLight
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumen del Tramo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RallyCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tramo?.identificador ?: "Tramo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = tramo?.nombre ?: "",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = RallySurface)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Distancia Medida:", fontSize = 12.sp, color = OdometerLabel)
                            Text(
                                text = String.format("%.3f km", tramo?.distanciaMedidaReal ?: 0.0),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = RallyAccentYellow
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Indicaciones:", fontSize = 12.sp, color = OdometerLabel)
                            Text(
                                text = "${vinetas.size} marcadas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = RallyCyan
                            )
                        }
                    }
                }
            }

            // BOTÓN EXPORTAR GPX
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RallySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Archivo GPX Estándar",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Incluye waypoints de cada indicación georreferenciada y track satelital continuo para compartir con autoridades, policía y equipos de rescate.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
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
                        colors = ButtonDefaults.buttonColors(containerColor = RallyCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERAR Y COMPARTIR GPX", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // BOTÓN EXPORTAR JSON (PLATAFORMA WEB)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RallySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Paquete JSON (Plataforma Web)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Estructura compatible para importar directamente al Editor Web de Frediani Roadbook o sincronizar vía API.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
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
                        colors = ButtonDefaults.buttonColors(containerColor = RallyAccentYellow),
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
