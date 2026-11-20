package com.nothingwear.app.core.bluetooth

/**
 * Translates common Android BluetoothGatt / HCI status codes into clear, honest
 * diagnostic explanations for M2 BLE troubleshooting.
 */
object GattStatusDecoder {

    fun describeStatus(statusCode: Int): String = when (statusCode) {
        0 -> "GATT_SUCCESS (0)"
        8 -> "GATT_CONN_TIMEOUT (8) — watch did not respond in time or went out of range"
        19 -> "GATT_CONN_TERMINATE_PEER_USER (19) — Watch S4 closed the secondary GATT connection (common when Mi Fitness holds the active BLE slot)"
        22 -> "GATT_CONN_TERMINATE_LOCAL_HOST (22) — phone Bluetooth stack closed the connection"
        62 -> "GATT_CONN_FAIL_ESTABLISH (62) — BLE link failed to establish; watch may not be advertising while connected to Mi Fitness"
        133 -> "GATT_ERROR (133) — Android BLE stack error (often caused by rapid re-connect, BR/EDR-only entry, or Mi Fitness occupying the single BLE peripheral link)"
        257 -> "GATT_FAILURE (257) — generic GATT stack failure"
        else -> "GATT status $statusCode"
    }

    fun shouldFallbackToAutoAttach(statusCode: Int): Boolean =
        statusCode == 133 || statusCode == 8 || statusCode == 62 || statusCode == 257
}
