package com.example.roadbookorganizador.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.roadbookorganizador.data.model.DrawingData
import com.example.roadbookorganizador.data.model.ManeuverArrow
import com.example.roadbookorganizador.data.model.StampData
import com.example.roadbookorganizador.data.model.StrokeData

object DiagramaCanvasRenderer {

    val TulipaCanvasRenderer get() = this

    fun render(
        drawScope: DrawScope,
        drawingData: DrawingData,
        fallbackTulip: String,
        primaryColor: Color = Color.Black,
        selectedArrowIdx: Int? = null
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        if (drawingData.isEmpty()) {
            if (fallbackTulip.isNotBlank() && 
                !fallbackTulip.equals("BLANCO", ignoreCase = true) && 
                !fallbackTulip.equals("VACIO", ignoreCase = true) && 
                !fallbackTulip.equals("NINGUNO", ignoreCase = true) &&
                !fallbackTulip.equals("RECTA", ignoreCase = true)) {
                renderDefaultTulip(drawScope, fallbackTulip, primaryColor)
            }
            return
        }

        // 1. Renderizar flechas de maniobra curvables Bézier (estándar oficial FIA)
        for (i in drawingData.arrows.indices) {
            val arrow = drawingData.arrows[i]
            renderManeuverArrow(drawScope, arrow, isSelected = (i == selectedArrowIdx), primaryColor)
        }

        // 2. Renderizar sellos colocados
        for (stamp in drawingData.stamps) {
            renderStamp(drawScope, stamp, primaryColor)
        }

        // 2. Renderizar trazos a mano alzada
        for (stroke in drawingData.strokes) {
            if (stroke.points.size < 2) continue
            val strokeColor = try {
                Color(android.graphics.Color.parseColor(stroke.colorHex))
            } catch (e: Exception) {
                primaryColor
            }

            val path = Path()
            val first = stroke.points[0]
            path.moveTo(first.x * width, first.y * height)

            for (i in 1 until stroke.points.size) {
                val pt = stroke.points[i]
                path.lineTo(pt.x * width, pt.y * height)
            }

            drawScope.drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }

    fun renderManeuverArrow(
        drawScope: DrawScope,
        arrow: ManeuverArrow,
        isSelected: Boolean = false,
        primaryColor: Color = Color.Black
    ) {
        val w = drawScope.size.width
        val h = drawScope.size.height
        val arrowColor = try { Color(android.graphics.Color.parseColor(arrow.colorHex)) } catch (e: Exception) { primaryColor }

        val pt0 = Offset(arrow.p0.x * w, arrow.p0.y * h)
        val pt1 = Offset(arrow.p1.x * w, arrow.p1.y * h)
        val pt2 = Offset(arrow.p2.x * w, arrow.p2.y * h)
        val pt3 = Offset(arrow.p3.x * w, arrow.p3.y * h)

        val effectiveStroke = (arrow.strokeWidth * 1.25f).coerceAtLeast(8.5f)

        // 1. Bola / círculo de entrada de la maniobra (estándar oficial FIA / Rally)
        drawScope.drawCircle(
            color = arrowColor,
            radius = (effectiveStroke * 0.85f).coerceAtLeast(10f),
            center = pt0
        )

        // 2. Trazo de curva Bézier suave
        val path = Path().apply {
            moveTo(pt0.x, pt0.y)
            cubicTo(pt1.x, pt1.y, pt2.x, pt2.y, pt3.x, pt3.y)
        }
        drawScope.drawPath(
            path = path,
            color = arrowColor,
            style = Stroke(
                width = effectiveStroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 3. Punta de flecha oficial FIA en pt3 con ángulo tangente
        val dx = if (pt3 != pt2) pt3.x - pt2.x else pt3.x - pt1.x
        val dy = if (pt3 != pt2) pt3.y - pt2.y else pt3.y - pt1.y
        val angle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()

        val headLength = (effectiveStroke * 2.3f).coerceAtLeast(25f)
        val headAngle = 0.55f // ~31 grados

        val pLeft = Offset(
            (pt3.x - headLength * Math.cos((angle - headAngle).toDouble())).toFloat(),
            (pt3.y - headLength * Math.sin((angle - headAngle).toDouble())).toFloat()
        )
        val pRight = Offset(
            (pt3.x - headLength * Math.cos((angle + headAngle).toDouble())).toFloat(),
            (pt3.y - headLength * Math.sin((angle + headAngle).toDouble())).toFloat()
        )

        val headPath = Path().apply {
            moveTo(pt3.x, pt3.y)
            lineTo(pLeft.x, pLeft.y)
            lineTo(pRight.x, pRight.y)
            close()
        }
        drawScope.drawPath(path = headPath, color = arrowColor)

        // 4. Si está seleccionada, dibujar guías tangentes y 4 tiradores de control
        if (isSelected) {
            // Línea tangente P0 -> P1
            drawScope.drawLine(
                color = Color(0xFF0284C7).copy(alpha = 0.6f),
                start = pt0,
                end = pt1,
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
            )
            // Línea tangente P3 -> P2
            drawScope.drawLine(
                color = Color(0xFF0284C7).copy(alpha = 0.6f),
                start = pt3,
                end = pt2,
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
            )

            // P0: Tirador de Entrada (Verde con punto interior)
            drawScope.drawCircle(color = Color.White, radius = 10f, center = pt0)
            drawScope.drawCircle(color = Color(0xFF10B981), radius = 10f, center = pt0, style = Stroke(3f))
            drawScope.drawCircle(color = Color(0xFF10B981), radius = 4f, center = pt0)

            // P1: Tirador de Curvatura 1 (Cyan)
            drawScope.drawCircle(color = Color.White, radius = 9f, center = pt1)
            drawScope.drawCircle(color = Color(0xFF0284C7), radius = 9f, center = pt1, style = Stroke(3f))
            drawScope.drawCircle(color = Color(0xFF0284C7), radius = 4f, center = pt1)

            // P2: Tirador de Curvatura 2 (Cyan)
            drawScope.drawCircle(color = Color.White, radius = 9f, center = pt2)
            drawScope.drawCircle(color = Color(0xFF0284C7), radius = 9f, center = pt2, style = Stroke(3f))
            drawScope.drawCircle(color = Color(0xFF0284C7), radius = 4f, center = pt2)

            // P3: Tirador de Salida / Punta (Rojo)
            drawScope.drawCircle(color = Color.White, radius = 11f, center = pt3)
            drawScope.drawCircle(color = Color(0xFFEF4444), radius = 11f, center = pt3, style = Stroke(3f))
            drawScope.drawCircle(color = Color(0xFFEF4444), radius = 5f, center = pt3)
        }
    }

    private fun renderStamp(
        drawScope: DrawScope,
        stamp: StampData,
        primaryColor: Color
    ) {
        val w = drawScope.size.width
        val h = drawScope.size.height
        val cx = stamp.x * w
        val cy = stamp.y * h
        val scale = stamp.scale * 1.25f // 25% más grande

        drawScope.withTransform({
            translate(left = cx, top = cy)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            rotate(degrees = stamp.rotation, pivot = Offset.Zero)
        }) {
            drawStampShape(this, stamp.type, primaryColor)
        }
    }

    fun renderStampPreview(
        drawScope: DrawScope,
        type: String,
        primaryColor: Color = Color.Black
    ) {
        val w = drawScope.size.width
        val h = drawScope.size.height
        val cx = w / 2f
        val cy = h / 2f
        val scale = (minOf(w, h) / 70f).coerceIn(0.4f, 1.2f)

        drawScope.withTransform({
            translate(left = cx, top = cy)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            drawStampShape(this, type, primaryColor)
        }
    }

    fun drawStampShape(drawScope: DrawScope, type: String, color: Color) {
        val stroke = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (type.uppercase()) {
            // === MANIOBRAS OFICIALES Y FLECHAS ===
            "FLECHA_MANIOBRA", "FLECHA_CURVA", "MANIOBRA_ARROW" -> {
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(0f, 26f))
                val p = Path().apply {
                    moveTo(0f, 26f)
                    cubicTo(0f, 5f, 15f, -10f, 24f, -22f)
                }
                drawScope.drawPath(path = p, color = color, style = Stroke(7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val head = Path().apply {
                    moveTo(24f, -22f)
                    lineTo(12f, -24f)
                    lineTo(20f, -12f)
                    close()
                }
                drawScope.drawPath(path = head, color = color)
            }
            "RECTA" -> {
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(0f, 30f))
                drawScope.drawLine(color = color, start = Offset(0f, 30f), end = Offset(0f, -30f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(0f, -30f), end = Offset(-10f, -18f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(0f, -30f), end = Offset(10f, -18f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            "CURVA_DER", "CURVA_SUAVE_DER" -> {
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(-10f, 30f))
                val p = Path().apply {
                    moveTo(-10f, 30f)
                    cubicTo(-10f, 10f, 10f, 0f, 25f, -20f)
                }
                drawScope.drawPath(path = p, color = color, style = stroke)
                drawScope.drawLine(color = color, start = Offset(25f, -20f), end = Offset(15f, -25f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(25f, -20f), end = Offset(18f, -10f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            "CURVA_IZQ", "CURVA_SUAVE_IZQ" -> {
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(10f, 30f))
                val p = Path().apply {
                    moveTo(10f, 30f)
                    cubicTo(10f, 10f, -10f, 0f, -25f, -20f)
                }
                drawScope.drawPath(path = p, color = color, style = stroke)
                drawScope.drawLine(color = color, start = Offset(-25f, -20f), end = Offset(-15f, -25f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(-25f, -20f), end = Offset(-18f, -10f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            "DERECHA_2", "DERECHA_3", "DERECHA_90" -> {
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(-20f, 30f))
                val p = Path().apply {
                    moveTo(-20f, 30f)
                    lineTo(-20f, 0f)
                    cubicTo(-20f, -20f, 0f, -20f, 30f, -20f)
                }
                drawScope.drawPath(path = p, color = color, style = stroke)
                drawScope.drawLine(color = color, start = Offset(30f, -20f), end = Offset(18f, -30f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(30f, -20f), end = Offset(18f, -10f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            "IZQUIERDA_2", "IZQUIERDA_3", "IZQUIERDA_90" -> {
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(20f, 30f))
                val p = Path().apply {
                    moveTo(20f, 30f)
                    lineTo(20f, 0f)
                    cubicTo(20f, -20f, 0f, -20f, -30f, -20f)
                }
                drawScope.drawPath(path = p, color = color, style = stroke)
                drawScope.drawLine(color = color, start = Offset(-30f, -20f), end = Offset(-18f, -30f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(-30f, -20f), end = Offset(-18f, -10f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            "RETOME_DER", "HORQUILLA_DER" -> {
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(-15f, 30f))
                val p = Path().apply {
                    moveTo(-15f, 30f)
                    lineTo(-15f, -10f)
                    cubicTo(-15f, -35f, 15f, -35f, 15f, -10f)
                    lineTo(15f, 25f)
                }
                drawScope.drawPath(path = p, color = color, style = stroke)
                drawScope.drawLine(color = color, start = Offset(15f, 25f), end = Offset(7f, 15f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(15f, 25f), end = Offset(23f, 15f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            "RETOME_IZQ", "HORQUILLA_IZQ" -> {
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(15f, 30f))
                val p = Path().apply {
                    moveTo(15f, 30f)
                    lineTo(15f, -10f)
                    cubicTo(15f, -35f, -15f, -35f, -15f, -10f)
                    lineTo(-15f, 25f)
                }
                drawScope.drawPath(path = p, color = color, style = stroke)
                drawScope.drawLine(color = color, start = Offset(-15f, 25f), end = Offset(-7f, 15f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(-15f, 25f), end = Offset(-23f, 15f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            "CRUCE", "CRUCE_RECTO", "CRUCE_X" -> {
                drawScope.drawLine(color = Color.Gray, start = Offset(-30f, 0f), end = Offset(30f, 0f), strokeWidth = 4f)
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(0f, 30f))
                drawScope.drawLine(color = color, start = Offset(0f, 30f), end = Offset(0f, -30f), strokeWidth = 6f)
                drawScope.drawLine(color = color, start = Offset(0f, -30f), end = Offset(-10f, -18f), strokeWidth = 6f)
                drawScope.drawLine(color = color, start = Offset(0f, -30f), end = Offset(10f, -18f), strokeWidth = 6f)
            }
            "CRUCE_T" -> {
                drawScope.drawLine(color = Color.Gray, start = Offset(-30f, -10f), end = Offset(30f, -10f), strokeWidth = 5f)
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(0f, 30f))
                drawScope.drawLine(color = color, start = Offset(0f, 30f), end = Offset(0f, -10f), strokeWidth = 6f)
                drawScope.drawLine(color = color, start = Offset(0f, -10f), end = Offset(25f, -10f), strokeWidth = 6f)
                drawScope.drawLine(color = color, start = Offset(25f, -10f), end = Offset(15f, -18f), strokeWidth = 5f)
                drawScope.drawLine(color = color, start = Offset(25f, -10f), end = Offset(15f, -2f), strokeWidth = 5f)
            }
            "BIFURCACION_Y" -> {
                drawScope.drawCircle(color = color, radius = 7f, center = Offset(0f, 30f))
                drawScope.drawLine(color = color, start = Offset(0f, 30f), end = Offset(0f, 5f), strokeWidth = 6f)
                drawScope.drawLine(color = color, start = Offset(0f, 5f), end = Offset(25f, -25f), strokeWidth = 6f)
                drawScope.drawLine(color = Color.Gray, start = Offset(0f, 5f), end = Offset(-25f, -25f), strokeWidth = 4f)
                drawScope.drawLine(color = color, start = Offset(25f, -25f), end = Offset(12f, -25f), strokeWidth = 5f)
                drawScope.drawLine(color = color, start = Offset(25f, -25f), end = Offset(22f, -12f), strokeWidth = 5f)
            }
            "ROTONDA" -> {
                drawScope.drawCircle(color = color, radius = 18f, center = Offset(0f, -5f), style = Stroke(5f))
                drawScope.drawCircle(color = color, radius = 6f, center = Offset(0f, 30f))
                drawScope.drawLine(color = color, start = Offset(0f, 30f), end = Offset(0f, 13f), strokeWidth = 5f)
                drawScope.drawLine(color = color, start = Offset(18f, -5f), end = Offset(32f, -5f), strokeWidth = 5f)
                drawScope.drawLine(color = color, start = Offset(32f, -5f), end = Offset(24f, -12f), strokeWidth = 5f)
                drawScope.drawLine(color = color, start = Offset(32f, -5f), end = Offset(24f, 2f), strokeWidth = 5f)
            }

            // === CONTROLES OFICIALES FIA / RALLY ===
            "TC", "CONTROL_HORARIO" -> {
                // Reloj de Control Horario FIA (Naranja/Rojo)
                val tcColor = Color(0xFFEA580C)
                drawScope.drawCircle(color = tcColor, radius = 22f, center = Offset(0f, 0f), style = Stroke(5f))
                drawScope.drawCircle(color = tcColor.copy(alpha = 0.15f), radius = 22f, center = Offset(0f, 0f))
                drawScope.drawLine(color = tcColor, start = Offset(0f, 0f), end = Offset(0f, -14f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawScope.drawLine(color = tcColor, start = Offset(0f, 0f), end = Offset(10f, 5f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawScope.drawCircle(color = tcColor, radius = 3.5f, center = Offset(0f, 0f))
            }
            "LARGADA", "START" -> {
                // Bandera verde de largada
                val green = Color(0xFF16A34A)
                drawScope.drawLine(color = Color.Black, start = Offset(-18f, 25f), end = Offset(-18f, -25f), strokeWidth = 4f, cap = StrokeCap.Round)
                val p = Path().apply {
                    moveTo(-18f, -25f)
                    lineTo(18f, -12f)
                    lineTo(-18f, 0f)
                    close()
                }
                drawScope.drawPath(path = p, color = green)
            }
            "LLEGADA", "FINISH" -> {
                // Bandera a cuadros de llegada
                drawScope.drawLine(color = Color.Black, start = Offset(-18f, 25f), end = Offset(-18f, -25f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawScope.drawRect(color = Color.Black, topLeft = Offset(-18f, -25f), size = Size(36f, 24f), style = Stroke(2f))
                drawScope.drawRect(color = Color.Black, topLeft = Offset(-18f, -25f), size = Size(18f, 12f))
                drawScope.drawRect(color = Color.Black, topLeft = Offset(0f, -13f), size = Size(18f, 12f))
            }
            "STOP" -> {
                // Cartel de STOP FIA
                val red = Color(0xFFDC2626)
                val p = Path().apply {
                    val r = 24f
                    for (i in 0 until 8) {
                        val angle = (i * 45.0 + 22.5) * Math.PI / 180.0
                        val x = (r * Math.cos(angle)).toFloat()
                        val y = (r * Math.sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawScope.drawPath(path = p, color = red)
                drawScope.drawLine(color = Color.White, start = Offset(-12f, 0f), end = Offset(12f, 0f), strokeWidth = 5f, cap = StrokeCap.Round)
            }
            "RESET_0000" -> {
                // Caja 0000 Reset Odómetro
                drawScope.drawRoundRect(color = Color.Black, topLeft = Offset(-26f, -14f), size = Size(52f, 28f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(3.5f))
                drawScope.drawLine(color = Color.Black, start = Offset(-16f, -6f), end = Offset(-16f, 6f), strokeWidth = 3f)
                drawScope.drawLine(color = Color.Black, start = Offset(-6f, -6f), end = Offset(-6f, 6f), strokeWidth = 3f)
                drawScope.drawLine(color = Color.Black, start = Offset(6f, -6f), end = Offset(6f, 6f), strokeWidth = 3f)
                drawScope.drawLine(color = Color.Black, start = Offset(16f, -6f), end = Offset(16f, 6f), strokeWidth = 3f)
            }
            "GPS_POINT", "ANTENA_GPS" -> {
                // Antena transmisora / GPS point
                val blue = Color(0xFF0284C7)
                drawScope.drawLine(color = blue, start = Offset(0f, 24f), end = Offset(0f, -10f), strokeWidth = 4f)
                drawScope.drawCircle(color = blue, radius = 5f, center = Offset(0f, -10f))
                drawScope.drawArc(color = blue, startAngle = 210f, sweepAngle = 120f, useCenter = false, topLeft = Offset(-16f, -26f), size = Size(32f, 32f), style = Stroke(3.5f))
                drawScope.drawArc(color = blue, startAngle = 210f, sweepAngle = 120f, useCenter = false, topLeft = Offset(-24f, -34f), size = Size(48f, 48f), style = Stroke(3f))
            }
            "SURTIDOR", "COMBUSTIBLE" -> {
                val fuel = Color(0xFF2563EB)
                drawScope.drawRoundRect(color = fuel, topLeft = Offset(-16f, -20f), size = Size(26f, 38f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f), style = Stroke(3.5f))
                drawScope.drawRect(color = fuel, topLeft = Offset(-11f, -14f), size = Size(16f, 12f))
                drawScope.drawLine(color = fuel, start = Offset(10f, -8f), end = Offset(18f, -8f), strokeWidth = 3f)
                drawScope.drawLine(color = fuel, start = Offset(18f, -8f), end = Offset(18f, 15f), strokeWidth = 3f)
            }
            "ASISTENCIA" -> {
                // Llave de taller
                drawScope.drawLine(color = Color(0xFF475569), start = Offset(-16f, -16f), end = Offset(16f, 16f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawCircle(color = Color(0xFF475569), radius = 10f, center = Offset(-16f, -16f), style = Stroke(4f))
            }
            "MEDICO", "PRIMEROS_AUXILIOS" -> {
                val med = Color(0xFFDC2626)
                drawScope.drawCircle(color = med, radius = 22f, center = Offset(0f, 0f), style = Stroke(4f))
                drawScope.drawLine(color = med, start = Offset(-12f, 0f), end = Offset(12f, 0f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = med, start = Offset(0f, -12f), end = Offset(0f, 12f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            "RADAR_DZ" -> {
                drawScope.drawCircle(color = Color(0xFF0284C7), radius = 20f, center = Offset(0f, 0f), style = Stroke(4f))
                drawScope.drawCircle(color = Color(0xFF0284C7).copy(alpha = 0.15f), radius = 20f, center = Offset(0f, 0f))
                // D
                drawScope.drawLine(color = Color(0xFF0284C7), start = Offset(-10f, -8f), end = Offset(-10f, 8f), strokeWidth = 3.5f)
                drawScope.drawArc(color = Color(0xFF0284C7), startAngle = -90f, sweepAngle = 180f, useCenter = false, topLeft = Offset(-14f, -8f), size = Size(14f, 16f), style = Stroke(3.5f))
                // Z
                val p = Path().apply {
                    moveTo(3f, -8f); lineTo(12f, -8f); lineTo(3f, 8f); lineTo(12f, 8f)
                }
                drawScope.drawPath(path = p, color = Color(0xFF0284C7), style = Stroke(3.5f))
            }
            "RADAR_FZ" -> {
                drawScope.drawCircle(color = Color(0xFF64748B), radius = 20f, center = Offset(0f, 0f), style = Stroke(4f))
                drawScope.drawLine(color = Color(0xFFDC2626), start = Offset(-15f, 15f), end = Offset(15f, -15f), strokeWidth = 4f)
            }

            // === PELIGROS Y SEÑALES ===
            "PELIGRO_1", "PELIGRO_!" -> {
                drawScope.drawLine(color = Color(0xFFDC2626), start = Offset(0f, -22f), end = Offset(0f, 6f), strokeWidth = 7f, cap = StrokeCap.Round)
                drawScope.drawCircle(color = Color(0xFFDC2626), radius = 4.5f, center = Offset(0f, 18f))
            }
            "PELIGRO_2", "PELIGRO_!!" -> {
                for (ox in listOf(-8f, 8f)) {
                    drawScope.drawLine(color = Color(0xFFDC2626), start = Offset(ox, -22f), end = Offset(ox, 6f), strokeWidth = 6f, cap = StrokeCap.Round)
                    drawScope.drawCircle(color = Color(0xFFDC2626), radius = 4f, center = Offset(ox, 18f))
                }
            }
            "PELIGRO_3", "PELIGRO_!!!" -> {
                for (ox in listOf(-14f, 0f, 14f)) {
                    drawScope.drawLine(color = Color(0xFFDC2626), start = Offset(ox, -22f), end = Offset(ox, 6f), strokeWidth = 5.5f, cap = StrokeCap.Round)
                    drawScope.drawCircle(color = Color(0xFFDC2626), radius = 3.5f, center = Offset(ox, 18f))
                }
            }
            "PRECAUCION", "PELIGRO" -> {
                val p = Path().apply {
                    moveTo(0f, -28f); lineTo(26f, 18f); lineTo(-26f, 18f); close()
                }
                drawScope.drawPath(path = p, color = Color(0xFFF59E0B), style = stroke)
                drawScope.drawLine(color = Color(0xFFDC2626), start = Offset(0f, -12f), end = Offset(0f, 5f), strokeWidth = 4.5f, cap = StrokeCap.Round)
                drawScope.drawCircle(color = Color(0xFFDC2626), radius = 3f, center = Offset(0f, 12f))
            }

            // === REFERENCIAS / INFRAESTRUCTURA (LANDMARKS) ===
            "TRANQUERA" -> {
                drawScope.drawRect(color = color, topLeft = Offset(-24f, -14f), size = Size(48f, 28f), style = Stroke(4f))
                drawScope.drawLine(color = color, start = Offset(-24f, -14f), end = Offset(24f, 14f), strokeWidth = 3.5f)
                drawScope.drawLine(color = color, start = Offset(-24f, 14f), end = Offset(24f, -14f), strokeWidth = 3.5f)
            }
            "GUARDAGANADO" -> {
                drawScope.drawLine(color = color, start = Offset(-24f, -14f), end = Offset(-24f, 14f), strokeWidth = 4f)
                drawScope.drawLine(color = color, start = Offset(24f, -14f), end = Offset(24f, 14f), strokeWidth = 4f)
                for (x in listOf(-16f, -8f, 0f, 8f, 16f)) {
                    drawScope.drawLine(color = color, start = Offset(x, -14f), end = Offset(x, 14f), strokeWidth = 3f)
                }
            }
            "PUENTE" -> {
                drawScope.drawLine(color = color, start = Offset(-22f, -22f), end = Offset(-12f, 0f), strokeWidth = 5f)
                drawScope.drawLine(color = color, start = Offset(-12f, 0f), end = Offset(-22f, 22f), strokeWidth = 5f)
                drawScope.drawLine(color = color, start = Offset(22f, -22f), end = Offset(12f, 0f), strokeWidth = 5f)
                drawScope.drawLine(color = color, start = Offset(12f, 0f), end = Offset(22f, 22f), strokeWidth = 5f)
                drawScope.drawLine(color = color, start = Offset(0f, 22f), end = Offset(0f, -22f), strokeWidth = 4f)
            }
            "ALCANTARILLA" -> {
                drawScope.drawCircle(color = color, radius = 16f, center = Offset(0f, 0f), style = Stroke(4f))
                drawScope.drawCircle(color = color, radius = 8f, center = Offset(0f, 0f), style = Stroke(3f))
                drawScope.drawLine(color = color, start = Offset(-24f, 0f), end = Offset(24f, 0f), strokeWidth = 3f)
            }
            "VIAS_TREN" -> {
                drawScope.drawLine(color = color, start = Offset(-24f, -8f), end = Offset(24f, -8f), strokeWidth = 4f)
                drawScope.drawLine(color = color, start = Offset(-24f, 8f), end = Offset(24f, 8f), strokeWidth = 4f)
                for (x in listOf(-18f, -9f, 0f, 9f, 18f)) {
                    drawScope.drawLine(color = color, start = Offset(x, -14f), end = Offset(x, 14f), strokeWidth = 3f)
                }
            }
            "ANTENA" -> {
                val p = Path().apply {
                    moveTo(0f, -26f); lineTo(14f, 24f); lineTo(-14f, 24f); close()
                }
                drawScope.drawPath(path = p, color = color, style = Stroke(3f))
                drawScope.drawLine(color = color, start = Offset(-8f, 8f), end = Offset(8f, 8f), strokeWidth = 3f)
                drawScope.drawLine(color = color, start = Offset(-4f, -6f), end = Offset(4f, -6f), strokeWidth = 3f)
            }
            "MOLINO" -> {
                drawScope.drawLine(color = color, start = Offset(0f, 24f), end = Offset(0f, -6f), strokeWidth = 4f)
                drawScope.drawCircle(color = color, radius = 14f, center = Offset(0f, -6f), style = Stroke(3f))
                drawScope.drawLine(color = color, start = Offset(-12f, -6f), end = Offset(12f, -6f), strokeWidth = 3f)
                drawScope.drawLine(color = color, start = Offset(0f, -18f), end = Offset(0f, 6f), strokeWidth = 3f)
            }
            "CASA", "POBLADO" -> {
                val p = Path().apply {
                    moveTo(-18f, 4f); lineTo(0f, -16f); lineTo(18f, 4f); close()
                }
                drawScope.drawPath(path = p, color = color, style = Stroke(3.5f))
                drawScope.drawRect(color = color, topLeft = Offset(-14f, 4f), size = Size(28f, 18f), style = Stroke(3.5f))
            }
            "IGLESIA" -> {
                drawScope.drawRect(color = color, topLeft = Offset(-14f, -2f), size = Size(28f, 24f), style = Stroke(3.5f))
                drawScope.drawLine(color = color, start = Offset(0f, -2f), end = Offset(0f, -24f), strokeWidth = 4f)
                drawScope.drawLine(color = color, start = Offset(-7f, -16f), end = Offset(7f, -16f), strokeWidth = 4f)
            }

            // === TERRENO Y GEOGRAFÍA ===
            "VADO", "AGUA" -> {
                for (offsetY in listOf(-8f, 8f)) {
                    val p = Path().apply {
                        moveTo(-25f, offsetY)
                        cubicTo(-15f, offsetY - 8f, -5f, offsetY + 8f, 5f, offsetY)
                        cubicTo(15f, offsetY - 8f, 25f, offsetY + 8f, 30f, offsetY)
                    }
                    drawScope.drawPath(path = p, color = Color(0xFF00B4D8), style = stroke)
                }
            }
            "SALTO", "LOMO" -> {
                val p = Path().apply {
                    moveTo(-28f, 6f)
                    cubicTo(-14f, -22f, 14f, -22f, 28f, 6f)
                }
                drawScope.drawPath(path = p, color = color, style = stroke)
                drawScope.drawCircle(color = Color(0xFFDC2626), radius = 5f, center = Offset(0f, -24f))
            }
            "ZANJA" -> {
                val p = Path().apply {
                    moveTo(-26f, -14f)
                    lineTo(-10f, 14f)
                    lineTo(10f, 14f)
                    lineTo(26f, -14f)
                }
                drawScope.drawPath(path = p, color = Color(0xFFB45309), style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            "DUNAS", "ARENA" -> {
                val sand = Color(0xFFD97706)
                for (offsetY in listOf(-6f, 10f)) {
                    val p = Path().apply {
                        moveTo(-24f, offsetY)
                        cubicTo(-12f, offsetY - 14f, 0f, offsetY, 12f, offsetY - 14f)
                        lineTo(24f, offsetY)
                    }
                    drawScope.drawPath(path = p, color = sand, style = Stroke(4f, cap = StrokeCap.Round))
                }
            }
            "PIEDRAS", "ROCAS" -> {
                val rock = Color(0xFF64748B)
                drawScope.drawCircle(color = rock, radius = 6f, center = Offset(-14f, 4f))
                drawScope.drawCircle(color = rock, radius = 9f, center = Offset(4f, -4f))
                drawScope.drawCircle(color = rock, radius = 5f, center = Offset(16f, 8f))
            }
            "BARRO", "HUELLON" -> {
                val mud = Color(0xFF78350F)
                drawScope.drawLine(color = mud, start = Offset(-10f, -22f), end = Offset(-10f, 22f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawScope.drawLine(color = mud, start = Offset(10f, -22f), end = Offset(10f, 22f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawScope.drawCircle(color = mud, radius = 3.5f, center = Offset(-18f, 0f))
                drawScope.drawCircle(color = mud, radius = 3.5f, center = Offset(18f, 0f))
            }
            "CIRCULO", "MOJON" -> {
                drawScope.drawCircle(color = color, radius = 15f, center = Offset(0f, 0f), style = Stroke(4f))
            }
            "FLECHA" -> {
                drawScope.drawLine(color = color, start = Offset(0f, 25f), end = Offset(0f, -25f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(0f, -25f), end = Offset(-10f, -15f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(color = color, start = Offset(0f, -25f), end = Offset(10f, -15f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            else -> {
                // Símbolo no reconocido o blanco: no dibujar flecha automática
            }
        }
    }

    private fun renderDefaultTulip(drawScope: DrawScope, tulip: String, color: Color) {
        if (tulip.isBlank() || 
            tulip.equals("BLANCO", ignoreCase = true) || 
            tulip.equals("VACIO", ignoreCase = true) || 
            tulip.equals("NINGUNO", ignoreCase = true) ||
            tulip.equals("RECTA", ignoreCase = true)) {
            return
        }
        val w = drawScope.size.width
        val h = drawScope.size.height
        val cx = w / 2f
        val cy = h / 2f

        drawScope.withTransform({
            translate(left = cx, top = cy)
            val scale = (w.coerceAtMost(h) / 90f).coerceIn(0.8f, 2.5f)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            drawStampShape(this, tulip, color)
        }
    }
}

val TulipaCanvasRenderer = DiagramaCanvasRenderer
