package com.example.tuyaswitch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tuyaswitch.data.ScanDeviceItem
import com.example.tuyaswitch.data.TuyaRepository
import com.example.tuyaswitch.ui.state.AppState
import com.example.tuyaswitch.ui.state.PairMode
import com.example.tuyaswitch.ui.state.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(private val repository: TuyaRepository) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var scanJob: Job? = null

    init {
        repository.initTuya()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            setLoading(true)
            runCatching { repository.login(email, password) }
                .onSuccess {
                    _state.update { it.copy(screen = Screen.HOME, loading = false, message = "Login successful") }
                }
                .onFailure { onError("Login failed: ${it.message}") }
        }
    }

    fun getOrCreateHome() {
        viewModelScope.launch {
            setLoading(true)
            runCatching { repository.getOrCreateHome() }
                .onSuccess { home ->
                    _state.update {
                        it.copy(
                            screen = Screen.SCAN,
                            loading = false,
                            homeId = home.homeId,
                            message = "Using homeId=${home.homeId} (${home.name})"
                        )
                    }
                }
                .onFailure { onError("Home error: ${it.message}") }
        }
    }

    fun startScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            repository.startBleScan().collect { devices ->
                _state.update { it.copy(scannedDevices = devices, message = "Found ${devices.size} devices") }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        repository.stopBleScan()
    }

    fun selectDevice(scanDeviceItem: ScanDeviceItem, mode: PairMode) {
        _state.update { it.copy(selectedScan = scanDeviceItem, pairMode = mode, screen = Screen.PAIR) }
    }

    fun pairSelected(ssid: String, password: String) {
        val selected = _state.value.selectedScan ?: return
        val homeId = _state.value.homeId ?: return

        viewModelScope.launch {
            setLoading(true)
            val op = if (_state.value.pairMode == PairMode.BLE_ONLY) {
                runCatching { repository.pairBleOnly(selected, homeId) }
            } else {
                runCatching { repository.pairBleWifiCombo(selected, homeId, ssid, password) }
            }

            op.onSuccess { device ->
                _state.update {
                    it.copy(
                        loading = false,
                        pairedDevice = device,
                        message = "Pair success: ${device.name}",
                        screen = Screen.CONTROL,
                    )
                }
            }.onFailure {
                onError("Pair failed: ${it.message}")
            }
        }
    }

    fun toggleSwitch(isOn: Boolean) {
        val device = _state.value.pairedDevice ?: return
        viewModelScope.launch {
            setLoading(true)
            runCatching { repository.toggleSwitch(device.deviceId, isOn) }
                .onSuccess {
                    _state.update { it.copy(loading = false, switchOn = isOn, message = "switch_1=$isOn") }
                }
                .onFailure { onError("Toggle failed: ${it.message}") }
        }
    }

    fun backToScan() {
        _state.update { it.copy(screen = Screen.SCAN) }
    }

    private fun onError(text: String) {
        _state.update { it.copy(loading = false, message = text) }
    }

    private fun setLoading(loading: Boolean) {
        _state.update { it.copy(loading = loading) }
    }

    class Factory(private val repository: TuyaRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
    }
}
