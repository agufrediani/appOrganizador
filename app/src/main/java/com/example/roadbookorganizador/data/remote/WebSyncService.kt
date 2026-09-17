package com.example.roadbookorganizador.data.remote

import android.util.Log
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SyncResult(
    val success: Boolean,
    val message: String,
    val tramosDescargados: Int = 0,
    val tramosSubidos: Int = 0,
    val indicacionesSubidas: Int = 0
)

class WebSyncService(private val repository: RoadbookRepository) {

    private val TAG = "WebSyncService"

    suspend fun sincronizarConServidor(baseUrl: String): SyncResult = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        var tramosDescargados = 0
        var tramosSubidos = 0
        var totalIndicacionesSubidas = 0

        try {
            Log.d(TAG, "Iniciando sincronización IDA Y VUELTA con: $cleanUrl")

            // ==========================================
            // PASO 1: DESCARGA (Web -> Tablet)
            // ==========================================
            val endpoint = URL("$cleanUrl/api/organizador/datos_completos")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 9000
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext SyncResult(
                    success = false,
                    message = "El servidor respondió HTTP $responseCode. Verifica que rally_web esté iniciado en tu PC."
                )
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            conn.disconnect()

            val jsonResponse = JSONObject(sb.toString())
            if (jsonResponse.optString("status") != "ok") {
                return@withContext SyncResult(
                    success = false,
                    message = "Respuesta no válida del servidor: ${jsonResponse.optString("message", "Error desconocido")}"
                )
            }

            // Sincronizar todos los Rallies disponibles desde la web
            val todosRalliesArray = jsonResponse.optJSONArray("todos_rallies") ?: JSONArray()
            for (i in 0 until todosRalliesArray.length()) {
                val rObj = todosRalliesArray.getJSONObject(i)
                val webId = rObj.getInt("id")
                val nombre = rObj.optString("nombre", "Rally Web #$webId")
                val sede = rObj.optString("localidad", "")
                val club = rObj.optString("campeonato", "Frediani Competición")

                val existente = repository.getRallyByWebId(webId)
                if (existente == null) {
                    repository.insertRally(
                        RallyEntity(
                            webId = webId,
                            nombre = nombre,
                            organizadorClub = club,
                            sede = sede,
                            esActivo = false,
                            sincronizado = true
                        )
                    )
                }
            }

            // Sincronizar el Rally activo y sus tramos asignados
            var localActiveRallyId: Long? = null
            val rallyObj = jsonResponse.optJSONObject("rally")
            if (rallyObj != null) {
                val activeWebId = rallyObj.getInt("id")
                val activeNombre = rallyObj.optString("nombre", "Rally Principal")
                val activeSede = rallyObj.optString("localidad", "")
                val activeClub = rallyObj.optString("campeonato", "Frediani Competición")
                val activeFecha = rallyObj.optString("fecha", "")

                var localRally = repository.getRallyByWebId(activeWebId)
                localActiveRallyId = if (localRally == null) {
                    repository.insertRally(
                        RallyEntity(
                            webId = activeWebId,
                            nombre = activeNombre,
                            organizadorClub = activeClub,
                            sede = activeSede,
                            fecha = activeFecha,
                            esActivo = true,
                            sincronizado = true
                        )
                    )
                } else {
                    repository.setRallyActivo(localRally.id)
                    localRally.id
                }

                // Descargar / actualizar los Tramos de este Rally
                val tramosArray = jsonResponse.optJSONArray("tramos") ?: JSONArray()
                for (j in 0 until tramosArray.length()) {
                    val tObj = tramosArray.getJSONObject(j)
                    val tramoWebId = tObj.getInt("id")
                    val tipo = tObj.optString("tipo", "PE")
                    val identificador = tObj.optString("identificador", "PE ${j + 1}")
                    val nombre = tObj.optString("nombre", "Tramo ${j + 1}")
                    val numeroSector = tObj.optInt("numero_sector", j + 1)
                    val chInicio = tObj.optString("ch_inicio", "CH")
                    val chFin = tObj.optString("ch_fin", "CH")
                    val distanciaEstimada = tObj.optDouble("distancia_total", 0.0)

                    val tramoExistente = repository.getTramoByRallyAndWebId(localActiveRallyId, tramoWebId)
                    if (tramoExistente == null) {
                        repository.insertTramo(
                            TramoEntity(
                                rallyId = localActiveRallyId,
                                webId = tramoWebId,
                                tipo = tipo,
                                identificador = identificador,
                                nombre = nombre,
                                numeroSector = numeroSector,
                                chInicio = chInicio,
                                chFin = chFin,
                                distanciaTotalEstimada = distanciaEstimada,
                                estadoTrazado = "BORRADOR",
                                ordenSecuencia = j + 1
                            )
                        )
                        tramosDescargados++
                    }
                }
            }

            // ==========================================
            // PASO 2: SUBIDA (Tablet -> Web)
            // ==========================================
            if (localActiveRallyId != null) {
                val tramosLocales = repository.getTramosByRally(localActiveRallyId).first()

                for (tramo in tramosLocales) {
                    val indicaciones = repository.getVinetasByTramo(tramo.id).first()

                    // Si el tramo es nuevo creado en la tablet (sin webId), primero registrarlo en la web
                    var tramoWebId = tramo.webId
                    if (tramoWebId == null) {
                        tramoWebId = registrarTramoEnWeb(cleanUrl, localActiveRallyId, tramo)
                        if (tramoWebId != null) {
                            repository.updateTramo(tramo.copy(webId = tramoWebId))
                        }
                    }

                    // Si tiene webId y tiene indicaciones trazadas, subirlas con POST a /sincronizar_indicaciones
                    if (tramoWebId != null && indicaciones.isNotEmpty()) {
                        val subidaOk = subirIndicacionesTramo(cleanUrl, tramoWebId, indicaciones)
                        if (subidaOk) {
                            repository.updateDistanciaYEstado(tramo.id, tramo.distanciaMedidaReal, "SINCRONIZADO")
                            tramosSubidos++
                            totalIndicacionesSubidas += indicaciones.size
                        }
                    }
                }
            }

            val mensajeFinal = StringBuilder("¡Sincronización IDA Y VUELTA exitosa!\n")
            if (tramosDescargados > 0) mensajeFinal.append("• $tramosDescargados tramos recibidos de la web\n")
            if (tramosSubidos > 0) mensajeFinal.append("• $tramosSubidos tramos enviados a la web ($totalIndicacionesSubidas indicaciones trazadas)\n")
            if (tramosDescargados == 0 && tramosSubidos == 0) mensajeFinal.append("Todos los tramos e indicaciones están al día.")

            SyncResult(
                success = true,
                message = mensajeFinal.toString().trim(),
                tramosDescargados = tramosDescargados,
                tramosSubidos = tramosSubidos,
                indicacionesSubidas = totalIndicacionesSubidas
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error de sincronización bidireccional", e)
            SyncResult(
                success = false,
                message = "No se pudo conectar a $cleanUrl. Asegurate de que la PC tenga el servidor iniciado (INICIAR_SERVIDOR.bat) y la tablet esté en el mismo Wi-Fi."
            )
        }
    }

