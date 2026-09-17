package com.example.roadbookorganizador.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.SessionManager
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RallyEstadisticas(
    val totalTramos: Int = 0,
    val tramosCompletados: Int = 0,
    val kmRelevados: Double = 0.0
)

class MenuPrincipalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoadbookRepository(AppDatabase.getInstance(application))

    val rallies: StateFlow<List<RallyEntity>> = repository.getAllRallies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRally: StateFlow<RallyEntity?> = repository.getActiveRally()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tramosRallyActivo = activeRally.flatMapLatest { rally ->
        if (rally != null) repository.getTramosByRally(rally.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val estadisticas = tramosRallyActivo.map { list ->
        val total = list.size
        val comp = list.count { it.estadoTrazado == "COMPLETADO" }
        val kms = list.sumOf { it.distanciaMedidaReal }
        RallyEstadisticas(total, comp, kms)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RallyEstadisticas())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableSharedFlow<String>()
    val syncMessage: SharedFlow<String> = _syncMessage.asSharedFlow()

    init {
        // Asegurar que si hay rallies pero ninguno está marcado activo, activar el primero
        viewModelScope.launch {
            rallies.collect { list ->
                if (list.isNotEmpty() && activeRally.value == null) {
                    repository.setRallyActivo(list.first().id)
                }
            }
        }
    }

    fun seleccionarRallyActivo(rallyId: Long) {
        viewModelScope.launch {
            repository.setRallyActivo(rallyId)
        }
    }

    fun editarRally(
        rally: RallyEntity,
        nuevoNombre: String,
        campeonato: String,
        club: String,
        sede: String,
        fecha: String,
        fechaFin: String,
        fiscalizador: String,
        estado: String,
        areaKm2: Double,
        desc: String
    ) {
        viewModelScope.launch {
            val actualizado = rally.copy(
                nombre = nuevoNombre,
                campeonato = campeonato,
                organizadorClub = club,
                sede = sede,
                fecha = fecha,
                fechaFin = fechaFin,
                fiscalizador = fiscalizador,
                estadoRally = estado,
                areaKm2 = areaKm2,
                descripcion = desc
            )
            repository.updateRally(actualizado)
        }
    }

    private val sessionManager = SessionManager(application)
    private val webSyncService = com.example.roadbookorganizador.data.remote.WebSyncService(repository)

    fun sincronizarConWeb() {
        viewModelScope.launch {
            _isSyncing.value = true
            val serverUrl = sessionManager.serverUrl.value
            val result = webSyncService.sincronizarConServidor(serverUrl)

            if (result.success) {
                val activo = activeRally.value
                if (activo != null) {
                    repository.updateRally(activo.copy(sincronizado = true))
                }
            }

            _isSyncing.value = false
            _syncMessage.emit(result.message)
        }
    }
}
