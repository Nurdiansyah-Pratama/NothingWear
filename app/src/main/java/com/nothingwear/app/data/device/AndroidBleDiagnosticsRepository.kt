package com.nothingwear.app.data.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.nothingwear.app.core.bluetooth.GattStatusDecoder
import com.nothingwear.app.core.bluetooth.GattUuidLookup
import com.nothingwear.app.core.bluetooth.LogRedactor
import com.nothingwear.app.core.bluetooth.WatchS4Identifier
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.permissions.BluetoothPermissionChecker
import com.nothingwear.app.domain.model.BleDeviceSummary
import com.nothingwear.app.domain.model.BleDiagnosticsSnapshot
import com.nothingwear.app.domain.model.BluetoothAdapterStatus
import com.nothingwear.app.domain.model.BluetoothTransportType
import com.nothingwear.app.domain.model.DiagnosticLogEntry
import com.nothingwear.app.domain.model.DiagnosticLogLevel
import com.nothingwear.app.domain.model.DiscoveredDeviceSource
import com.nothingwear.app.domain.model.GattCharacteristicInfo
import com.nothingwear.app.domain.model.GattInspectionSession
import com.nothingwear.app.domain.model.GattServiceInfo
import com.nothingwear.app.domain.model.StandardDeviceInfo
import com.nothingwear.app.domain.model.WatchConnectionState
import com.nothingwear.app.domain.repository.DeviceDiagnosticsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * M2 read-only BLE discovery & GATT diagnostics repository.
 *
 * SAFETY CONSTRAINTS (Master Project Plan Section 9):
 * - Strictly read-only GATT inspection: NEVER writes characteristics or descriptors.
 * - Auto-reads ONLY verified Bluetooth SIG standard Battery Level (0x2A19) and
 *   Device Information (0x180A) strings when exposed with PROPERTY_READ.
 * - Preserves official Mi Fitness pairing (never unbonds or sends unknown commands).
 * - Redacts MAC addresses from all logs and exported diagnostic text.
 */
