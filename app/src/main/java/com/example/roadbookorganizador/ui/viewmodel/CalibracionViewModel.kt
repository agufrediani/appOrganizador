package com.example.roadbookorganizador.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.entity.CalibracionEntity
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import com.example.roadbookorganizador.service.OdometerEngine
import com.example.roadbookorganizador.service.OdometerState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CalibracionPaso {
    LISTO_PARA_INICIAR,
    EN_MARCHA,
    FINALIZADO
}

class CalibracionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoadbookRepository(AppDatabase.getInstance(application))
    val odoEngine = OdometerEngine()

    val odoState: StateFlow<OdometerState> = odoEngine.state
    val calibracionActiva = repository.getCalibracionActiva()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _paso = MutableStateFlow(CalibracionPaso.LISTO_PARA_INICIAR)
    val paso: StateFlow<CalibracionPaso> = _paso.asStateFlow()

    private val _distanciaMedidaMetros = MutableStateFlow(0.0)
    val distanciaMedidaMetros: StateFlow<Double> = _distanciaMedidaMetros.asStateFlow()

    private val _factorCalculado = MutableStateFlow(1.0)
    val factorCalculado: StateFlow<Double> = _factorCalculado.asStateFlow()

    init {
        // En calibración el factor base debe ser 1.0 para medir metros satelitales puros
        odoEngine.setFactorCalibracion(1.0)
    }

    fun iniciarRecorridoCalibracion() {
        odoEngine.resetTotalYParcial(0.0)
        _paso.value = CalibracionPaso.EN_MARCHA
    }

    fun finalizarRecorridoCalibracion(distanciaOficialMetros: Double = 1000.0, vehiculoNombre: String = "Auto Trazador") {
        val totalKm = odoEngine.state.value.odometroTotalKm
        val metrosMedidos = totalKm * 1000.0
        _distanciaMedidaMetros.value = metrosMedidos

        val factor = if (metrosMedidos > 10.0) distanciaOficialMetros / metrosMedidos else 1.0
        _factorCalculado.value = factor
        _paso.value = CalibracionPaso.FINALIZADO

        viewModelScope.launch {
            repository.guardarCalibracion(vehiculoNombre, metrosMedidos, distanciaOficialMetros)
        }
    }

    fun reiniciarCalibrador() {
        odoEngine.resetTotalYParcial(0.0)
        _paso.value = CalibracionPaso.LISTO_PARA_INICIAR
    }
}
