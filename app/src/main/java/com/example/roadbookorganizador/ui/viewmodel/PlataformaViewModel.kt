package com.example.roadbookorganizador.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.remote.ApiCliente
import com.example.roadbookorganizador.data.remote.ErrorServidor
import com.example.roadbookorganizador.data.remote.SesionVencida
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import com.example.roadbookorganizador.ui.components.AvisoSuperior
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Las pestañas del rally que se leen directo de la plataforma (necesitan señal):
 * Cronograma (`GET /api/rallies/{id}/itinerario`) y Director de carrera
 * (`/api/control-carrera/...`). Lo que se ve es lo mismo que en la web.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlataformaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoadbookRepository(AppDatabase.getInstance(application))
    private val api = ApiCliente(application)

    val rally: StateFlow<RallyEntity?> = repository.getActiveRally()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tramos: StateFlow<List<TramoEntity>> = rally.flatMapLatest { r ->
        if (r != null) repository.getTramosByRally(r.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------------- Cronograma ----------------
    private val _itinerario = MutableStateFlow<JsonObject?>(null)
    val itinerario: StateFlow<JsonObject?> = _itinerario.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    /** Motivo por el que no se pudo leer (sin señal, sin permiso...), o null. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private suspend fun rallyWebId(): Int? {
        val r = rally.value ?: repository.getActiveRally().first()
        return r?.webId
    }

    private fun motivo(e: Exception): String = when (e) {
        is SesionVencida -> e.message ?: "La sesión venció."
        is ErrorServidor -> when (e.codigo) {
            403 -> "Tu usuario no tiene permiso para esto en la plataforma."
            else -> textoDelServidor(e.detalle) ?: if (e.codigo == 404) "La plataforma no encontró lo que se pidió." else (e.message ?: "Error del servidor.")
        }
        else -> "Sin conexión con la plataforma. Revisá la señal y probá de nuevo."
    }

    /** El texto que manda el servidor en un error (`mensaje`, `error` o `message`, o el texto solo). */
    private fun textoDelServidor(detalle: String): String? {
        val t = detalle.trim()
        if (t.isEmpty()) return null
        return try {
            val el = com.google.gson.JsonParser.parseString(t)
            when {
                el.isJsonObject -> el.asJsonObject.let { o ->
                    listOf("mensaje", "Mensaje", "error", "message", "title").firstNotNullOfOrNull { k -> o.get(k)?.takeIf { it.isJsonPrimitive }?.asString }
                }
                el.isJsonPrimitive -> el.asString
                else -> null
            }
        } catch (e: Exception) { t.take(200) }
    }

    // ---------------- Pedidos del cronograma (días, sectores, anexos) ----------------
    private val _detalleRally = MutableStateFlow<JsonObject?>(null)
    /** `GET /api/rallies/{id}`: los días con su contraseña y los tramos del rally (para los selectores). */
    val detalleRally: StateFlow<JsonObject?> = _detalleRally.asStateFlow()

    private val _anexos = MutableStateFlow<List<JsonObject>>(emptyList())
    val anexos: StateFlow<List<JsonObject>> = _anexos.asStateFlow()

    /** Cartel para mostrar (los mismos textos que los avisos de la plataforma). */
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()
    fun cerrarMensaje() { _mensaje.value = null }
    fun mostrarMensaje(texto: String) { _mensaje.value = texto }

    /** El tramo de la tablet que corresponde a un tramo de la plataforma (null si no se bajó). */
    fun tramoLocal(tramoWebId: Int): Long? = tramos.value.firstOrNull { it.webId == tramoWebId }?.id

    /** El id del rally en la plataforma (para armar las direcciones). */
    suspend fun idRally(): Int? = rallyWebId()

    /** Lee el cronograma, los anexos y los datos de los días. */
    fun cargarCronograma() {
        cargarItinerario()
        viewModelScope.launch {
            val id = rallyWebId() ?: return@launch
            try {
                _anexos.value = api.get("/api/rallies/$id/anexos")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }.orEmpty()
            } catch (e: Exception) { }
            try {
                _detalleRally.value = api.get("/api/rallies/$id")?.takeIf { it.isJsonObject }?.asJsonObject
            } catch (e: Exception) { }
        }
    }

    /** Lee algo de la plataforma (por ejemplo las viñetas de un tramo). null si no se pudo. */
    suspend fun leer(ruta: String): com.google.gson.JsonElement? = try { api.get(ruta) } catch (e: Exception) { null }

    /**
     * Manda un cambio a la plataforma. Si sale bien muestra [exito] (si hay) y vuelve a leer el
     * cronograma; si falla, muestra el motivo en un cartel y también vuelve a leer.
     */
    fun pedir(
        metodo: String, ruta: String, cuerpo: com.google.gson.JsonElement? = null, exito: String? = null,
        recargarCronograma: Boolean = true,
        alTerminar: (ok: Boolean, respuesta: com.google.gson.JsonElement?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            var ok = false
            var resp: com.google.gson.JsonElement? = null
            try {
                resp = when (metodo) {
                    "PUT" -> api.put(ruta, cuerpo ?: JsonObject())
                    "DELETE" -> api.delete(ruta)
                    else -> api.post(ruta, cuerpo ?: JsonObject())
                }
                ok = true
                exito?.let { AvisoSuperior.mostrar(it) }
            } catch (e: Exception) {
                _mensaje.value = motivo(e)
            }
            alTerminar(ok, resp)
            if (recargarCronograma) cargarCronograma()
        }
    }

    fun cargarItinerario() {
        viewModelScope.launch {
            val id = rallyWebId() ?: run { _error.value = "Este rally no está en la plataforma."; return@launch }
            _cargando.value = true
            try {
                _itinerario.value = api.get("/api/rallies/$id/itinerario")?.takeIf { it.isJsonObject }?.asJsonObject
                _error.value = null
            } catch (e: Exception) {
                _error.value = motivo(e)
            } finally {
                _cargando.value = false
            }
        }
    }

    /** Cambia algo del cronograma en la plataforma y vuelve a leer el itinerario. */
    private fun cambiarCronograma(metodo: String, ruta: String, cuerpo: JsonObject, aviso: String?) {
        viewModelScope.launch {
            try {
                if (metodo == "PUT") api.put(ruta, cuerpo) else api.post(ruta, cuerpo)
                aviso?.let { AvisoSuperior.mostrar(it) }
            } catch (e: Exception) {
                AvisoSuperior.mostrar(motivo(e))
            }
            cargarItinerario()
        }
    }

    /** Minutos entre el CH y la largada de la P.E. (para todo el rally). */
    fun minutosLargadaPe(minutos: Int) {
        viewModelScope.launch {
            val id = rallyWebId() ?: return@launch
            cambiarCronograma("PUT", "/api/rallies/$id/itinerario/config", JsonObject().apply { addProperty("minutosLargadaPe", minutos) }, null)
        }
    }

    /** Atraso máximo y notas al pie de un día. */
    fun datosDia(etapaId: Int, atrasoMaximoMin: Int?, notas: String?) {
        cambiarCronograma("PUT", "/api/etapas/$etapaId/itinerario", JsonObject().apply {
            if (atrasoMaximoMin != null) addProperty("atrasoMaximoMin", atrasoMaximoMin) else add("atrasoMaximoMin", com.google.gson.JsonNull.INSTANCE)
            if (notas.isNullOrBlank()) add("notasItinerario", com.google.gson.JsonNull.INSTANCE) else addProperty("notasItinerario", notas.trim().uppercase())
        }, null)
    }

    /** Corre todos los horarios del día (+ demora, − adelanta). */
    fun demorarDia(etapaId: Int, minutos: Int) {
        cambiarCronograma("POST", "/api/etapas/$etapaId/itinerario/demora?minutos=$minutos", JsonObject(),
            if (minutos >= 0) "Día demorado $minutos minutos" else "Día adelantado ${-minutos} minutos")
    }

    // ---------------- Director de carrera ----------------
    private val _reenganches = MutableStateFlow<List<JsonObject>>(emptyList())
    val reenganches: StateFlow<List<JsonObject>> = _reenganches.asStateFlow()

    /** `GET /api/rallies/{id}/reenganches`: las solicitudes de enganche. */
    fun cargarReenganches() {
        viewModelScope.launch {
            val id = rallyWebId() ?: return@launch
            try {
                _reenganches.value = api.get("/api/rallies/$id/reenganches")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }.orEmpty()
            } catch (e: Exception) { }
        }
    }

    private val _mensajes = MutableStateFlow<List<JsonObject>>(emptyList())
    val mensajes: StateFlow<List<JsonObject>> = _mensajes.asStateFlow()

    private val _errorMensajes = MutableStateFlow<String?>(null)
    val errorMensajes: StateFlow<String?> = _errorMensajes.asStateFlow()

    private val _enviando = MutableStateFlow(false)
    val enviando: StateFlow<Boolean> = _enviando.asStateFlow()

    fun cargarMensajes() {
        viewModelScope.launch {
            val id = rallyWebId() ?: return@launch
            try {
                _mensajes.value = api.get("/api/control-carrera/mensajes?rallyId=$id")
                    ?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }.orEmpty()
                _errorMensajes.value = null
            } catch (e: Exception) {
                _errorMensajes.value = motivo(e)
            }
        }
    }

    /** Mensaje oficial a la caravana (o a un tramo). Devuelve por [listo] si salió. */
    fun emitirMensaje(tipo: String, tramoWebId: Int?, texto: String, listo: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val id = rallyWebId() ?: return@launch
            _enviando.value = true
            try {
                val cuerpo = JsonObject().apply {
                    addProperty("rallyId", id)
                    if (tramoWebId != null) addProperty("tramoId", tramoWebId)
                    addProperty("tipo", tipo)
                    addProperty("mensaje", texto.trim())
                }
                api.post("/api/control-carrera/emitir-mensaje", cuerpo)
                AvisoSuperior.mostrar("Mensaje emitido a la caravana")
                listo(true)
                cargarMensajes()
            } catch (e: Exception) {
                AvisoSuperior.mostrar(motivo(e))
                listo(false)
            } finally {
                _enviando.value = false
            }
        }
    }

    fun banderaRoja(tramoWebId: Int?, motivoTexto: String, listo: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val id = rallyWebId() ?: return@launch
            _enviando.value = true
            try {
                val cuerpo = JsonObject().apply {
                    addProperty("rallyId", id)
                    if (tramoWebId != null) addProperty("tramoId", tramoWebId)
                    addProperty("motivo", motivoTexto.trim())
                }
                api.post("/api/control-carrera/bandera-roja", cuerpo)
                AvisoSuperior.mostrar("Bandera roja emitida")
                listo(true)
                cargarMensajes()
            } catch (e: Exception) {
                AvisoSuperior.mostrar(motivo(e))
                listo(false)
            } finally {
                _enviando.value = false
            }
        }
    }
}
