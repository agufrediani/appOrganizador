package com.example.roadbookorganizador.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn

/**
 * Refresco de pantalla: el odómetro y el RaceBox se siguen procesando a 25 Hz (precisión
 * intacta), pero la pantalla se redibuja como máximo 5 veces por segundo, que es lo que el ojo
 * alcanza a leer. Ahorra batería y temperatura de la tablet.
 */
const val PERIODO_PANTALLA_MS = 200L

@OptIn(FlowPreview::class)
fun <T> StateFlow<T>.paraPantalla(scope: CoroutineScope, periodoMs: Long = PERIODO_PANTALLA_MS): StateFlow<T> =
    sample(periodoMs).stateIn(scope, SharingStarted.WhileSubscribed(5000), value)

@OptIn(FlowPreview::class)
fun <T> Flow<T>.muestrearParaPantalla(periodoMs: Long = PERIODO_PANTALLA_MS): Flow<T> = sample(periodoMs)
