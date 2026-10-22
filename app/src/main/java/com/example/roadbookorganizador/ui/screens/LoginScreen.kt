package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.SessionManager
import com.example.roadbookorganizador.ui.theme.ThemeManager
import com.example.roadbookorganizador.ui.viewmodel.AuthViewModel
import com.example.roadbookorganizador.ui.viewmodel.EstadoServidor

private val CELESTE = Color(0xFF00B4D8)
private val AMARILLO = Color(0xFFFFB703)
private val VERDE = Color(0xFF06D6A0)
private val TARJETA = Color(0xFF131B2A)
private val CAMPO = Color(0xFF1C273A)
private val BORDE = Color(0xFF2A374D)
private val GRIS = Color(0xFF94A3B8)

/**
 * Inicio de sesión, igual al de la plataforma (App.tsx): tarjeta oscura con la franja de colores
 * arriba, "FREDIANI ROADBOOK · Plataforma Oficial Digital", estado del servidor, Usuario,
 * Contraseña y "Acceder a la Plataforma". Se entra con el mismo usuario y clave de la web.
 *
 * Lo único propio de la tablet: la dirección del servidor se puede cambiar (para probar contra
 * la PC: http://<IP-de-la-PC>:5000).
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginExitoso: () -> Unit
) {
    var usuario by remember { mutableStateOf(viewModel.ultimoUsuario) }
    var clave by remember { mutableStateOf("") }
    var mostrarClave by remember { mutableStateOf(false) }
    var editandoServidor by remember { mutableStateOf(false) }

    val error by viewModel.loginError.collectAsState()
    val cargando by viewModel.isLoading.collectAsState()
    val estado by viewModel.estadoServidor.collectAsState()
    val servidor by viewModel.serverUrl.collectAsState()
    val motivo by viewModel.motivoServidor.collectAsState()
    val isDark = ThemeManager.isDarkTheme

    val entrar = { if (!cargando) viewModel.login(usuario, clave, onLoginExitoso) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0A0E17) else Color(0xFFF1F5F9))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 448.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TARJETA)
                .border(1.dp, BORDE, RoundedCornerShape(24.dp))
        ) {
            // Franja de colores de arriba
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(CELESTE, AMARILLO, VERDE)))
            )
            Column(modifier = Modifier.padding(32.dp)) {
                // Encabezado: bandera, nombre y botón de tema
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CELESTE.copy(alpha = 0.2f))
                            .border(1.dp, CELESTE.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) { Icon(Icons.Default.Flag, contentDescription = null, tint = CELESTE, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("FREDIANI ROADBOOK", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("PLATAFORMA OFICIAL DIGITAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CELESTE, letterSpacing = 0.6.sp)
                    }
                    com.example.roadbookorganizador.ui.components.BotonTema()
                }

                Spacer(Modifier.height(24.dp))

                // Estado del servidor (y su dirección, que se puede cambiar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CAMPO)
                        .border(1.dp, BORDE, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Estado del servidor:", fontSize = 12.sp, color = GRIS, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        val (txt, col) = when (estado) {
                            EstadoServidor.EN_LINEA -> "● ONLINE" to VERDE
                            EstadoServidor.DESCONECTADO -> "○ DESCONECTADO" to Color(0xFFFB7185)
                            EstadoServidor.REVISANDO -> "… REVISANDO" to GRIS
                        }
                        Text(
                            txt, fontSize = 10.sp, fontWeight = FontWeight.Black, color = col,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(col.copy(alpha = 0.2f))
                                .border(1.dp, col.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .clickable { viewModel.revisarServidor() }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (estado == EstadoServidor.DESCONECTADO && motivo != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(motivo!!, fontSize = 11.sp, color = Color(0xFFFDA4AF), fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!editandoServidor) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { editandoServidor = true }
                        ) {
                            Text(servidor, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Edit, contentDescription = "Cambiar servidor", tint = GRIS, modifier = Modifier.size(14.dp))
                        }
                    } else {
                        var nuevo by remember { mutableStateOf(servidor) }
                        CampoPlataforma(
                            valor = nuevo, onCambio = { nuevo = it },
                            placeholder = SessionManager.SERVIDOR_PRODUCCION,
                            teclado = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            acciones = KeyboardActions(onDone = { viewModel.cambiarServidor(nuevo); editandoServidor = false })
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Para probar contra la PC: http://<IP-de-la-PC>:5000",
                            fontSize = 10.sp, color = GRIS
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            TextButton(onClick = { nuevo = SessionManager.SERVIDOR_PRODUCCION }) {
                                Text("Usar Azure", fontSize = 11.sp, color = GRIS, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { editandoServidor = false }) {
                                Text("Cancelar", fontSize = 11.sp, color = GRIS, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { viewModel.cambiarServidor(nuevo); editandoServidor = false }) {
                                Text("Guardar", fontSize = 11.sp, color = CELESTE, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("USUARIO", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFCBD5E1), letterSpacing = 0.6.sp)
                Spacer(Modifier.height(6.dp))
                CampoPlataforma(
                    valor = usuario, onCambio = { usuario = it },
                    placeholder = "director, piloto o trazador",
                    teclado = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(Modifier.height(16.dp))

                Text("CONTRASEÑA", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFCBD5E1), letterSpacing = 0.6.sp)
                Spacer(Modifier.height(6.dp))
                CampoPlataforma(
                    valor = clave, onCambio = { clave = it },
                    placeholder = "",
                    oculto = !mostrarClave,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                    acciones = KeyboardActions(onGo = { entrar() }),
                    final = {
                        IconButton(onClick = { mostrarClave = !mostrarClave }) {
                            Icon(if (mostrarClave) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Ver contraseña", tint = GRIS)
                        }
                    }
                )

                error?.let { e ->
                    Spacer(Modifier.height(16.dp))
                    Text(
                        e, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDA4AF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33F43F5E))
                            .border(1.dp, Color(0x66F43F5E), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { entrar() },
                    enabled = !cargando,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CELESTE, disabledContainerColor = CELESTE.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (cargando) {
                        CircularProgressIndicator(color = Color(0xFF0A0E17), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("ACCEDER A LA PLATAFORMA", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0A0E17), letterSpacing = 0.6.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF0A0E17), modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = BORDE)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Entrá con el mismo usuario y contraseña de la plataforma web.",
                    fontSize = 11.sp, color = GRIS, modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/** Campo de texto con el estilo de la plataforma: fondo #1C273A, borde #2A374D, celeste al enfocar. */
@Composable
private fun CampoPlataforma(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    oculto: Boolean = false,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    acciones: KeyboardActions = KeyboardActions.Default,
    final: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Color(0xFF64748B)) },
        singleLine = true,
        visualTransformation = if (oculto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = teclado,
        keyboardActions = acciones,
        trailingIcon = final,
        shape = RoundedCornerShape(12.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CAMPO,
            unfocusedContainerColor = CAMPO,
            focusedBorderColor = CELESTE,
            unfocusedBorderColor = BORDE,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = CELESTE
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
