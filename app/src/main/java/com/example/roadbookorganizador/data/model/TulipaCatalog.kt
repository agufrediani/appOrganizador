package com.example.roadbookorganizador.data.model

data class DiagramaItem(
    val codigo: String,
    val nombre: String,
    val categoria: String, // CURVA, CRUCE, PELIGRO, REFERENCIA
    val iconoNombre: String = ""
)

typealias TulipaItem = DiagramaItem

object DiagramaCatalog {
    val items = listOf(
        // Rectas y continuaciones
        DiagramaItem("RECTA", "Seguir Principal", "REFERENCIA"),
        DiagramaItem("RECTA_HUELLAS", "Seguir por Huellas", "REFERENCIA"),

        // Curvas Derecha
        DiagramaItem("DERECHA_1", "Derecha Rápida (1)", "CURVA"),
        DiagramaItem("DERECHA_2", "Derecha Media (2)", "CURVA"),
        DiagramaItem("DERECHA_3", "Derecha 90° (3)", "CURVA"),
        DiagramaItem("DERECHA_4", "Derecha Cerrada (4)", "CURVA"),
        DiagramaItem("RETOME_DER", "Horquilla / Retome Der", "CURVA"),

        // Curvas Izquierda
        DiagramaItem("IZQUIERDA_1", "Izquierda Rápida (1)", "CURVA"),
        DiagramaItem("IZQUIERDA_2", "Izquierda Media (2)", "CURVA"),
        DiagramaItem("IZQUIERDA_3", "Izquierda 90° (3)", "CURVA"),
        DiagramaItem("IZQUIERDA_4", "Izquierda Cerrada (4)", "CURVA"),
        DiagramaItem("RETOME_IZQ", "Horquilla / Retome Izq", "CURVA"),

        // Cruces y bifurcaciones
        DiagramaItem("CRUCE_RECTO", "Cruce Siga Derecho", "CRUCE"),
        DiagramaItem("CRUCE_DER", "Cruce a la Derecha", "CRUCE"),
        DiagramaItem("CRUCE_IZQ", "Cruce a la Izquierda", "CRUCE"),
        DiagramaItem("BIFURCACION_DER", "Bifurcación Der (Y)", "CRUCE"),
        DiagramaItem("BIFURCACION_IZQ", "Bifurcación Izq (Y)", "CRUCE"),
        DiagramaItem("ROTONDA_1", "Rotonda Salida 1", "CRUCE"),
        DiagramaItem("ROTONDA_2", "Rotonda Salida 2", "CRUCE"),

        // Peligros y Accidentes Geográficos
        DiagramaItem("SALTO", "Salto / Lomo", "PELIGRO"),
        DiagramaItem("VADO", "Vado / Agua", "PELIGRO"),
        DiagramaItem("PUENTE", "Puente Angosto", "PELIGRO"),
        DiagramaItem("TRANQUERA", "Tranquera / Portón", "PELIGRO"),
        DiagramaItem("GUARDAGANADO", "Guardaganado", "PELIGRO"),
        DiagramaItem("CHICANA_DER", "Chicana Entrada Der", "PELIGRO"),
        DiagramaItem("CHICANA_IZQ", "Chicana Entrada Izq", "PELIGRO"),

        // Finales y Controles
        DiagramaItem("LARGADA", "Largada de Tramo / PE", "CONTROL"),
        DiagramaItem("STOP_FIN", "Stop / Fin de PE", "CONTROL"),
        DiagramaItem("CONTROL_HORARIO", "Control Horario (CH)", "CONTROL")
    )

    fun getByCodigo(codigo: String): DiagramaItem {
        return items.find { it.codigo == codigo } ?: DiagramaItem(codigo, codigo, "REFERENCIA")
    }
}

val TulipaCatalog = DiagramaCatalog
