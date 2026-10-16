package com.example.roadbookorganizador.vineta

/**
 * La biblioteca oficial de la especificación: símbolos (simbolos.json), flechas armadas
 * (flechas_armadas.json) y equivalencias del formato viejo de la app (equivalencias_app.json).
 * Se construye a partir del texto de esos archivos, sin depender de Android.
 */
data class Simbolo(
    val codigo: String,
    val nombre: String,
    val categoria: String,
    val color: String,
    val velocidadKmh: Double?,
    val svg: String
) {
    /** Operaciones de dibujo, calculadas una sola vez. */
    val ops: List<OpSvg> by lazy { SvgSimple.parse(svg) }
}

data class Categoria(val codigo: String, val nombre: String)

data class FlechaArmada(val codigo: String, val nombre: String, val flecha: Flecha)

/** Resultado de convertir un código viejo de la app (punto 9). */
sealed class Equivalencia {
    data class Simbolo(val codigoOficial: String) : Equivalencia()
    data class FlechaArmadaEq(val codigoArmada: String) : Equivalencia()
    object PalitoGps : Equivalencia()
    data class Nuevo(val tipoMientrasTanto: String) : Equivalencia()
    object Vacio : Equivalencia()
}

class Biblioteca(
    val categorias: List<Categoria>,
    val simbolos: Map<String, Simbolo>,
    val flechasArmadas: Map<String, FlechaArmada>,
    private val equivalencias: Map<String, Equivalencia>,
    private val reglasTulipTipo: Map<String, Equivalencia>
) {
    fun simbolo(codigo: String): Simbolo? = simbolos[codigo]

    /** Símbolos oficiales en el orden de simbolos.json (para la barra del editor). */
    val listaSimbolos: List<Simbolo> get() = simbolos.values.toList()

    /** Conversión de un código de símbolo viejo. Cualquier código desconocido → "APP:<código>". */
    fun convertirCodigoApp(codigo: String): Equivalencia =
        equivalencias[codigo] ?: Equivalencia.Nuevo("APP:$codigo")

    /** Conversión de tulipTipo sin dibujo: primero sus reglas propias, después las equivalencias. */
    fun convertirTulipTipo(codigo: String): Equivalencia =
        reglasTulipTipo[codigo] ?: convertirCodigoApp(codigo)

    companion object {
        fun desdeJson(simbolosJson: String, flechasJson: String, equivalenciasJson: String): Biblioteca {
            @Suppress("UNCHECKED_CAST")
            val s = JsonLite.parse(simbolosJson) as Map<String, Any?>
            val categorias = s.lista("categorias").objetos().map { Categoria(it.txt("codigo", ""), it.txt("nombre", "")) }
            val simbolos = LinkedHashMap<String, Simbolo>()
            for (o in s.lista("simbolos").objetos()) {
                val sim = Simbolo(
                    codigo = o.txt("codigo", ""),
                    nombre = o.txt("nombre", ""),
                    categoria = o.txt("categoria", ""),
                    color = o.txt("color", "#000000"),
                    velocidadKmh = o.numONull("velocidadKmh"),
                    svg = o.txt("svg", "")
                )
                simbolos[sim.codigo] = sim
            }

            @Suppress("UNCHECKED_CAST")
            val f = JsonLite.parse(flechasJson) as Map<String, Any?>
            val armadas = LinkedHashMap<String, FlechaArmada>()
            for (o in f.lista("flechas").objetos()) {
                val codigo = o.txt("codigo", "")
                val flecha = o.obj("flecha") ?: continue
                armadas[codigo] = FlechaArmada(codigo, o.txt("nombre", codigo), DibujoV1.leerFlecha(flecha))
            }

            @Suppress("UNCHECKED_CAST")
            val e = JsonLite.parse(equivalenciasJson) as Map<String, Any?>
            val eq = HashMap<String, Equivalencia>()
            for (o in e.lista("equivalencias").objetos()) {
                val codigos = o.lista("codigos_app").filterIsInstance<String>()
                val r = leerResultado(o, codigos.firstOrNull() ?: "")
                for (c in codigos) eq[c] = if (r is Equivalencia.Nuevo && o["tipo_mientras_tanto"] == null) Equivalencia.Nuevo("APP:$c") else r
            }
            val reglas = HashMap<String, Equivalencia>()
            for (o in e.obj("tulipTipo_sin_dibujo")?.lista("reglas")?.objetos() ?: emptyList()) {
                for (c in o.lista("tulipTipo").filterIsInstance<String>()) {
                    val r = leerResultado(o, c)
                    reglas[c] = if (r is Equivalencia.Nuevo && o["tipo_mientras_tanto"] == null) Equivalencia.Nuevo("APP:$c") else r
                }
            }
            return Biblioteca(categorias, simbolos, armadas, eq, reglas)
        }

        private fun leerResultado(o: Map<String, Any?>, codigo: String): Equivalencia =
            when (o.txt("resultado", "")) {
                "igual", "simbolo" -> Equivalencia.Simbolo(o.txt("codigo_oficial", "APP:$codigo"))
                "flecha" -> Equivalencia.FlechaArmadaEq(o.txt("flecha_armada", "recta"))
                "gps" -> Equivalencia.PalitoGps
                "vacio" -> Equivalencia.Vacio
                else -> Equivalencia.Nuevo(o.txt("tipo_mientras_tanto", "APP:$codigo"))
            }
    }
}