@Singleton
class AndroidBleDiagnosticsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionChecker: BluetoothPermissionChecker,
    private val timeProvider: TimeProvider,
) : DeviceDiagnosticsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val logSequence = AtomicLong(1L)
    private val activeSessionId = AtomicLong(0L)
    private val discoveredDevices = ConcurrentHashMap<String, BleDeviceSummary>()
    private val readQueue = ArrayDeque<BluetoothGattCharacteristic>()
    private val logTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())

    private val _diagnostics = MutableStateFlow(BleDiagnosticsSnapshot())
    override fun observeDiagnostics(): Flow<BleDiagnosticsSnapshot> = _diagnostics.asStateFlow()

    private var activeScanCallback: ScanCallback? = null
    private var scanTimeoutJob: Job? = null
    private var connectJob: Job? = null
    private var connectTimeoutJob: Job? = null
    private var activeGatt: BluetoothGatt? = null

    private val bluetoothManager: BluetoothManager?
        get() = context.getSystemService(BluetoothManager::class.java)

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private fun hasBleFeature(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    private fun currentAdapterStatus(): BluetoothAdapterStatus {
        if (!hasBleFeature()) return BluetoothAdapterStatus.UNSUPPORTED
        val adapter = bluetoothAdapter ?: return BluetoothAdapterStatus.UNSUPPORTED
        return if (adapter.isEnabled) BluetoothAdapterStatus.ENABLED else BluetoothAdapterStatus.DISABLED
    }

    @SuppressLint("MissingPermission")
    override fun refreshEnvironmentAndBondedDevices() {
        val status = currentAdapterStatus()
        _diagnostics.update { it.copy(adapterStatus = status) }

        if (status != BluetoothAdapterStatus.ENABLED) {
            appendLog(
                level = DiagnosticLogLevel.WARN,
                rawMessage = "Bluetooth adapter status: $status. Enable Bluetooth to discover devices.",
            )
            return
        }

        if (!permissionChecker.hasConnectPermission()) {
            return
        }

        try {
            val now = timeProvider.nowMillis()
            val connectedGattDevices = bluetoothManager
                ?.getConnectedDevices(BluetoothProfile.GATT)
                .orEmpty()
            val connectedGattAddresses = connectedGattDevices.map { it.address }.toSet()

            connectedGattDevices.forEach { device ->
                upsertDeviceFromBluetoothDevice(
                    device = device,
                    source = DiscoveredDeviceSource.SYSTEM_CONNECTED,
                    isSystemConnected = true,
                    rssi = null,
                    manufacturerIds = emptySet(),
                    serviceUuids = emptyList(),
                    nowMillis = now,
                )
            }

            bluetoothAdapter?.bondedDevices.orEmpty().forEach { device ->
                val isSystemConnected = device.address in connectedGattAddresses
                upsertDeviceFromBluetoothDevice(
                    device = device,
                    source = if (isSystemConnected) {
                        DiscoveredDeviceSource.SYSTEM_CONNECTED
                    } else {
                        DiscoveredDeviceSource.BONDED
                    },
                    isSystemConnected = isSystemConnected,
                    rssi = null,
                    manufacturerIds = emptySet(),
                    serviceUuids = emptyList(),
                    nowMillis = now,
                )
            }

            publishSortedDevices()
            appendLog(
                level = DiagnosticLogLevel.INFO,
                rawMessage = "Checked bonded/system-connected devices (${discoveredDevices.size} listed; ${connectedGattAddresses.size} active OS GATT links).",
            )
        } catch (se: SecurityException) {
            appendLog(
                level = DiagnosticLogLevel.ERROR,
                rawMessage = "Missing BLUETOOTH_CONNECT permission while reading bonded devices: ${se.message}",
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun startBleScan() {
        val status = currentAdapterStatus()
        _diagnostics.update { it.copy(adapterStatus = status) }

        if (status != BluetoothAdapterStatus.ENABLED) {
            appendLog(
                level = DiagnosticLogLevel.WARN,
                rawMessage = "Cannot start BLE scan: Bluetooth adapter is $status.",
            )
            return
        }

        if (!permissionChecker.hasScanPermission()) {
            appendLog(
                level = DiagnosticLogLevel.WARN,
                rawMessage = "BLE scan blocked: required Bluetooth scan permission is not granted.",
            )
            return
        }

        if (activeScanCallback != null) {
            return
        }

        refreshEnvironmentAndBondedDevices()

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            appendLog(
                level = DiagnosticLogLevel.ERROR,
                rawMessage = "BluetoothLeScanner is unavailable on this adapter.",
            )
            return
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::handleScanResult)
            }

            override fun onScanFailed(errorCode: Int) {
                activeScanCallback = null
                scanTimeoutJob?.cancel()
                _diagnostics.update { it.copy(isScanning = false) }
                appendLog(
                    level = DiagnosticLogLevel.ERROR,
                    rawMessage = "BLE scan failed with Android errorCode=$errorCode.",
                )
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            activeScanCallback = callback
            scanner.startScan(null, settings, callback)
            _diagnostics.update { it.copy(isScanning = true) }
            appendLog(
                level = DiagnosticLogLevel.INFO,
                rawMessage = "Started read-only BLE scan (auto-stop in ${SCAN_TIMEOUT_MS / 1000}s).",
            )

            scanTimeoutJob?.cancel()
            scanTimeoutJob = scope.launch {
                delay(SCAN_TIMEOUT_MS)
                stopBleScanInternal(reason = "auto-stopped after ${SCAN_TIMEOUT_MS / 1000}s timeout")
            }
        } catch (se: SecurityException) {
            activeScanCallback = null
            _diagnostics.update { it.copy(isScanning = false) }
            appendLog(
                level = DiagnosticLogLevel.ERROR,
                rawMessage = "SecurityException starting BLE scan: ${se.message}",
            )
        } catch (e: Exception) {
            activeScanCallback = null
            _diagnostics.update { it.copy(isScanning = false) }
            appendLog(
                level = DiagnosticLogLevel.ERROR,
                rawMessage = "Unexpected error starting BLE scan: ${e.message}",
            )
        }
    }

    override fun stopBleScan() {
        stopBleScanInternal(reason = "stopped by user")
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScanInternal(reason: String) {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        val callback = activeScanCallback ?: run {
            _diagnostics.update { it.copy(isScanning = false) }
            return
        }
        activeScanCallback = null

        try {
            if (permissionChecker.hasScanPermission() && bluetoothAdapter?.isEnabled == true) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
            }
        } catch (_: Exception) {
            // Safe cleanup if Bluetooth turned off mid-scan
        } finally {
            _diagnostics.update { it.copy(isScanning = false) }
            appendLog(
                level = DiagnosticLogLevel.INFO,
                rawMessage = "BLE scan $reason (${discoveredDevices.size} devices listed).",
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun inspectDeviceGatt(addressKey: String) {
        val status = currentAdapterStatus()
        _diagnostics.update { it.copy(adapterStatus = status) }

        if (status != BluetoothAdapterStatus.ENABLED) {
            appendLog(
                level = DiagnosticLogLevel.WARN,
                rawMessage = "Cannot inspect GATT: Bluetooth is $status.",
            )
            return
        }

        if (!permissionChecker.hasConnectPermission()) {
            appendLog(
                level = DiagnosticLogLevel.WARN,
                rawMessage = "Cannot inspect GATT: BLUETOOTH_CONNECT permission is not granted.",
            )
            return
        }

        stopBleScanInternal(reason = "paused before read-only GATT inspection")

        val hadActiveGatt = activeGatt != null
        connectJob?.cancel()
        connectTimeoutJob?.cancel()
        // Advance session ID BEFORE closing old gatt so stale callbacks from the previous
        // gatt are ignored and cannot overwrite the new inspection session state.
        val sessionId = activeSessionId.incrementAndGet()
        closeActiveGatt(silent = true)

        val summary = discoveredDevices[addressKey]
        val redacted = summary?.redactedAddress ?: LogRedactor.redactMacAddress(addressKey)
        val displayName = summary?.displayName ?: "BLE Device"

        val remoteDevice = try {
            bluetoothAdapter?.getRemoteDevice(addressKey)
        } catch (e: IllegalArgumentException) {
            appendLog(
                level = DiagnosticLogLevel.ERROR,
                rawMessage = "Invalid Bluetooth address for GATT inspection: ${e.message}",
            )
            null
        } ?: return

        val transportType = mapTransportType(remoteDevice.type)
        val osGattConnected = try {
            bluetoothManager?.getConnectionState(remoteDevice, BluetoothProfile.GATT) ==
                BluetoothProfile.STATE_CONNECTED
        } catch (_: Exception) {
            false
        }

        if (transportType == BluetoothTransportType.CLASSIC && !osGattConnected) {
            val classicNote = "This Bluetooth entry is Classic BR/EDR only (calls/audio) and does not expose BLE GATT. " +
                "Run 'Start BLE Scan' to check if Watch S4 exposes a separate BLE advertisement, or inspect the System GATT entry."
            appendLog(
                level = DiagnosticLogLevel.WARN,
                rawMessage = "$displayName [$redacted] reported DEVICE_TYPE_CLASSIC (not LE/DUAL). $classicNote",
            )
            _diagnostics.update {
                it.copy(
                    gattSession = GattInspectionSession(
                        deviceAddressKey = addressKey,
                        deviceName = displayName,
                        redactedAddress = redacted,
                        connectionState = WatchConnectionState.UNSUPPORTED,
                        lastStatusNote = classicNote,
                    ),
                )
            }
            return
        }

        // If OS already has an active GATT link (e.g. Mi Fitness), autoConnect=true attaches
        // cleanly without sending a conflicting direct LE connect command.
        val preferAutoAttachFirst = osGattConnected || summary?.isSystemConnected == true

        _diagnostics.update {
            it.copy(
                gattSession = GattInspectionSession(
                    deviceAddressKey = addressKey,
                    deviceName = displayName,
                    redactedAddress = redacted,
                    connectionState = WatchConnectionState.CONNECTING,
                    services = emptyList(),
                    standardDeviceInfo = StandardDeviceInfo(),
                    lastStatusNote = if (hadActiveGatt) {
                        "Resetting previous GATT handle before re-inspecting (${transportType.label})..."
                    } else {
                        "Opening read-only GATT session (${transportType.label}, OS GATT=${if (osGattConnected) "connected" else "idle"})..."
                    },
                ),
            )
        }

        appendLog(
            level = DiagnosticLogLevel.INFO,
            rawMessage = "Inspecting $displayName [$redacted] | type=${transportType.label} | bonded=${summary?.isBonded == true} | osGattConnected=$osGattConnected | cooldown=${if (hadActiveGatt) "${RECONNECT_COOLDOWN_MS}ms" else "0ms"}",
        )

        connectJob = scope.launch {
            if (hadActiveGatt) {
                delay(RECONNECT_COOLDOWN_MS)
            }
            if (activeSessionId.get() != sessionId) return@launch

            openGattAttempt(
                sessionId = sessionId,
                remoteDevice = remoteDevice,
                displayName = displayName,
                redacted = redacted,
                autoConnect = preferAutoAttachFirst,
                allowFallback = !preferAutoAttachFirst,
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun openGattAttempt(
        sessionId: Long,
        remoteDevice: BluetoothDevice,
        displayName: String,
        redacted: String,
        autoConnect: Boolean,
        allowFallback: Boolean,
    ) {
        if (activeSessionId.get() != sessionId) return

        val strategyLabel = if (autoConnect) "Passive Auto-Attach (autoConnect=true)" else "Direct LE (autoConnect=false)"
        appendLog(
            level = DiagnosticLogLevel.INFO,
            rawMessage = "Attempt [$strategyLabel] -> $displayName [$redacted]",
        )
        _diagnostics.update {
            it.copy(
                gattSession = it.gattSession.copy(
                    connectionState = WatchConnectionState.CONNECTING,
                    lastStatusNote = "Connecting via $strategyLabel (read-only, 0 writes)...",
                ),
            )
        }

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, statusCode: Int, newState: Int) {
                if (activeSessionId.get() != sessionId) {
                    // Ignore stale callback from an earlier GATT handle and ensure it is closed.
                    runCatching { gatt.close() }
                    return
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectTimeoutJob?.cancel()
                        appendLog(
                            level = DiagnosticLogLevel.INFO,
                            rawMessage = "GATT connected via $strategyLabel to $displayName [$redacted] (${GattStatusDecoder.describeStatus(statusCode)}). Discovering services...",
                        )
                        _diagnostics.update {
                            it.copy(
                                gattSession = it.gattSession.copy(
                                    connectionState = WatchConnectionState.CONNECTED,
                                    lastStatusNote = "Connected ($strategyLabel). Discovering GATT services...",
                                ),
                            )
                        }
                        try {
                            val started = gatt.discoverServices()
                            if (!started) {
                                appendLog(
                                    level = DiagnosticLogLevel.ERROR,
                                    rawMessage = "gatt.discoverServices() returned false for $displayName [$redacted].",
                                )
                            }
                        } catch (se: SecurityException) {
                            appendLog(
                                level = DiagnosticLogLevel.ERROR,
                                rawMessage = "SecurityException during discoverServices: ${se.message}",
                            )
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectTimeoutJob?.cancel()
                        synchronized(readQueue) { readQueue.clear() }
                        if (activeGatt == gatt) {
                            activeGatt = null
                        }
                        runCatching { gatt.close() }

                        val decodedStatus = GattStatusDecoder.describeStatus(statusCode)
                        if (allowFallback && GattStatusDecoder.shouldFallbackToAutoAttach(statusCode)) {
                            appendLog(
                                level = DiagnosticLogLevel.WARN,
                                rawMessage = "$strategyLabel disconnected ($decodedStatus). Waiting ${RECONNECT_COOLDOWN_MS}ms then retrying with Passive Auto-Attach...",
                            )
                            connectJob = scope.launch {
                                delay(RECONNECT_COOLDOWN_MS)
                                if (activeSessionId.get() == sessionId) {
                                    openGattAttempt(
                                        sessionId = sessionId,
                                        remoteDevice = remoteDevice,
                                        displayName = displayName,
                                        redacted = redacted,
                                        autoConnect = true,
                                        allowFallback = false,
                                    )
                                }
                            }
                            return
                        }

                        val note = if (statusCode == BluetoothGatt.GATT_SUCCESS) {
                            "Disconnected cleanly (status=0). Mi Fitness pairing unaffected."
                        } else {
                            "Failed ($strategyLabel): $decodedStatus"
                        }
                        appendLog(
                            level = if (statusCode == BluetoothGatt.GATT_SUCCESS) {
                                DiagnosticLogLevel.INFO
                            } else {
                                DiagnosticLogLevel.WARN
                            },
                            rawMessage = "GATT disconnected from $displayName [$redacted]: $note",
                        )
                        _diagnostics.update {
                            it.copy(
                                gattSession = it.gattSession.copy(
                                    connectionState = if (statusCode == BluetoothGatt.GATT_SUCCESS) {
                                        WatchConnectionState.DISCONNECTED
                                    } else {
                                        WatchConnectionState.FAILED
                                    },
                                    lastStatusNote = note,
                                ),
                            )
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, statusCode: Int) {
                if (activeSessionId.get() != sessionId) return

                if (statusCode != BluetoothGatt.GATT_SUCCESS) {
                    val decoded = GattStatusDecoder.describeStatus(statusCode)
                    appendLog(
                        level = DiagnosticLogLevel.ERROR,
                        rawMessage = "Service discovery failed ($decoded) for $displayName [$redacted].",
                    )
                    _diagnostics.update {
                        it.copy(
                            gattSession = it.gattSession.copy(
                                connectionState = WatchConnectionState.FAILED,
                                lastStatusNote = "Service discovery failed: $decoded",
                            ),
                        )
                    }
                    return
                }

                val discoveredServices = gatt.services.orEmpty()
                val mappedServices = discoveredServices.map { service ->
                    GattServiceInfo(
                        uuid = service.uuid.toString().lowercase(),
                        displayName = GattUuidLookup.serviceDisplayName(service.uuid.toString()),
                        isPrimary = service.type == BluetoothGattService.SERVICE_TYPE_PRIMARY,
                        characteristics = service.characteristics.orEmpty().map { characteristic ->
                            val uuidStr = characteristic.uuid.toString().lowercase()
                            GattCharacteristicInfo(
                                uuid = uuidStr,
                                displayName = GattUuidLookup.characteristicDisplayName(uuidStr),
                                properties = GattUuidLookup.decodeProperties(characteristic.properties),
                                descriptorUuids = characteristic.descriptors.orEmpty()
                                    .map { it.uuid.toString().lowercase() },
                                isReadOnlyInspected = GattUuidLookup.isStandardSafeReadOnlyCharacteristic(uuidStr),
                            )
                        },
                    )
                }

                val totalChars = mappedServices.sumOf { it.characteristics.size }
                appendLog(
                    level = DiagnosticLogLevel.INFO,
                    rawMessage = "Discovered ${mappedServices.size} GATT services / $totalChars characteristics on $displayName [$redacted] (read-only; 0 writes).",
                )

                _diagnostics.update {
                    it.copy(
                        gattSession = it.gattSession.copy(
                            connectionState = WatchConnectionState.PROTOCOL_UNKNOWN,
                            services = mappedServices,
                            lastStatusNote = "Discovered ${mappedServices.size} services ($totalChars characteristics) via $strategyLabel (read-only; 0 writes). Watch S4 event sync protocol remains unverified (M3/M4).",
                        ),
                    )
                }

                synchronized(readQueue) {
                    readQueue.clear()
                    discoveredServices.forEach { service ->
                        service.characteristics.orEmpty().forEach { characteristic ->
                            val isReadable = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0
                            if (isReadable && GattUuidLookup.isStandardSafeReadOnlyCharacteristic(characteristic.uuid.toString())) {
                                readQueue.addLast(characteristic)
                            }
                        }
                    }
                }
                triggerNextStandardRead(sessionId, gatt)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                if (activeSessionId.get() != sessionId) return
                handleStandardCharacteristicRead(sessionId, gatt, characteristic, value, status)
            }

            @Deprecated("Used on Android versions below API 33")
            @Suppress("DEPRECATION")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                if (activeSessionId.get() != sessionId) return
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    handleStandardCharacteristicRead(
                        sessionId = sessionId,
                        gatt = gatt,
                        characteristic = characteristic,
                        value = characteristic.value ?: ByteArray(0),
                        status = status,
                    )
                }
            }
        }

        try {
            @Suppress("DEPRECATION")
            activeGatt = remoteDevice.connectGatt(
                context,
                autoConnect,
                callback,
                BluetoothDevice.TRANSPORT_LE,
                BluetoothDevice.PHY_LE_1M_MASK,
            )

            connectTimeoutJob?.cancel()
            val timeoutMs = if (autoConnect) AUTO_ATTACH_TIMEOUT_MS else DIRECT_CONNECT_TIMEOUT_MS
            connectTimeoutJob = scope.launch {
                delay(timeoutMs)
                if (activeSessionId.get() != sessionId) return@launch
                if (_diagnostics.value.gattSession.connectionState != WatchConnectionState.CONNECTING) return@launch

                closeActiveGatt(silent = true)
                if (allowFallback) {
                    appendLog(
                        level = DiagnosticLogLevel.WARN,
                        rawMessage = "$strategyLabel timed out after ${timeoutMs / 1000}s. Retrying via Passive Auto-Attach...",
                    )
                    delay(RECONNECT_COOLDOWN_MS)
                    if (activeSessionId.get() == sessionId) {
                        openGattAttempt(
                            sessionId = sessionId,
                            remoteDevice = remoteDevice,
                            displayName = displayName,
                            redacted = redacted,
                            autoConnect = true,
                            allowFallback = false,
                        )
                    }
                } else {
                    val timeoutNote = "Timed out after ${timeoutMs / 1000}s ($strategyLabel). Watch S4 may be connected via Classic Bluetooth only or Mi Fitness is exclusively holding the BLE GATT slot."
                    appendLog(
                        level = DiagnosticLogLevel.WARN,
                        rawMessage = "$displayName [$redacted]: $timeoutNote",
                    )
                    _diagnostics.update {
                        it.copy(
                            gattSession = it.gattSession.copy(
                                connectionState = WatchConnectionState.FAILED,
                                lastStatusNote = timeoutNote,
                            ),
                        )
                    }
                }
            }
        } catch (se: SecurityException) {
            appendLog(
                level = DiagnosticLogLevel.ERROR,
                rawMessage = "SecurityException opening GATT connection: ${se.message}",
            )
            _diagnostics.update {
                it.copy(
                    gattSession = it.gattSession.copy(
                        connectionState = WatchConnectionState.FAILED,
                        lastStatusNote = "Permission denied while opening GATT connection.",
                    ),
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerNextStandardRead(sessionId: Long, gatt: BluetoothGatt) {
        if (activeSessionId.get() != sessionId) return
        val next = synchronized(readQueue) { readQueue.pollFirst() } ?: return
        try {
            if (!gatt.readCharacteristic(next)) {
                appendLog(
                    level = DiagnosticLogLevel.WARN,
                    rawMessage = "Standard read skipped for ${GattUuidLookup.characteristicDisplayName(next.uuid.toString())}.",
                )
                triggerNextStandardRead(sessionId, gatt)
            }
        } catch (se: SecurityException) {
            appendLog(
                level = DiagnosticLogLevel.ERROR,
                rawMessage = "SecurityException on standard read: ${se.message}",
            )
        }
    }

    private fun handleStandardCharacteristicRead(
        sessionId: Long,
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        if (activeSessionId.get() != sessionId) return
        val uuid = characteristic.uuid.toString().lowercase()
        if (status == BluetoothGatt.GATT_SUCCESS && value.isNotEmpty()) {
            when (uuid) {
                GattUuidLookup.CHAR_BATTERY_LEVEL -> {
                    val percent = (value[0].toInt() and 0xFF).coerceIn(0, 100)
                    _diagnostics.update {
                        it.copy(
                            gattSession = it.gattSession.copy(
                                standardDeviceInfo = it.gattSession.standardDeviceInfo.copy(
                                    batteryPercent = percent,
                                ),
                            ),
                        )
                    }
                    appendLog(
                        level = DiagnosticLogLevel.INFO,
                        rawMessage = "Read standard Battery Service (0x2A19): $percent%.",
                    )
                }

                GattUuidLookup.CHAR_MANUFACTURER_NAME -> updateStandardStringField("Manufacturer", value) {
                    copy(manufacturerName = it)
                }

                GattUuidLookup.CHAR_MODEL_NUMBER -> updateStandardStringField("Model", value) {
                    copy(modelNumber = it)
                }

                GattUuidLookup.CHAR_FIRMWARE_REVISION -> updateStandardStringField("Firmware", value) {
                    copy(firmwareRevision = it)
                }

                GattUuidLookup.CHAR_HARDWARE_REVISION -> updateStandardStringField("Hardware", value) {
                    copy(hardwareRevision = it)
                }

                GattUuidLookup.CHAR_SOFTWARE_REVISION -> updateStandardStringField("Software", value) {
                    copy(softwareRevision = it)
                }
            }
        } else {
            appendLog(
                level = DiagnosticLogLevel.WARN,
                rawMessage = "Standard read returned status=$status for ${GattUuidLookup.characteristicDisplayName(uuid)}.",
            )
        }
        triggerNextStandardRead(sessionId, gatt)
    }

    private fun updateStandardStringField(
        label: String,
        value: ByteArray,
        updater: StandardDeviceInfo.(String) -> StandardDeviceInfo,
    ) {
        val decoded = value.toString(Charsets.UTF_8).trim('\u0000', ' ', '\n', '\r')
        if (decoded.isBlank()) return
        _diagnostics.update {
            it.copy(
                gattSession = it.gattSession.copy(
                    standardDeviceInfo = it.gattSession.standardDeviceInfo.updater(decoded),
                ),
            )
        }
        appendLog(
            level = DiagnosticLogLevel.INFO,
            rawMessage = "Read standard DIS $label: $decoded",
        )
    }

    override fun disconnectGatt() {
        connectJob?.cancel()
        connectTimeoutJob?.cancel()
        activeSessionId.incrementAndGet()
        closeActiveGatt(silent = false)
    }

    @SuppressLint("MissingPermission")
    private fun closeActiveGatt(silent: Boolean) {
        synchronized(readQueue) { readQueue.clear() }
        val gatt = activeGatt
        activeGatt = null
        if (gatt != null) {
            try {
                gatt.disconnect()
                gatt.close()
            } catch (_: Exception) {
            }
            if (!silent) {
                appendLog(
                    level = DiagnosticLogLevel.INFO,
                    rawMessage = "Closed diagnostic GATT session cleanly.",
                )
            }
        }
        if (!silent) {
            _diagnostics.update {
                it.copy(
                    gattSession = GattInspectionSession(
                        connectionState = WatchConnectionState.DISCONNECTED,
                        lastStatusNote = "Diagnostic session closed. Official Mi Fitness pairing preserved.",
                    ),
                )
            }
        }
    }

    override fun clearLogs() {
        _diagnostics.update { it.copy(logs = emptyList()) }
    }

    override fun buildRedactedDiagnosticReport(): String {
        val snapshot = _diagnostics.value
        return buildString {
            appendLine("# NothingWear BLE Diagnostic Report (Redacted)")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            appendLine("Bluetooth Adapter: ${snapshot.adapterStatus}")
            appendLine("Discovered/Bonded Devices: ${snapshot.devices.size}")
            snapshot.devices.take(15).forEach { device ->
                appendLine(
                    "- ${device.displayName} [${device.redactedAddress}] | ${device.transportType.label} | ${device.matchConfidence.label} | ${device.source.label} | RSSI=${device.rssi ?: "N/A"} | Bonded=${device.isBonded} | SysGATT=${device.isSystemConnected}",
                )
            }
            appendLine()
            val session = snapshot.gattSession
            appendLine("## GATT Inspection Session (Read-Only)")
            appendLine("Target: ${session.deviceName ?: "None"} [${session.redactedAddress ?: "N/A"}]")
            appendLine("State: ${session.connectionState}")
            appendLine("Note: ${session.lastStatusNote ?: "None"}")
            val info = session.standardDeviceInfo
            appendLine("Standard Battery (0x2A19): ${info.batteryPercent?.let { "$it%" } ?: "Unavailable"}")
            appendLine("DIS Manufacturer: ${info.manufacturerName ?: "Unavailable"}")
            appendLine("DIS Model: ${info.modelNumber ?: "Unavailable"}")
            appendLine("DIS Firmware: ${info.firmwareRevision ?: "Unavailable"}")
            appendLine("Services Discovered: ${session.services.size}")
            session.services.forEach { service ->
                appendLine("  * ${service.uuid} (${service.displayName})")
                service.characteristics.forEach { ch ->
                    appendLine("      - ${ch.uuid} [${ch.properties.joinToString(",")}] (${ch.displayName})")
                }
            }
            appendLine()
            appendLine("## Recent Logs (Redacted)")
            snapshot.logs.take(40).forEach { entry ->
                appendLine("[${entry.timestampLabel}] [${entry.level}] ${entry.message}")
            }
        }.let(LogRedactor::redactMessage)
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val record = result.scanRecord
        val manufacturerIds = buildSet {
            val sparse = record?.manufacturerSpecificData
            if (sparse != null) {
                for (index in 0 until sparse.size()) {
                    add(sparse.keyAt(index))
                }
            }
        }
        val serviceUuids = record?.serviceUuids.orEmpty().map { it.uuid.toString().lowercase() }
        val advertisedName = record?.deviceName
        upsertDeviceFromBluetoothDevice(
            device = device,
            explicitName = advertisedName,
            source = DiscoveredDeviceSource.BLE_SCAN,
            isSystemConnected = false,
            rssi = result.rssi,
            manufacturerIds = manufacturerIds,
            serviceUuids = serviceUuids,
            nowMillis = timeProvider.nowMillis(),
        )
        publishSortedDevices()
    }

    @SuppressLint("MissingPermission")
    private fun upsertDeviceFromBluetoothDevice(
        device: BluetoothDevice,
        explicitName: String? = null,
        source: DiscoveredDeviceSource,
        isSystemConnected: Boolean,
        rssi: Int?,
        manufacturerIds: Set<Int>,
        serviceUuids: List<String>,
        nowMillis: Long,
    ) {
        val address = device.address ?: return
        val deviceName = try {
            explicitName?.takeIf { it.isNotBlank() }
                ?: device.name?.takeIf { it.isNotBlank() }
        } catch (_: SecurityException) {
            explicitName?.takeIf { it.isNotBlank() }
        }
        val isBonded = try {
            device.bondState == BluetoothDevice.BOND_BONDED
        } catch (_: SecurityException) {
            false
        }
        val transportType = try {
            mapTransportType(device.type)
        } catch (_: SecurityException) {
            BluetoothTransportType.UNKNOWN
        }

        val existing = discoveredDevices[address]
        val mergedName = deviceName ?: existing?.displayName?.takeIf { it != "Unnamed BLE Device" }
        val mergedUuids = (existing?.advertisedServiceUuids.orEmpty() + serviceUuids).distinct()
        val match = WatchS4Identifier.classify(
            name = mergedName,
            manufacturerIds = manufacturerIds,
            advertisedServiceUuids = mergedUuids,
        )
        val betterMatch = if (existing != null && existing.matchConfidence.ordinal < match.confidence.ordinal) {
            existing.matchConfidence to existing.matchReason
        } else {
            match.confidence to match.reason
        }
        val mergedTransport = when {
            transportType != BluetoothTransportType.UNKNOWN -> transportType
            existing != null -> existing.transportType
            else -> BluetoothTransportType.UNKNOWN
        }

        discoveredDevices[address] = BleDeviceSummary(
            addressKey = address,
            redactedAddress = LogRedactor.redactMacAddress(address),
            displayName = mergedName ?: "Unnamed BLE Device",
            rssi = rssi ?: existing?.rssi,
            isBonded = isBonded || (existing?.isBonded == true),
            isSystemConnected = isSystemConnected || (existing?.isSystemConnected == true),
            transportType = mergedTransport,
            source = if (existing?.source == DiscoveredDeviceSource.SYSTEM_CONNECTED || isSystemConnected) {
                DiscoveredDeviceSource.SYSTEM_CONNECTED
            } else if (existing?.source == DiscoveredDeviceSource.BONDED || isBonded) {
                DiscoveredDeviceSource.BONDED
            } else {
                source
            },
            matchConfidence = betterMatch.first,
            matchReason = betterMatch.second,
            advertisedServiceUuids = mergedUuids,
            lastSeenAtMillis = nowMillis,
        )
    }

    private fun mapTransportType(type: Int): BluetoothTransportType = when (type) {
        BluetoothDevice.DEVICE_TYPE_LE -> BluetoothTransportType.LE
        BluetoothDevice.DEVICE_TYPE_DUAL -> BluetoothTransportType.DUAL
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> BluetoothTransportType.CLASSIC
        else -> BluetoothTransportType.UNKNOWN
    }

    private fun publishSortedDevices() {
        val sorted = discoveredDevices.values.sortedWith(
            compareBy<BleDeviceSummary> { it.matchConfidence.ordinal }
                .thenByDescending { it.isSystemConnected }
                .thenByDescending { it.isBonded }
                .thenByDescending { it.rssi ?: Int.MIN_VALUE }
                .thenBy { it.displayName.lowercase() },
        )
        _diagnostics.update { it.copy(devices = sorted) }
    }

    private fun appendLog(
        level: DiagnosticLogLevel,
        rawMessage: String,
    ) {
        val timestamp = Instant.ofEpochMilli(timeProvider.nowMillis())
            .atZone(timeProvider.zoneId())
            .format(logTimeFormatter)
        val entry = DiagnosticLogEntry(
            id = logSequence.getAndIncrement(),
            timestampLabel = timestamp,
            level = level,
            message = LogRedactor.redactMessage(rawMessage),
        )
        _diagnostics.update { state ->
            state.copy(logs = (listOf(entry) + state.logs).take(MAX_LOG_ENTRIES))
        }
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 15_000L
        const val RECONNECT_COOLDOWN_MS = 450L
        const val DIRECT_CONNECT_TIMEOUT_MS = 10_000L
        const val AUTO_ATTACH_TIMEOUT_MS = 12_000L
        const val MAX_LOG_ENTRIES = 120
    }
}
