package com.example.roadbookorganizador.ui.screens

import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.roadbookorganizador.R
import kotlinx.coroutines.delay

/**
 * PANTALLA DE CARGA (SPLASH SCREEN):
 * Reproduce la animación GIF oficial de Frediani Roadbook al iniciar la app.
 * Permite omitir tocando la pantalla o avanza automáticamente luego de la animación.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var yaFinalizo by remember { mutableStateOf(false) }

    fun avanzar() {
        if (!yaFinalizo) {
            yaFinalizo = true
            onSplashFinished()
        }
    }

    // Temporizador automático para avanzar luego de reproducir el splash
    LaunchedEffect(Unit) {
        delay(4000) // 4 segundos de presentación
        avanzar()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { avanzar() },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(resources, R.drawable.splash)
                            val drawable = ImageDecoder.decodeDrawable(source)
                            setImageDrawable(drawable)
                            if (drawable is Animatable) {
                                drawable.start()
                            }
                        } else {
                            setImageResource(R.drawable.splash)
                        }
                    } catch (e: Exception) {
                        setImageResource(R.drawable.logo_frediani_roadbook)
                    }
                }
            }
        )

        // Indicador discreto para saltar si el usuario tiene prisa
        Text(
            text = "Toca para continuar",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }
}
