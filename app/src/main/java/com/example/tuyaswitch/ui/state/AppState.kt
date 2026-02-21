package com.example.tuyaswitch.ui.state

import com.example.tuyaswitch.data.PairedDevice
import com.example.tuyaswitch.data.ScanDeviceItem

enum class Screen {
    LOGIN,
    HOME,
    SCAN,
    PAIR,
    CONTROL,
}

enum class PairMode {
    BLE_ONLY,
    BLE_WIFI,
}

data class AppState(
    val screen: Screen = Screen.LOGIN,
    val loading: Boolean = false,
    val message: String = "",
    val homeId: Long? = null,
    val scannedDevices: List<ScanDeviceItem> = emptyList(),
    val selectedScan: ScanDeviceItem? = null,
    val pairMode: PairMode = PairMode.BLE_ONLY,
    val pairedDevice: PairedDevice? = null,
    val switchOn: Boolean = false,
)
