package com.example.tuyaswitch.data

data class HomeInfo(
    val homeId: Long,
    val name: String,
)

data class ScanDeviceItem(
    val id: String,
    val name: String,
    val productId: String?,
    val raw: Any,
)

data class PairedDevice(
    val deviceId: String,
    val name: String,
)
