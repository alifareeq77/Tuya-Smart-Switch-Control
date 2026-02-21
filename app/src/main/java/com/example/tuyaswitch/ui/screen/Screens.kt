package com.example.tuyaswitch.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tuyaswitch.data.PairedDevice
import com.example.tuyaswitch.data.ScanDeviceItem
import com.example.tuyaswitch.ui.state.PairMode

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, loading: Boolean, message: String) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Tuya Login", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onLogin(email, password) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }
        StatusArea(loading, message)
    }
}

@Composable
fun HomeScreen(onCreateOrSelectHome: () -> Unit, loading: Boolean, message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Home / Space", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Create or select your first home to get relationId(homeId).")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCreateOrSelectHome, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text("Get or Create Home")
        }
        StatusArea(loading, message)
    }
}

@Composable
fun ScanScreen(
    devices: List<ScanDeviceItem>,
    loading: Boolean,
    message: String,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onSelect: (ScanDeviceItem, PairMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("BLE Scan", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Put device into pairing mode (fast/slow blink), keep Bluetooth ON, and stay nearby.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartScan, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Start Scan") }
            Button(onClick = onStopScan, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Stop Scan") }
        }
        StatusArea(loading, message)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(devices) { item ->
                DeviceRow(item = item, onSelect = onSelect)
            }
        }
    }
}

@Composable
private fun DeviceRow(item: ScanDeviceItem, onSelect: (ScanDeviceItem, PairMode) -> Unit) {
    var mode by remember { mutableStateOf(PairMode.BLE_ONLY) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(item.name, fontWeight = FontWeight.Bold)
        Text("id=${item.id} pid=${item.productId ?: "-"}")
        Row {
            RadioButton(selected = mode == PairMode.BLE_ONLY, onClick = { mode = PairMode.BLE_ONLY })
            Text("BLE only")
            Spacer(Modifier.height(0.dp).weight(1f))
            RadioButton(selected = mode == PairMode.BLE_WIFI, onClick = { mode = PairMode.BLE_WIFI })
            Text("BLE + Wi-Fi")
        }
        Button(onClick = { onSelect(item, mode) }) { Text("Pair This Device") }
    }
}

@Composable
fun PairScreen(
    mode: PairMode,
    loading: Boolean,
    message: String,
    onPair: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    var ssid by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Pair Device", style = MaterialTheme.typography.headlineSmall)
        Text("Selected mode: $mode")
        if (mode == PairMode.BLE_WIFI) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = ssid, onValueChange = { ssid = it }, label = { Text("Wi-Fi SSID (2.4GHz)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Wi-Fi Password") }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onPair(ssid, pass) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text("Start Pairing")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        StatusArea(loading, message)
    }
}

@Composable
fun ControlScreen(device: PairedDevice?, isOn: Boolean, loading: Boolean, message: String, onToggle: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Control", style = MaterialTheme.typography.headlineSmall)
        Text("Device: ${device?.name ?: "N/A"}")
        Text("deviceId: ${device?.deviceId ?: "N/A"}")
        Spacer(Modifier.height(16.dp))
        Row {
            Text("switch_1")
            Switch(checked = isOn, enabled = !loading, onCheckedChange = onToggle)
        }
        StatusArea(loading, message)
    }
}

@Composable
private fun StatusArea(loading: Boolean, message: String) {
    Spacer(Modifier.height(12.dp))
    if (loading) CircularProgressIndicator()
    if (message.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(message)
    }
}
