package com.example.roadbookorganizador.data.remote

import android.content.Context
import com.example.roadbookorganizador.data.local.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** El servidor contestó 401: el token venció o no vale. Hay que volver a entrar. */
class SesionVencida : IOException("La sesión venció: volvé a entrar con tu usuario y contraseña.")

/** El servidor contestó con un error (código y texto que mandó). */
class ErrorServidor(val codigo: Int, val detalle: String) : IOException("Error del servidor ($codigo): $detalle")

/**
 * Cliente del servidor de la plataforma (ESPECIFICACION_TAQUEO punto 0).
 * - La dirección sale del ajuste editable (SessionManager.serverUrl).
 * - Cada pedido lleva `Authorization: Bearer <token>`.
 * - Si el servidor contesta 401, se cierra la sesión (la app vuelve a pedir usuario y clave).
 */
class ApiCliente(context: Context) {

    private val sesion = SessionManager(context)
    private val gson = Gson()

    private val base: String get() = sesion.serverUrl.value.trimEnd('/')

    /** `GET /api/health`: true si el servidor contesta. */
    suspend fun enLinea(direccion: String = base): Boolean = revisar(direccion) == null

    /**
     * Revisa el servidor. Devuelve null si contesta, o el motivo en castellano si no
     * (para mostrarlo en el login y saber qué arreglar).
     */
    suspend fun revisar(direccion: String = base): String? = withContext(Dispatchers.IO) {
        try {
            val c = (URL(direccion.trimEnd('/') + "/api/health").openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 8000
            }
            val codigo = c.responseCode
            c.disconnect()
            if (codigo in 200..299) null else "El servidor contestó con error $codigo"
        } catch (e: java.net.ConnectException) {
            "No responde: ¿está arrancado el servidor en la PC? (${e.message ?: "conexión rechazada"})"
        } catch (e: java.net.SocketTimeoutException) {
            "No contesta a tiempo: revisá la dirección, el Wi-Fi o el firewall de la PC"
        } catch (e: java.net.UnknownHostException) {
            "No se encuentra esa dirección: revisá cómo está escrita (y que haya internet)"
        } catch (e: java.net.MalformedURLException) {
            "La dirección está mal escrita: tiene que empezar con http:// o https://"
        } catch (e: Exception) {
            "${e.javaClass.simpleName}: ${e.message ?: ""}"
        }
    }

    /** Respuesta del login: `{ token, expiresInHours, usuario: { id, username, nombreCompleto, rol, email } }`. */
    suspend fun login(username: String, password: String): JsonObject {
        val cuerpo = JsonObject().apply { addProperty("username", username); addProperty("password", password) }
        val r = pedir("POST", "/api/auth/login", cuerpo, conToken = false)
        return r?.asJsonObject ?: throw ErrorServidor(0, "respuesta vacía")
    }

    suspend fun get(ruta: String): JsonElement? = pedir("GET", ruta, null)
    suspend fun put(ruta: String, cuerpo: JsonElement): JsonElement? = pedir("PUT", ruta, cuerpo)
    suspend fun post(ruta: String, cuerpo: JsonElement): JsonElement? = pedir("POST", ruta, cuerpo)
    suspend fun delete(ruta: String): JsonElement? = pedir("DELETE", ruta, null)

    /**
     * Baja un archivo (por ejemplo la portada o la chapa del rally). Las direcciones que empiezan
     * con "/" son del servidor de la plataforma (llevan el token); las de afuera van sin token.
     * Devuelve null si no existe (404).
     */
    suspend fun bajarArchivo(url: String): ByteArray? = withContext(Dispatchers.IO) {
        val propia = url.startsWith("/")
        val c = (URL(if (propia) base + url else url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            if (propia) {
                val t = sesion.sesion.value.token
                if (t.isNotBlank()) setRequestProperty("Authorization", "Bearer $t")
            }
        }
        try {
            val codigo = c.responseCode
            when {
                codigo == 404 -> null
                codigo !in 200..299 -> throw ErrorServidor(codigo, "no se pudo bajar el archivo")
                else -> c.inputStream.use { it.readBytes() }
            }
        } finally {
            c.disconnect()
        }
    }

    private suspend fun pedir(metodo: String, ruta: String, cuerpo: JsonElement?, conToken: Boolean = true): JsonElement? =
        withContext(Dispatchers.IO) {
            val c = (URL(base + ruta).openConnection() as HttpURLConnection).apply {
                requestMethod = metodo
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("Accept", "application/json")
                if (conToken) {
                    val t = sesion.sesion.value.token
                    if (t.isNotBlank()) setRequestProperty("Authorization", "Bearer $t")
                }
                if (cuerpo != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            try {
                if (cuerpo != null) c.outputStream.use { it.write(gson.toJson(cuerpo).toByteArray(Charsets.UTF_8)) }
                val codigo = c.responseCode
                if (codigo == 401) {
                    if (conToken) sesion.cerrarSesion()
                    throw if (conToken) SesionVencida() else ErrorServidor(401, "Usuario o contraseña incorrectos")
                }
                val texto = (if (codigo in 200..299) c.inputStream else c.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (codigo !in 200..299) throw ErrorServidor(codigo, texto.take(300))
                if (texto.isBlank()) null else JsonParser.parseString(texto)
            } finally {
                c.disconnect()
            }
        }
}
