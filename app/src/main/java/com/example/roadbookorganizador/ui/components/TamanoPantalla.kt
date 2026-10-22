package com.example.roadbookorganizador.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Tamaño de la pantalla según el ancho disponible (la forma que recomienda Android, no
 * "horizontal / vertical"):
 * - GRANDE: tablet en horizontal (840 dp o más): mapa, hoja y paleta lado a lado.
 * - MEDIANO: tablet en vertical o celular en horizontal (600 a 839 dp): mapa arriba y hoja abajo;
 *   la paleta se abre como panel desde el costado.
 * - CHICO: celular en vertical (menos de 600 dp): una cosa por vez, con pestañas abajo.
 * Cambia sola al girar el equipo.
 */
enum class TamanoPantalla { CHICO, MEDIANO, GRANDE }

@Composable
fun tamanoPantalla(): TamanoPantalla {
    val ancho = LocalConfiguration.current.screenWidthDp
    return when {
        ancho < 600 -> TamanoPantalla.CHICO
        ancho < 840 -> TamanoPantalla.MEDIANO
        else -> TamanoPantalla.GRANDE
    }
}
