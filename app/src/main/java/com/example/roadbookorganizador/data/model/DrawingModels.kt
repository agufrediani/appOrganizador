package com.example.roadbookorganizador.data.model

import org.json.JSONArray
import org.json.JSONObject

data class PointData(val x: Float, val y: Float)

data class StrokeData(
    val points: List<PointData>,
    val colorHex: String = "#000000",
    val strokeWidth: Float = 6f
)

data class StampData(
    val type: String,
    val x: Float, // 0.0 to 1.0 normalized
    val y: Float, // 0.0 to 1.0 normalized
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f
)

data class ManeuverArrow(
    val p0: PointData = PointData(0.5f, 0.88f), // Inicio (círculo de entrada)
    val p1: PointData = PointData(0.5f, 0.60f), // Control Bézier 1
    val p2: PointData = PointData(0.5f, 0.35f), // Control Bézier 2
    val p3: PointData = PointData(0.5f, 0.12f), // Fin (punta de flecha de salida)
    val strokeWidth: Float = 10f,
    val colorHex: String = "#0284C7", // Azul reglamentario FIA Art. 5.6.1
    val hasPin: Boolean = true, // Pin de kilometraje FIA Art. 5.6.2
    val pinT: Float = 0.5f, // Posición 0.0..1.0 sobre la curva Bézier
    val pinSide: Float = 1.0f // +1 = derecha, -1 = izquierda
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("x0", p0.x.toDouble())
        obj.put("y0", p0.y.toDouble())
        obj.put("x1", p1.x.toDouble())
        obj.put("y1", p1.y.toDouble())
        obj.put("x2", p2.x.toDouble())
        obj.put("y2", p2.y.toDouble())
        obj.put("x3", p3.x.toDouble())
        obj.put("y3", p3.y.toDouble())
        obj.put("width", strokeWidth.toDouble())
        obj.put("color", colorHex)
        obj.put("hasPin", hasPin)
        obj.put("pinT", pinT.toDouble())
        obj.put("pinSide", pinSide.toDouble())
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject?): ManeuverArrow? {
            if (obj == null) return null
            return try {
                ManeuverArrow(
                    p0 = PointData(obj.optDouble("x0", 0.5).toFloat(), obj.optDouble("y0", 0.88).toFloat()),
                    p1 = PointData(obj.optDouble("x1", 0.5).toFloat(), obj.optDouble("y1", 0.60).toFloat()),
                    p2 = PointData(obj.optDouble("x2", 0.5).toFloat(), obj.optDouble("y2", 0.35).toFloat()),
                    p3 = PointData(obj.optDouble("x3", 0.5).toFloat(), obj.optDouble("y3", 0.12).toFloat()),
                    strokeWidth = obj.optDouble("width", 10.0).toFloat(),
                    colorHex = obj.optString("color", "#0284C7"),
                    hasPin = obj.optBoolean("hasPin", true),
                    pinT = obj.optDouble("pinT", 0.5).toFloat(),
                    pinSide = obj.optDouble("pinSide", 1.0).toFloat()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class DrawingData(
    val strokes: List<StrokeData> = emptyList(),
    val stamps: List<StampData> = emptyList(),
    val arrows: List<ManeuverArrow> = emptyList()
) {
    fun toJson(): String {
        val root = JSONObject()
        val strokesArr = JSONArray()
        for (stroke in strokes) {
            val sObj = JSONObject()
            sObj.put("color", stroke.colorHex)
            sObj.put("width", stroke.strokeWidth.toDouble())
            val ptsArr = JSONArray()
            for (pt in stroke.points) {
                val pObj = JSONObject()
                pObj.put("x", pt.x.toDouble())
                pObj.put("y", pt.y.toDouble())
                ptsArr.put(pObj)
            }
            sObj.put("pts", ptsArr)
            strokesArr.put(sObj)
        }
        root.put("strokes", strokesArr)

        val stampsArr = JSONArray()
        for (stamp in stamps) {
            val stObj = JSONObject()
            stObj.put("type", stamp.type)
            stObj.put("x", stamp.x.toDouble())
            stObj.put("y", stamp.y.toDouble())
            stObj.put("scale", stamp.scale.toDouble())
            stObj.put("rot", stamp.rotation.toDouble())
            stampsArr.put(stObj)
        }
        root.put("stamps", stampsArr)

        val arrowsArr = JSONArray()
        for (arrow in arrows) {
            arrowsArr.put(arrow.toJson())
        }
        root.put("arrows", arrowsArr)

        return root.toString()
    }

    fun isEmpty(): Boolean = strokes.isEmpty() && stamps.isEmpty() && arrows.isEmpty()

    companion object {
        fun fromJson(jsonStr: String?): DrawingData {
            if (jsonStr.isNullOrBlank()) return DrawingData()
            return try {
                val root = JSONObject(jsonStr)
                val strokesList = mutableListOf<StrokeData>()
                val strokesArr = root.optJSONArray("strokes")
                if (strokesArr != null) {
                    for (i in 0 until strokesArr.length()) {
                        val sObj = strokesArr.getJSONObject(i)
                        val color = sObj.optString("color", "#000000")
                        val width = sObj.optDouble("width", 6.0).toFloat()
                        val ptsArr = sObj.optJSONArray("pts") ?: JSONArray()
                        val ptsList = mutableListOf<PointData>()
                        for (j in 0 until ptsArr.length()) {
                            val pObj = ptsArr.getJSONObject(j)
                            ptsList.add(PointData(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                        }
                        if (ptsList.isNotEmpty()) {
                            strokesList.add(StrokeData(ptsList, color, width))
                        }
                    }
                }

                val stampsList = mutableListOf<StampData>()
                val stampsArr = root.optJSONArray("stamps")
                if (stampsArr != null) {
                    for (i in 0 until stampsArr.length()) {
                        val stObj = stampsArr.getJSONObject(i)
                        stampsList.add(
                            StampData(
                                type = stObj.optString("type", "RECTA"),
                                x = stObj.optDouble("x", 0.5).toFloat(),
                                y = stObj.optDouble("y", 0.5).toFloat(),
                                scale = stObj.optDouble("scale", 1.0).toFloat(),
                                rotation = stObj.optDouble("rot", 0.0).toFloat()
                            )
                        )
                    }
                }

                val arrowsList = mutableListOf<ManeuverArrow>()
                val arrowsArr = root.optJSONArray("arrows")
                if (arrowsArr != null) {
                    for (i in 0 until arrowsArr.length()) {
                        val aObj = arrowsArr.optJSONObject(i)
                        val a = ManeuverArrow.fromJson(aObj)
                        if (a != null) arrowsList.add(a)
                    }
                }

                DrawingData(strokesList, stampsList, arrowsList)
            } catch (e: Exception) {
                DrawingData()
            }
        }
    }
}
