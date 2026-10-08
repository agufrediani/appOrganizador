package com.example.roadbookorganizador.gps.racebox

import java.io.ByteArrayOutputStream

/**
 * Parser de paquetes binarios U-Blox UBX para RaceBox Mini / Mini S / Micro.
 * Cumple con la especificación de protocolo RaceBox Revisión 9.
 *
 * Maneja el buffer FIFO, detección de cabeceras de sincronización (0xB5 0x62),
 * validación de Checksum Fletcher y decodificación de enteros Little-Endian.
 */
class RaceBoxPacketParser(
    /**
     * Modelo del equipo conectado: true = RaceBox Micro, false = Mini / Mini S,
     * null = desconocido (se usa la heurística por valor).
     */
    private val esRaceBoxMicro: () -> Boolean? = { null },
    private val onTelemetryParsed: (RaceBoxTelemetry) -> Unit
) {

    private val buffer = ByteArrayOutputStream()

    @Synchronized
    fun feedBytes(bytes: ByteArray) {
        buffer.write(bytes)
        processBuffer()
    }

    @Synchronized
    fun reset() {
        buffer.reset()
    }

    private fun processBuffer() {
        var raw = buffer.toByteArray()

        while (raw.size >= 8) { // Mínimo encabezado (6) + checksum (2)
            // 1. Buscar cabecera de sincronización 0xB5 0x62
            var syncIndex = -1
            for (i in 0 until raw.size - 1) {
                if ((raw[i].toInt() and 0xFF) == 0xB5 && (raw[i + 1].toInt() and 0xFF) == 0x62) {
                    syncIndex = i
                    break
                }
            }

            if (syncIndex == -1) {
                // No hay cabecera en todo el buffer, conservar solo el último byte por si es 0xB5
                val lastByte = raw.last()
                buffer.reset()
                if ((lastByte.toInt() and 0xFF) == 0xB5) {
                    buffer.write(byteArrayOf(lastByte))
                }
                return
            }

            // Descartar bytes basura anteriores al sync
            if (syncIndex > 0) {
                raw = raw.copyOfRange(syncIndex, raw.size)
                buffer.reset()
                buffer.write(raw)
            }

            if (raw.size < 6) return // Esperar a tener clase, ID y longitud

            val msgClass = raw[2].toInt() and 0xFF
            val msgId = raw[3].toInt() and 0xFF
            val payloadLength = (raw[4].toInt() and 0xFF) or ((raw[5].toInt() and 0xFF) shl 8)

            val totalPacketLength = 6 + payloadLength + 2 // 2 sync + 2 class/id + 2 len + payload + 2 checksum

            if (totalPacketLength > 512) {
                // Paquete inválido/corrupto, saltar sync para buscar el siguiente
                raw = raw.copyOfRange(2, raw.size)
                buffer.reset()
                buffer.write(raw)
                continue
            }

            if (raw.size < totalPacketLength) {
                // Aún faltan fragmentos por llegar de la notificación BLE
                return
            }

            // 2. Validar Checksum (Fletcher-8 según spec RaceBox / U-Blox UBX)
            var ckA = 0
            var ckB = 0
            for (i in 2 until (6 + payloadLength)) {
                ckA = (ckA + (raw[i].toInt() and 0xFF)) and 0xFF
                ckB = (ckB + ckA) and 0xFF
            }

            val expectedCkA = raw[6 + payloadLength].toInt() and 0xFF
            val expectedCkB = raw[6 + payloadLength + 1].toInt() and 0xFF

            if (ckA == expectedCkA && ckB == expectedCkB) {
                // Checksum válido
                if (msgClass == 0xFF && msgId == 0x01 && payloadLength == 80) {
                    val telemetry = decodeDataMessage(raw, 6)
                    onTelemetryParsed(telemetry)
                }
                // Descartar el paquete procesado del buffer
                raw = raw.copyOfRange(totalPacketLength, raw.size)
                buffer.reset()
                buffer.write(raw)
            } else {
                // Checksum incorrecto, descartar sync y buscar siguiente
                raw = raw.copyOfRange(2, raw.size)
                buffer.reset()
                buffer.write(raw)
            }
        }
    }

    private fun decodeDataMessage(raw: ByteArray, offset: Int): RaceBoxTelemetry {
        val iTow = readUInt32(raw, offset + 0)
        val year = readUInt16(raw, offset + 4)
        val month = raw[offset + 6].toInt() and 0xFF
        val day = raw[offset + 7].toInt() and 0xFF
        val hour = raw[offset + 8].toInt() and 0xFF
        val minute = raw[offset + 9].toInt() and 0xFF
        val second = raw[offset + 10].toInt() and 0xFF
        val validityFlags = raw[offset + 11].toInt() and 0xFF

        val timeAccuracyNs = readUInt32(raw, offset + 12)
        val nanoseconds = readInt32(raw, offset + 16)
        val fixStatus = raw[offset + 20].toInt() and 0xFF
        val fixStatusFlags = raw[offset + 21].toInt() and 0xFF
        val satellitesCount = raw[offset + 23].toInt() and 0xFF

        val rawLon = readInt32(raw, offset + 24)
        val rawLat = readInt32(raw, offset + 28)
        val longitude = rawLon / 10_000_000.0
        val latitude = rawLat / 10_000_000.0

        val wgsAltMm = readInt32(raw, offset + 32)
        val mslAltMm = readInt32(raw, offset + 36)
        val wgsAltitude = wgsAltMm / 1000.0
        val mslAltitude = mslAltMm / 1000.0

        val hAccMm = readUInt32(raw, offset + 40)
        val vAccMm = readUInt32(raw, offset + 44)
        val horizontalAccuracy = hAccMm / 1000.0f
        val verticalAccuracy = vAccMm / 1000.0f

        val speedMmS = readInt32(raw, offset + 48)
        val speedKmh = (speedMmS * 0.0036f).coerceAtLeast(0.0f)

        val rawHeading = readInt32(raw, offset + 52)
        val heading = (rawHeading / 100_000.0f + 360f) % 360f

        val speedAccMmS = readUInt32(raw, offset + 56)
        val speedAccKmh = speedAccMmS * 0.0036f

        val headingAccRaw = readUInt32(raw, offset + 60)
        val headingAccDeg = headingAccRaw / 100_000.0f

        val pdopRaw = readUInt16(raw, offset + 64)
        val pdop = pdopRaw / 100.0f

        // Offset 67: en Mini / Mini S = bit 7 "cargando" + 7 bits de batería (%).
        //            en Micro = voltaje de entrada x10 (byte completo, ej. 138 -> 13,8 V).
        val battByte = raw[offset + 67].toInt() and 0xFF
        val isMicro = when (esRaceBoxMicro()) {
            true -> true
            false -> false
            // Modelo desconocido: un valor > 100 sin el bit de carga solo puede ser voltaje
            null -> (battByte and 0x80) == 0 && battByte > 100
        }
        val isCharging = !isMicro && (battByte and 0x80) != 0
        val batteryPct = if (isMicro) 100 else (battByte and 0x7F)
        val inputVoltage = if (isMicro) battByte / 10.0f else 0.0f

        // Fuerzas G (milli-g / 1000)
        val gForceX = readInt16(raw, offset + 68) / 1000.0f
        val gForceY = readInt16(raw, offset + 70) / 1000.0f
        val gForceZ = readInt16(raw, offset + 72) / 1000.0f

        // Velocidades de rotación (centi-grados/s / 100)
        val rotX = readInt16(raw, offset + 74) / 100.0f
        val rotY = readInt16(raw, offset + 76) / 100.0f
        val rotZ = readInt16(raw, offset + 78) / 100.0f

        return RaceBoxTelemetry(
            iTow = iTow,
            year = year,
            month = month,
            day = day,
            hour = hour,
            minute = minute,
            second = second,
            nanoseconds = nanoseconds,
            timeAccuracyNs = timeAccuracyNs,
            validityFlags = validityFlags,
            fixStatus = fixStatus,
            fixStatusFlags = fixStatusFlags,
            satellitesCount = satellitesCount,
            latitude = latitude,
            longitude = longitude,
            wgsAltitudeMeters = wgsAltitude,
            mslAltitudeMeters = mslAltitude,
            horizontalAccuracyMeters = horizontalAccuracy,
            verticalAccuracyMeters = verticalAccuracy,
            speedKmh = speedKmh,
            speedAccuracyKmh = speedAccKmh,
            headingDegrees = heading,
            headingAccuracyDeg = headingAccDeg,
            pdop = pdop,
            isCharging = isCharging,
            batteryPercent = batteryPct,
            isMicroVoltage = isMicro,
            inputVoltageVolts = inputVoltage,
            gForceX = gForceX,
            gForceY = gForceY,
            gForceZ = gForceZ,
            rotationRateX = rotX,
            rotationRateY = rotY,
            rotationRateZ = rotZ
        )
    }

    private fun readUInt16(raw: ByteArray, offset: Int): Int {
        return (raw[offset].toInt() and 0xFF) or
                ((raw[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readInt16(raw: ByteArray, offset: Int): Short {
        return ((raw[offset].toInt() and 0xFF) or
                ((raw[offset + 1].toInt() and 0xFF) shl 8)).toShort()
    }

    private fun readUInt32(raw: ByteArray, offset: Int): Long {
        val b0 = raw[offset].toLong() and 0xFF
        val b1 = raw[offset + 1].toLong() and 0xFF
        val b2 = raw[offset + 2].toLong() and 0xFF
        val b3 = raw[offset + 3].toLong() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun readInt32(raw: ByteArray, offset: Int): Int {
        val b0 = raw[offset].toInt() and 0xFF
        val b1 = raw[offset + 1].toInt() and 0xFF
        val b2 = raw[offset + 2].toInt() and 0xFF
        val b3 = raw[offset + 3].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }
}
