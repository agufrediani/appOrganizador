package com.example.roadbookorganizador.ui.vineta

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.vineta.Biblioteca
import com.example.roadbookorganizador.vineta.DibujoV1
import com.example.roadbookorganizador.vineta.Flecha
import com.example.roadbookorganizador.vineta.VinetaDibujo

/** Dibujo versión 1 de la viñeta (convierte el formato viejo si todavía no se guardó en v1). */
fun VinetaEntity.dibujoV1(bib: Biblioteca): DibujoV1 =
    VinetaDibujo.leer(bib, dibujo, tulipSvgData, notasSvgData, tulipTipo, peligro)

/** Copia de la viñeta con el dibujo nuevo guardado en v1 y el peligro calculado (punto 11). */
fun VinetaEntity.conDibujo(d: DibujoV1): VinetaEntity {
    val (json, peligroCalculado) = VinetaDibujo.paraGuardar(d)
    return copy(dibujo = json, peligro = peligroCalculado)
}

/** Qué recuadro de la viñeta se está editando. */
enum class CapaEditada { DIBUJO, NOTAS }

/** Lo que se puede insertar desde la barra de símbolos. */
sealed class ItemPaleta {
    abstract val clave: String
    data class SimboloItem(val codigo: String, val nombre: String) : ItemPaleta() {
        override val clave get() = "S:$codigo"
    }
    data class ArmadaItem(val codigo: String, val nombre: String, val flecha: Flecha) : ItemPaleta() {
        override val clave get() = "F:$codigo"
    }
}

/**
 * Recuadro donde se puede soltar un símbolo arrastrado (coordenadas de ventana).
 * [visible] es la parte que se ve en pantalla (para saber si el dedo está adentro) y
 * [completo] el recuadro entero (para calcular x, y de 0 a 1).
 */
class DestinoSoltar(
    var visible: Rect,
    var completo: Rect,
    var alSoltar: (item: ItemPaleta, x: Double, y: Double) -> Unit
)

/**
 * Estado del arrastre de un símbolo desde la barra hasta un recuadro.
 * Todo en coordenadas de ventana, así la barra y la tabla no necesitan conocerse.
 */
class ArrastreSimbolo {
    var item by mutableStateOf<ItemPaleta?>(null)
        private set
    var posicion by mutableStateOf(Offset.Zero)
        private set
    /** Clave del recuadro que está debajo del dedo (para resaltarlo). */
    var destinoBajoDedo by mutableStateOf<Any?>(null)
        private set

    private val destinos = LinkedHashMap<Any, DestinoSoltar>()

    fun registrar(clave: Any, visible: Rect, completo: Rect, alSoltar: (ItemPaleta, Double, Double) -> Unit) {
        val d = destinos[clave]
        if (d == null) destinos[clave] = DestinoSoltar(visible, completo, alSoltar)
        else { d.visible = visible; d.completo = completo; d.alSoltar = alSoltar }
    }

    fun quitar(clave: Any) {
        destinos.remove(clave)
    }

    fun iniciar(i: ItemPaleta, pos: Offset) {
        item = i
        mover(pos)
    }

    fun mover(pos: Offset) {
        posicion = pos
        destinoBajoDedo = destinos.entries.lastOrNull { !it.value.visible.isEmpty && it.value.visible.contains(pos) }?.key
    }

    /** Suelta: si el dedo está dentro de un recuadro, inserta ahí; si no, no pasa nada. */
    fun soltar() {
        val i = item
        val clave = destinoBajoDedo
        val d = clave?.let { destinos[it] }
        item = null
        destinoBajoDedo = null
        val r = d?.completo
        if (i != null && d != null && r != null && r.width > 0f && r.height > 0f) {
            val x = ((posicion.x - r.left) / r.width).toDouble().coerceIn(0.0, 1.0)
            val y = ((posicion.y - r.top) / r.height).toDouble().coerceIn(0.0, 1.0)
            d.alSoltar(i, x, y)
        }
    }

    fun cancelar() {
        item = null
        destinoBajoDedo = null
    }
}
