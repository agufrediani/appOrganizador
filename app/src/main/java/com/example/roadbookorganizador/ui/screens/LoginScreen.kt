package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.roadbookorganizador.R
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginExitoso: () -> Unit
) {
    var email by remember { mutableStateOf("organizador@frediani.com") }
    var clave by remember { mutableStateOf("123456") }
    var mostrarClave by remember { mutableStateOf(false) }

    val error by viewModel.loginError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RallyDarkBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RallyCardBg),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LOGO OFICIAL FREDIANI ROADBOOK
                    Image(
                        painter = painterResource(id = R.drawable.logo_frediani_roadbook),
                        contentDescription = "Frediani Roadbook",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FREDIANI ROADBOOK",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = RallyCyanLight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Plataforma Oficial de Trazado en Terreno",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RallySurface
                    ) {
                        Text(
                            text = "ACCESO ORGANIZADORES & COMISIÓN DEPORTIVA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RallyAccentYellow,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico / Usuario") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RallyCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = clave,
                        onValueChange = { clave = it },
                        label = { Text("Contraseña de Acceso") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RallyCyan) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarClave = !mostrarClave }) {
                                Icon(
                                    if (mostrarClave) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (mostrarClave) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    error?.let { err ->
                        Text(
                            text = err,
                            color = RallyRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.login(email, clave, onLoginExitoso)
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RallyCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "INGRESAR A LA PLATAFORMA",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            email = "organizador@frediani.com"
                            clave = "123456"
                            viewModel.login(email, clave, onLoginExitoso)
                        }
                    ) {
                        Text(
                            text = "Ingresar con credenciales de prueba",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
