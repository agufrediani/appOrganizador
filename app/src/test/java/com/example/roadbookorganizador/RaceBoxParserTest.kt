package com.example.roadbookorganizador

import com.example.roadbookorganizador.gps.racebox.RaceBoxPacketParser
import com.example.roadbookorganizador.gps.racebox.RaceBoxTelemetry
import org.junit.Assert.*
import org.junit.Test

class RaceBoxParserTest {

    @Test
    fun testOfficialPdfSamplePacketDecoding() {
        // Paquete de ejemplo oficial extraído directamente de la página 7 y 8 del PDF Rev 9
        val hexString = "B562FF015000A0E70C07E607010A08330837190000002AAD4D0E0301EA0BC693E10D3B376F19618C09000F0109009C0300002C0700002300000000000000D000000088A9DD002C010059FDFF7100CE032FFF5600FCFF06DB"
        val bytes = hexStringToByteArray(hexString)
        assertEquals(88, bytes.size) // 6 header + 80 payload + 2 checksum

        var receivedTelemetry: RaceBoxTelemetry? = null
        val parser = RaceBoxPacketParser { telemetry ->
            receivedTelemetry = telemetry
        }

        // Alimentar en dos partes para probar reensamblado de fragmentos BLE
        val part1 = bytes.copyOfRange(0, 30)
        val part2 = bytes.copyOfRange(30, bytes.size)

        parser.feedBytes(part1)
        assertNull("Aún no debe emitir con fragmento parcial", receivedTelemetry)

        parser.feedBytes(part2)
        assertNotNull("Debe emitir telemetría al completar el paquete", receivedTelemetry)

        val t = receivedTelemetry!!

        // Verificaciones exactas contra la tabla de la página 8 del PDF
        assertEquals(118286240L, t.iTow)
        assertEquals(2022, t.year)
        assertEquals(1, t.month)
        assertEquals(10, t.day)
        assertEquals(8, t.hour)
        assertEquals(51, t.minute)
        assertEquals(8, t.second)
        assertEquals(0x37, t.validityFlags)
        assertEquals(3, t.fixStatus) // 3D Fix
        assertEquals(11, t.satellitesCount) // 11 SVs

        // Coordenadas
        assertEquals(23.2887238, t.longitude, 0.0000001)
        assertEquals(42.6719035, t.latitude, 0.0000001)

        // Altitudes
        assertEquals(625.761, t.wgsAltitudeMeters, 0.001)
        assertEquals(590.095, t.mslAltitudeMeters, 0.001)

        // Precisión
        assertEquals(0.924f, t.horizontalAccuracyMeters, 0.001f)
        assertEquals(1.836f, t.verticalAccuracyMeters, 0.001f)

        // Velocidad: 35 mm/s = 0.126 km/h
        assertEquals(0.126f, t.speedKmh, 0.001f)

        // Rumbo
        assertEquals(0f, t.headingDegrees, 0.01f)

        // Batería: 89%, no cargando
        assertFalse(t.isCharging)
        assertEquals(89, t.batteryPercent)

        // Fuerzas G
        assertEquals(-0.003f, t.gForceX, 0.001f)
        assertEquals(0.113f, t.gForceY, 0.001f)
        assertEquals(0.974f, t.gForceZ, 0.001f)

        // Tasas de rotación
        assertEquals(-2.09f, t.rotationRateX, 0.01f)
        assertEquals(0.86f, t.rotationRateY, 0.01f)
        assertEquals(-0.04f, t.rotationRateZ, 0.01f)
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
