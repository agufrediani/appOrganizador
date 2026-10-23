package com.example.roadbookorganizador.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vinetas",
    foreignKeys = [
        ForeignKey(
            entity = TramoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tramoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tramoId")]
)
data class VinetaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tramoId: Long,
    val numero: Int,
    val distanciaTotal: Double,       // Km acumulados con 3 decimales
    val distanciaParcial: Double,     // Km desde la viñeta previa
    val latitud: Double,
    val longitud: Double,
    val altitud: Double = 0.0,        // Metros sobre el nivel del mar
    val rumbo: Float = 0.0f,          // Rumbo (CAP) cargado a mano. No se calcula solo ni se muestra (punto 12)
    val velocidadKmh: Float = 0.0f,   // Velocidad instantánea al marcar
    val tulipTipo: String = "BLANCO", // Tipo de maniobra/ícono (por defecto en blanco hasta que el usuario dibuje o elija una maniobra)
    val tulipSvgData: String = "",    // Coordenadas o dibujo vectorial opcional
    val notasSvgData: String = "",    // Croquis o dibujo a mano alzada en anotaciones
    // Dibujo y capa de notas en el formato versión 1 de la especificación (texto JSON).
    // null = nunca se guardó en versión 1: se arma desde tulipSvgData / notasSvgData / tulipTipo,
    // que quedan como respaldo. "" = viñeta sin dibujo.
    val dibujo: String? = null,
    val informacion: String = "",     // Notas: "Firme", "Puente angosto"
    val peligro: String = "",         // "", "!", "!!", "!!!" — se calcula al guardar desde los símbolos EXCL_n / PELIGRO_n
    val esOffRoad: Boolean = false,   // True = trazo directo sin snap / False = ajustado a red de caminos
    val velocidadPromedioSectorKmh: Double = 0.0, // Promedio exigido en este sector
    val velocidadMaximaSectorKmh: Double = 0.0,   // Velocidad máxima permitida en este sector (DZ/Radar)
    val tiempoIdealSegundos: Double = 0.0,        // Tiempo objetivo acumulado o de sector en segundos
    val radioCapturaMetros: Int = 30,             // Radio virtual para geofencing WPV
    val horaPasoRealMillis: Long? = null,         // Timestamp UTC de cruce registrado
    val velocidadPasoRealKmh: Float? = null,      // Velocidad registrada al cruzar el WPV
    val fotoUri: String? = null,      // Foto capturada in situ
    val audioUri: String? = null,     // Nota de voz rápida
    val esReinicioCero: Boolean = false,    // Opción 0,00: Reinicia odómetro para inicio de tramo (duplicación en PDF)
    val distanciaOculta: Boolean = false,   // Opción X.XX: Distancia oculta a competidores en regularidad
    val esOcultaOrg: Boolean = false,       // Opción OCUL: Viñeta oculta/secreta solo visible para la organización
    val mostrarWpt: Boolean = false,        // Opción WPT: Coordenadas GPS visibles para el competidor
    val esTierra: Boolean = false,          // Opción DIRT: Terreno de tierra (vs asfalto)
    val esPuntoControl: Boolean = false,    // Marca si la viñeta actúa como Punto de Control (PC)
    val tipoPuntoControl: String = "",      // "REGULARIDAD", "PASO", "CH", "RADAR"
    val timestamp: Long = System.currentTimeMillis(),

    // ---- Taqueo (ESPECIFICACION_TAQUEO.md): los mismos datos que la plataforma ----
    /** Identificador fijo de la viñeta, el mismo en la plataforma y en todas las tablets (32 hex). */
    val uid: String = nuevoUid(),
    /** De dónde salió la coordenada: MAPA, ARCHIVO, RUTA o TABLET. */
    val origenCoordenada: String? = null,
    /** Cuándo se puso la coordenada actual (UTC, ISO 8601). */
    val coordenadaFechaUtc: String? = null,
    /** Usuario que la puso. */
    val coordenadaPor: String? = null,
    /** Precisión del GPS en metros (solo si vino de la tablet). */
    val precisionGpsM: Double? = null,
    /** true si alguien pasó por ahí con la tablet y la confirmó o la taqueó. */
    val verificadaTerreno: Boolean = false,
    /** La coordenada que había antes de la última (para volver atrás). */
    val latitudAnterior: Double? = null,
    val longitudAnterior: Double? = null,

    // ---- Sincronización con la plataforma ----
    /** id numérico de la viñeta en el servidor (para `PUT /api/indicaciones/{id}`). null = todavía no está en la plataforma. */
    val webId: Int? = null,
    /** La viñeta tal como vino del servidor la última vez (JSON). Sirve para mandar solo lo que cambió en la tablet. */
    val datosServidor: String? = null,
    /** true = tiene cambios hechos en la tablet que todavía no llegaron a la plataforma. */
    val pendienteSubir: Boolean = false,
    /**
     * El camino desde la viñeta anterior hasta esta, para dibujar la línea en el mapa sin volver a
     * pedirlo (también sin señal). JSON `[[lat,lng],...]`, como las zonas. Vale solo si empieza en
     * la viñeta anterior y termina en esta (si alguna se movió, se vuelve a pedir).
     */
    val geometriaDesdeAnterior: String? = null,
    /** true = el camino cambió en la tablet y falta subirlo (`PUT /api/indicaciones/{id}/camino`, revisión 23). */
    val caminoPendiente: Boolean = false
) {
    companion object {
        /** uid nuevo creado por la tablet: 32 caracteres hexadecimales. */
        fun nuevoUid(): String = java.util.UUID.randomUUID().toString().replace("-", "")

        /** Fecha UTC en el formato ISO de la plataforma. */
        fun ahoraUtc(): String = java.time.Instant.now().toString()
    }
}