    private fun subirIndicacionesTramo(
        baseUrl: String,
        tramoWebId: Int,
        indicaciones: List<com.example.roadbookorganizador.data.local.entity.VinetaEntity>
    ): Boolean {
        return try {
            val url = URL("$baseUrl/api/tramos/$tramoWebId/sincronizar_indicaciones")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val rootJson = JSONObject()
            val indArray = JSONArray()

            for (ind in indicaciones) {
                val item = JSONObject()
                item.put("kmTotal", ind.distanciaTotal)
                item.put("kmParcial", ind.distanciaParcial)
                item.put("tulipa", ind.tulipTipo)
                item.put("nota", ind.informacion)
                item.put("peligro", ind.peligro)
                item.put("lat", ind.latitud)
                item.put("lng", ind.longitud)
                item.put("dibujo", null)
                indArray.put(item)
            }
            rootJson.put("indicaciones", indArray)

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(rootJson.toString())
            writer.flush()
            writer.close()

            val code = conn.responseCode
            conn.disconnect()
            code == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e(TAG, "Error subiendo indicaciones de tramo $tramoWebId", e)
            false
        }
    }

    private fun registrarTramoEnWeb(
        baseUrl: String,
        localRallyId: Long,
        tramo: TramoEntity
    ): Int? {
        return try {
            val rallyLocal = repository.getAllRallies()
            val rally = kotlinx.coroutines.runBlocking { repository.getRallyById(localRallyId) }
            val rallyWebId = rally?.webId ?: 1

            val url = URL("$baseUrl/api/tramos/crear_rapido")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "application/json")
            }

            val postData = StringBuilder()
            postData.append("rally_id=").append(rallyWebId)
            postData.append("&identificador=").append(URLEncoder.encode(tramo.identificador, "UTF-8"))
            postData.append("&nombre=").append(URLEncoder.encode(tramo.nombre, "UTF-8"))
            postData.append("&tipo=").append(URLEncoder.encode(tramo.tipo, "UTF-8"))
            postData.append("&distancia_total=").append(tramo.distanciaMedidaReal)

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(postData.toString())
            writer.flush()
            writer.close()

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                conn.disconnect()

                val json = JSONObject(sb.toString())
                if (json.optString("status") == "ok") {
                    val tObj = json.optJSONObject("tramo")
                    return tObj?.optInt("id")
                }
            }
            conn.disconnect()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error creando tramo en la web", e)
            null
        }
    }
}
