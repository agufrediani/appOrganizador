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
    val rumbo: Float = 0.0f,          // Heading 0-360 grados
    val velocidadKmh: Float = 0.0f,   // Velocidad instantánea al marcar
    val tulipTipo: String = "BLANCO", // Tipo de maniobra/ícono (por defecto en blanco hasta que el usuario dibuje o elija una maniobra)
    val tulipSvgData: String = "",    // Coordenadas o dibujo vectorial opcional
    val notasSvgData: String = "",    // Croquis o dibujo a mano alzada en anotaciones
    val informacion: String = "",     // Notas: "Firme", "Puente angosto"
    val peligro: String = "",         // "", "!", "!!", "!!!"
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
    val timestamp: Long = System.currentTimeMillis()
)
