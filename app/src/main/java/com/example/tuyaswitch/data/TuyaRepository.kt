package com.example.tuyaswitch.data

import com.example.tuyaswitch.tuya.TuyaService
import kotlinx.coroutines.flow.Flow

class TuyaRepository(
    private val service: TuyaService,
) {
    fun initTuya() = service.initTuya()

    suspend fun login(email: String, pass: String) = service.login(email, pass)

    suspend fun getOrCreateHome(): HomeInfo = service.getOrCreateHome()

    fun startBleScan(): Flow<List<ScanDeviceItem>> = service.startBleScan()

    fun stopBleScan() = service.stopBleScan()

    suspend fun pairBleOnly(scanBean: ScanDeviceItem, homeId: Long): PairedDevice =
        service.pairBleOnly(scanBean, homeId)

    suspend fun pairBleWifiCombo(
        scanBean: ScanDeviceItem,
        homeId: Long,
        ssid: String,
        password: String,
    ): PairedDevice = service.pairBleWifiCombo(scanBean, homeId, ssid, password)

    suspend fun toggleSwitch(deviceId: String, isOn: Boolean) = service.toggleSwitch(deviceId, isOn)
}
