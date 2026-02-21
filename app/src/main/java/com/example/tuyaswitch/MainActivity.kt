package com.example.tuyaswitch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuyaswitch.data.TuyaRepository
import com.example.tuyaswitch.tuya.TuyaService
import com.example.tuyaswitch.ui.screen.ControlScreen
import com.example.tuyaswitch.ui.screen.HomeScreen
import com.example.tuyaswitch.ui.screen.LoginScreen
import com.example.tuyaswitch.ui.screen.PairScreen
import com.example.tuyaswitch.ui.screen.ScanScreen
import com.example.tuyaswitch.ui.state.Screen
import com.example.tuyaswitch.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(TuyaRepository(TuyaService(context.applicationContext)))
    )
    val state by viewModel.state.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        val permissions = requiredBlePermissions()
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    when {
        !isLocationEnabled(context) -> {
            // BLE scan often requires location services enabled in Android settings,
            // even if runtime permissions are granted.
            Text("Please enable Location services and Bluetooth, then restart scan.")
        }

        else -> {
            when (state.screen) {
                Screen.LOGIN -> LoginScreen(viewModel::login, state.loading, state.message)
                Screen.HOME -> HomeScreen(viewModel::getOrCreateHome, state.loading, state.message)
                Screen.SCAN -> ScanScreen(
                    devices = state.scannedDevices,
                    loading = state.loading,
                    message = state.message,
                    onStartScan = viewModel::startScan,
                    onStopScan = viewModel::stopScan,
                    onSelect = viewModel::selectDevice,
                )

                Screen.PAIR -> PairScreen(
                    mode = state.pairMode,
                    loading = state.loading,
                    message = state.message,
                    onPair = viewModel::pairSelected,
                    onBack = viewModel::backToScan,
                )

                Screen.CONTROL -> ControlScreen(
                    device = state.pairedDevice,
                    isOn = state.switchOn,
                    loading = state.loading,
                    message = state.message,
                    onToggle = viewModel::toggleSwitch,
                )
            }
        }
    }
}

private fun requiredBlePermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
}

private fun isLocationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}
