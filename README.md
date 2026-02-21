# Tuya Smart Switch Control (Android, Kotlin, Compose)

A sample app that logs into Tuya Smart Life SDK, prepares a home, scans BLE devices, pairs via BLE-only or BLE+Wi-Fi combo activation, then toggles `switch_1`.

## Configure SDK keys
Edit `TuyaConfig.kt` and replace placeholders:
- `TUYA_APP_KEY = "YOUR_TUYA_APP_KEY"`
- `TUYA_APP_SECRET = "YOUR_TUYA_APP_SECRET"`

## Build notes
- This project uses Gradle Kotlin DSL and Jetpack Compose.
- **TODO:** Confirm Tuya/Thingclips dependency coordinates and versions in `app/build.gradle.kts` for your account/region.

## Run flow
1. Login with email + password.
2. Tap **Get or Create Home** to obtain a homeId (relationId).
3. Grant BLE/location permissions and enable location services + Bluetooth.
4. Start scan and select a device.
5. Choose BLE-only or BLE+Wi-Fi mode.
6. Pair and then toggle `switch_1` ON/OFF.

## Troubleshooting
- If device doesn’t appear, check permissions/location/BT.
- If pairing fails, confirm device supports BLE or BLE+Wi-Fi.
