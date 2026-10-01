package com.example.roadbookorganizador.gps.racebox

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

enum class RaceBoxConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED
}

data class DiscoveredRaceBox(
    val name: String,
    val address: String,
    val rssi: Int
)

/**
 * Gestor Bluetooth Low Energy (BLE) nativo para antenas RaceBox Mini, Mini S y Micro.
 * Implementa el protocolo UART-over-BLE oficial a 25 Hz.
 */
class RaceBoxBleManager(private val context: Context) {

    companion object {
        private const val TAG = "RaceBoxBleManager"

        // UUIDs oficiales según documentación RaceBox Rev 9
        val UART_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val CLIENT_CONFIG_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        @Volatile
        private var instance: RaceBoxBleManager? = null

        fun getInstance(context: Context): RaceBoxBleManager {
            return instance ?: synchronized(this) {
                instance ?: RaceBoxBleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val _connectionStatus = MutableStateFlow(RaceBoxConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<RaceBoxConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _latestTelemetry = MutableStateFlow<RaceBoxTelemetry?>(null)
    val latestTelemetry: StateFlow<RaceBoxTelemetry?> = _latestTelemetry.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredRaceBox>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredRaceBox>> = _discoveredDevices.asStateFlow()

    private val prefs = context.getSharedPreferences("racebox_prefs", Context.MODE_PRIVATE)
    private var bluetoothGatt: BluetoothGatt? = null
    private var lastConnectedAddress: String? = prefs.getString("last_address", null)
    private var autoReconnectEnabled = true

    private var simulationJob: Job? = null
    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    fun startSimulation() {
        stopScan()
        disconnect()
        simulationJob?.cancel()
        _isSimulating.value = true
        _connectionStatus.value = RaceBoxConnectionStatus.CONNECTED
        _connectedDeviceName.value = "GPS Externo (Demo 25Hz)"

        simulationJob = CoroutineScope(Dispatchers.Default).launch {
            var simLat = -31.420082
            var simLon = -64.499120
            var heading = 45f
            var speedKmh = 92.5f
            var timeMillis = System.currentTimeMillis()

            while (isActive && _isSimulating.value) {
                delay(40) // 25 Hz = 40 ms
                timeMillis += 40

                val noiseSpeed = ((Math.random() - 0.49) * 0.5).toFloat()
                speedKmh = (speedKmh + noiseSpeed).coerceIn(65f, 135f)
                heading = (heading + 0.15f) % 360f

                val distMeters = (speedKmh / 3.6f) * 0.04f
                val rad = Math.toRadians(heading.toDouble())
                simLat += (distMeters * Math.cos(rad)) / 111111.0
                simLon += (distMeters * Math.sin(rad)) / (111111.0 * Math.cos(Math.toRadians(simLat)))

                val gLat = (Math.sin(timeMillis / 1200.0) * 0.45).toFloat()
                val gLon = (Math.cos(timeMillis / 900.0) * 0.35).toFloat()

                val telemetry = RaceBoxTelemetry(
                    iTow = timeMillis,
                    year = 2026,
                    month = 9,
                    day = 24,
                    hour = 10,
                    minute = 35,
                    second = ((timeMillis / 1000) % 60).toInt(),
                    nanoseconds = ((timeMillis % 1000) * 1_000_000).toInt(),
                    timeAccuracyNs = 15L,
                    validityFlags = 0x07,
                    fixStatus = 3,
                    fixStatusFlags = 1,
                    satellitesCount = 28,
                    latitude = simLat,
                    longitude = simLon,
                    wgsAltitudeMeters = 660.0,
                    mslAltitudeMeters = 642.5,
                    horizontalAccuracyMeters = 0.08f,
                    verticalAccuracyMeters = 0.12f,
                    speedKmh = speedKmh,
                    speedAccuracyKmh = 0.05f,
                    headingDegrees = heading,
                    headingAccuracyDeg = 0.2f,
                    pdop = 0.9f,
                    isCharging = false,
                    batteryPercent = 95,
                    isMicroVoltage = false,
                    inputVoltageVolts = 4.14f,
                    gForceX = gLon,
                    gForceY = gLat,
                    gForceZ = 1.01f,
                    rotationRateX = 0.1f,
                    rotationRateY = 0.2f,
                    rotationRateZ = 1.5f
                )

                _latestTelemetry.value = telemetry
                synchronized(telemetryListeners) {
                    telemetryListeners.forEach { listener ->
                        try { listener(telemetry) } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    fun stopSimulation() {
        _isSimulating.value = false
        simulationJob?.cancel()
        simulationJob = null
        if (_connectedDeviceName.value?.contains("Demo") == true) {
            _connectedDeviceName.value = null
            _connectionStatus.value = RaceBoxConnectionStatus.DISCONNECTED
            _latestTelemetry.value = null
        }
    }

    private val parser = RaceBoxPacketParser { telemetry ->
        _latestTelemetry.value = telemetry
        // Notificar a listeners registrados (ej. OdometerEngine)
        telemetryListeners.forEach { listener ->
            try {
                listener(telemetry)
            } catch (e: Exception) {
                Log.e(TAG, "Error en telemetryListener", e)
            }
        }
    }

    private val telemetryListeners = mutableListOf<(RaceBoxTelemetry) -> Unit>()

    fun addTelemetryListener(listener: (RaceBoxTelemetry) -> Unit) {
        synchronized(telemetryListeners) {
            telemetryListeners.add(listener)
        }
    }

    fun removeTelemetryListener(listener: (RaceBoxTelemetry) -> Unit) {
        synchronized(telemetryListeners) {
            telemetryListeners.remove(listener)
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: ""
            val hasUart = result.scanRecord?.serviceUuids?.any { 
                it.uuid == UART_SERVICE_UUID || it.uuid.toString().startsWith("6e400001", ignoreCase = true) 
            } == true
            val isRaceBox = name.contains("RaceBox", ignoreCase = true) ||
                            name.startsWith("RB", ignoreCase = true) ||
                            hasUart

            if (isRaceBox) {
                val displayName = "GPS Externo"
                val currentList = _discoveredDevices.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.address == device.address }
                val discovered = DiscoveredRaceBox(displayName, device.address, result.rssi)

                if (existingIndex >= 0) {
                    currentList[existingIndex] = discovered
                } else {
                    currentList.add(discovered)
                    Log.d(TAG, "GPS Externo detectado: $displayName (${device.address})")
                }
                _discoveredDevices.value = currentList
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Fallo el escaneo BLE. Código: $errorCode")
            _connectionStatus.value = RaceBoxConnectionStatus.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) {
            try {
                @Suppress("DEPRECATION")
                adapter.enable()
            } catch (_: Exception) {}
            if (!adapter.isEnabled) {
                Log.w(TAG, "Bluetooth deshabilitado")
                return
            }
        }

        // Cargar dispositivos vinculados/emparejados previamente en Android
        val initialList = mutableListOf<DiscoveredRaceBox>()
        try {
            val paired = adapter.bondedDevices
            paired?.forEach { dev ->
                val dName = dev.name ?: ""
                if (dName.contains("RaceBox", ignoreCase = true) || dName.startsWith("RB", ignoreCase = true)) {
                    initialList.add(DiscoveredRaceBox("GPS Externo", dev.address, -45))
                }
            }
        } catch (_: Exception) {}
        _discoveredDevices.value = initialList
        _connectionStatus.value = RaceBoxConnectionStatus.SCANNING

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _connectionStatus.value = RaceBoxConnectionStatus.DISCONNECTED
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando escaneo BLE", e)
            _connectionStatus.value = RaceBoxConnectionStatus.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        try {
            scanner.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo escaneo", e)
        }
        if (_connectionStatus.value == RaceBoxConnectionStatus.SCANNING) {
            _connectionStatus.value = RaceBoxConnectionStatus.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        stopScan()
        disconnect()

        val adapter = bluetoothAdapter ?: return
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            Log.e(TAG, "Dirección Bluetooth inválida: $address", e)
            return
        }

        lastConnectedAddress = address
        _connectedDeviceName.value = "GPS Externo"
        _connectionStatus.value = RaceBoxConnectionStatus.CONNECTING
        parser.reset()

        Log.d(TAG, "Conectando a GPS Externo en $address...")
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error desconectando GATT", e)
        }
        bluetoothGatt = null
        _connectionStatus.value = RaceBoxConnectionStatus.DISCONNECTED
        _connectedDeviceName.value = null
        _latestTelemetry.value = null
        parser.reset()
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Conectado al GATT de GPS Externo. Solicitando MTU 512...")
                _connectionStatus.value = RaceBoxConnectionStatus.CONNECTING
                // 1. Pedir MTU máximo para recibir paquetes completos sin fragmentar
                val mtuRequested = gatt.requestMtu(512)
                if (!mtuRequested) {
                    gatt.discoverServices()
                }
                // Fallback para descubrir servicios si onMtuChanged no es invocado por la ROM
                CoroutineScope(Dispatchers.Main).launch {
                    delay(800L)
                    if (_connectionStatus.value == RaceBoxConnectionStatus.CONNECTING) {
                        try {
                            gatt.discoverServices()
                        } catch (_: Exception) {}
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "Desconectado de GPS Externo")
                _connectionStatus.value = RaceBoxConnectionStatus.DISCONNECTED
                _connectedDeviceName.value = null
                _latestTelemetry.value = null
                parser.reset()

                if (autoReconnectEnabled && lastConnectedAddress != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(2500L)
                        lastConnectedAddress?.let { addr ->
                            if (_connectionStatus.value == RaceBoxConnectionStatus.DISCONNECTED) {
                                Log.d(TAG, "Intentando reconexión automática a GPS Externo...")
                                connect(addr)
                            }
                        }
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "MTU configurado en $mtu (status: $status)")
            // 2. Prioridad de alta velocidad (intervalos cortos de 7.5ms - 15ms)
            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            // 3. Descubrir servicios
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Fallo al descubrir servicios: $status")
                return
            }

            val uartService = gatt.getService(UART_SERVICE_UUID)
                ?: gatt.services.find { it.uuid.toString().startsWith("6e400001", ignoreCase = true) }
            if (uartService == null) {
                Log.e(TAG, "Servicio UART GPS no encontrado en el dispositivo")
                gatt.services.forEach { s -> Log.d(TAG, "Servicio disponible: ${s.uuid}") }
                return
            }

            val txChar = uartService.getCharacteristic(TX_CHAR_UUID)
                ?: uartService.characteristics.find { it.uuid.toString().startsWith("6e400003", ignoreCase = true) }
            if (txChar == null) {
                Log.e(TAG, "Característica TX no encontrada en servicio UART")
                return
            }

            // Habilitar notificaciones localmente
            val notificationSet = gatt.setCharacteristicNotification(txChar, true)
            Log.d(TAG, "setCharacteristicNotification TX: $notificationSet")

            // Escribir en el descriptor CCCD para que el hardware comience a transmitir
            val descriptor = txChar.getDescriptor(CLIENT_CONFIG_DESCRIPTOR_UUID)
                ?: txChar.descriptors.find { it.uuid.toString().startsWith("00002902", ignoreCase = true) }
            if (descriptor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
                Log.d(TAG, "Suscripción a notificaciones de 25 Hz iniciada exitosamente")
                _connectionStatus.value = RaceBoxConnectionStatus.CONNECTED
                prefs.edit().putString("last_address", gatt.device.address).putString("last_name", _connectedDeviceName.value).apply()
            } else {
                Log.e(TAG, "Descriptor CCCD no encontrado en TX, marcando como conectado directamente")
                _connectionStatus.value = RaceBoxConnectionStatus.CONNECTED
                prefs.edit().putString("last_address", gatt.device.address).putString("last_name", _connectedDeviceName.value).apply()
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor CCCD confirmado por GPS Externo. Transmisión 25 Hz lista.")
                _connectionStatus.value = RaceBoxConnectionStatus.CONNECTED
            }
        }

        @Deprecated("Deprecated for Android 13+")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val isTx = characteristic.uuid == TX_CHAR_UUID ||
                    characteristic.uuid.toString().startsWith("6e400003", ignoreCase = true)
            if (isTx) {
                @Suppress("DEPRECATION")
                val data = characteristic.value ?: return
                parser.feedBytes(data)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val isTx = characteristic.uuid == TX_CHAR_UUID ||
                    characteristic.uuid.toString().startsWith("6e400003", ignoreCase = true)
            if (isTx) {
                parser.feedBytes(value)
            }
        }
    }
}
