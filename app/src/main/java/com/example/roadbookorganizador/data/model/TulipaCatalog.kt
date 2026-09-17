package com.example.roadbookorganizador.data.model

data class TulipaItem(
    val codigo: String,
    val nombre: String,
    val categoria: String, // CURVA, CRUCE, PELIGRO, REFERENCIA
    val iconoNombre: String = ""
)

object TulipaCatalog {
    val items = listOf(
        // Rectas y continuaciones
        TulipaItem("RECTA", "Seguir Principal", "REFERENCIA"),
        TulipaItem("RECTA_HUELLAS", "Seguir por Huellas", "REFERENCIA"),

        // Curvas Derecha
        TulipaItem("DERECHA_1", "Derecha Rápida (1)", "CURVA"),
        TulipaItem("DERECHA_2", "Derecha Media (2)", "CURVA"),
        TulipaItem("DERECHA_3", "Derecha 90° (3)", "CURVA"),
        TulipaItem("DERECHA_4", "Derecha Cerrada (4)", "CURVA"),
        TulipaItem("RETOME_DER", "Horquilla / Retome Der", "CURVA"),

        // Curvas Izquierda
        TulipaItem("IZQUIERDA_1", "Izquierda Rápida (1)", "CURVA"),
        TulipaItem("IZQUIERDA_2", "Izquierda Media (2)", "CURVA"),
        TulipaItem("IZQUIERDA_3", "Izquierda 90° (3)", "CURVA"),
        TulipaItem("IZQUIERDA_4", "Izquierda Cerrada (4)", "CURVA"),
        TulipaItem("RETOME_IZQ", "Horquilla / Retome Izq", "CURVA"),

        // Cruces y bifurcaciones
        TulipaItem("CRUCE_RECTO", "Cruce Siga Derecho", "CRUCE"),
        TulipaItem("CRUCE_DER", "Cruce a la Derecha", "CRUCE"),
        TulipaItem("CRUCE_IZQ", "Cruce a la Izquierda", "CRUCE"),
        TulipaItem("BIFURCACION_DER", "Bifurcación Der (Y)", "CRUCE"),
        TulipaItem("BIFURCACION_IZQ", "Bifurcación Izq (Y)", "CRUCE"),
        TulipaItem("ROTONDA_1", "Rotonda Salida 1", "CRUCE"),
        TulipaItem("ROTONDA_2", "Rotonda Salida 2", "CRUCE"),

        // Peligros y Accidentes Geográficos
        TulipaItem("SALTO", "Salto / Lomo", "PELIGRO"),
        TulipaItem("VADO", "Vado / Agua", "PELIGRO"),
        TulipaItem("PUENTE", "Puente Angosto", "PELIGRO"),
        TulipaItem("TRANQUERA", "Tranquera / Portón", "PELIGRO"),
        TulipaItem("GUARDAGANADO", "Guardaganado", "PELIGRO"),
        TulipaItem("CHICANA_DER", "Chicana Entrada Der", "PELIGRO"),
        TulipaItem("CHICANA_IZQ", "Chicana Entrada Izq", "PELIGRO"),

        // Finales y Controles
        TulipaItem("LARGADA", "Largada de Tramo / PE", "CONTROL"),
        TulipaItem("STOP_FIN", "Stop / Fin de PE", "CONTROL"),
        TulipaItem("CONTROL_HORARIO", "Control Horario (CH)", "CONTROL")
    )

    fun getByCodigo(codigo: String): TulipaItem {
        return items.find { it.codigo == codigo } ?: TulipaItem(codigo, codigo, "REFERENCIA")
    }
}
