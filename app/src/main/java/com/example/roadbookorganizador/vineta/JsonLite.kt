package com.example.roadbookorganizador.vineta

/**
 * Lector/escritor JSON mínimo en Kotlin puro (sin dependencias de Android), para que el
 * formato de la viñeta se pueda probar con tests de JVM comunes.
 * Objetos = LinkedHashMap<String, Any?> (conserva el orden), arrays = List<Any?>,
 * números = Double, y además String, Boolean y null.
 */
object JsonLite {

    fun parse(texto: String): Any? {
        val p = Lector(texto)
        p.espacios()
        val v = p.valor()
        p.espacios()
        if (p.i != texto.length) throw IllegalArgumentException("JSON con texto de más en la posición ${p.i}")
        return v
    }

    fun write(valor: Any?): String = StringBuilder().also { escribir(it, valor) }.toString()

    private fun escribir(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> escribirTexto(sb, v)
            is Boolean -> sb.append(if (v) "true" else "false")
            is Number -> escribirNumero(sb, v.toDouble())
            is Map<*, *> -> {
                sb.append('{')
                var primero = true
                for ((k, valor) in v) {
                    if (!primero) sb.append(',')
                    primero = false
                    escribirTexto(sb, k.toString())
                    sb.append(':')
                    escribir(sb, valor)
                }
                sb.append('}')
            }
            is List<*> -> {
                sb.append('[')
                v.forEachIndexed { idx, valor ->
                    if (idx > 0) sb.append(',')
                    escribir(sb, valor)
                }
                sb.append(']')
            }
            else -> escribirTexto(sb, v.toString())
        }
    }

    private fun escribirNumero(sb: StringBuilder, d: Double) {
        if (d.isNaN() || d.isInfinite()) { sb.append("0"); return }
        if (d == Math.rint(d) && kotlin.math.abs(d) < 1e15) sb.append(d.toLong().toString())
        else sb.append(d.toString())
    }

    private fun escribirTexto(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c < ' ') sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }

    private class Lector(val s: String) {
        var i = 0

        fun espacios() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun valor(): Any? {
            espacios()
            if (i >= s.length) throw IllegalArgumentException("JSON incompleto")
            return when (s[i]) {
                '{' -> objeto()
                '[' -> lista()
                '"' -> texto()
                't' -> palabra("true", true)
                'f' -> palabra("false", false)
                'n' -> palabra("null", null)
                else -> numero()
            }
        }

        private fun palabra(p: String, v: Any?): Any? {
            if (!s.startsWith(p, i)) throw IllegalArgumentException("JSON inválido en la posición $i")
            i += p.length
            return v
        }

        private fun objeto(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            i++ // {
            espacios()
            if (i < s.length && s[i] == '}') { i++; return m }
            while (true) {
                espacios()
                val k = texto()
                espacios()
                if (i >= s.length || s[i] != ':') throw IllegalArgumentException("Falta ':' en la posición $i")
                i++
                m[k] = valor()
                espacios()
                if (i < s.length && s[i] == ',') { i++; continue }
                if (i < s.length && s[i] == '}') { i++; return m }
                throw IllegalArgumentException("Falta '}' en la posición $i")
            }
        }

        private fun lista(): List<Any?> {
            val l = ArrayList<Any?>()
            i++ // [
            espacios()
            if (i < s.length && s[i] == ']') { i++; return l }
            while (true) {
                l.add(valor())
                espacios()
                if (i < s.length && s[i] == ',') { i++; continue }
                if (i < s.length && s[i] == ']') { i++; return l }
                throw IllegalArgumentException("Falta ']' en la posición $i")
            }
        }

        private fun texto(): String {
            if (s[i] != '"') throw IllegalArgumentException("Se esperaba texto en la posición $i")
            i++
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val e = s[i++]
                        when (e) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                            'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                            else -> throw IllegalArgumentException("Escape inválido en la posición $i")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw IllegalArgumentException("Texto sin cerrar")
        }

        private fun numero(): Double {
            val inicio = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            if (inicio == i) throw IllegalArgumentException("JSON inválido en la posición $i")
            return s.substring(inicio, i).toDouble()
        }
    }
}

// Ayudas de lectura tolerantes (valor ausente o de otro tipo -> valor por defecto)
@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.obj(k: String): Map<String, Any?>? = this[k] as? Map<String, Any?>
internal fun Map<String, Any?>.lista(k: String): List<Any?> = (this[k] as? List<Any?>) ?: emptyList()
internal fun Map<String, Any?>.num(k: String, def: Double): Double = (this[k] as? Number)?.toDouble() ?: def
internal fun Map<String, Any?>.numONull(k: String): Double? = (this[k] as? Number)?.toDouble()
internal fun Map<String, Any?>.txt(k: String, def: String): String = (this[k] as? String) ?: def
internal fun Map<String, Any?>.bool(k: String, def: Boolean): Boolean = (this[k] as? Boolean) ?: def
@Suppress("UNCHECKED_CAST")
internal fun List<Any?>.objetos(): List<Map<String, Any?>> = mapNotNull { it as? Map<String, Any?> }
