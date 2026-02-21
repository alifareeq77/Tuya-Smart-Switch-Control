package com.example.tuyaswitch.tuya

import android.content.Context
import android.util.Log
import com.example.tuyaswitch.data.HomeInfo
import com.example.tuyaswitch.data.PairedDevice
import com.example.tuyaswitch.data.ScanDeviceItem
import com.thingclips.smart.activator.core.ThingActivatorCoreKit
import com.thingclips.smart.activator.core.bean.ThingDeviceActiveBuilder
import com.thingclips.smart.activator.core.bean.ThingDeviceBlueActiveTypeEnum
import com.thingclips.smart.activator.core.bean.ThingSearchDeviceBean
import com.thingclips.smart.activator.core.enums.ScanType
import com.thingclips.smart.activator.core.listener.IThingDeviceActiveListener
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.bean.scene.PlaceInfoBean
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.home.sdk.callback.IThingSmartHomeDataCallback
import com.thingclips.smart.home.sdk.callback.ILoginCallback
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.thingclips.smart.sdk.api.IThingDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TuyaService(private val context: Context) {

    fun initTuya() {
        // SDK init is done in Application. Keep this entry point for explicit app-layer call.
        Log.d("TuyaService", "initTuya called")
    }

    suspend fun login(email: String, password: String): Unit = suspendCancellableCoroutine { cont ->
        // TODO: regionCode/countryCode may be required by your account setup.
        ThingHomeSdk.getUserInstance().loginWithEmail("1", email, password, object : ILoginCallback {
            override fun onSuccess(user: com.thingclips.smart.home.sdk.bean.User?) {
                Log.i("TuyaService", "login success: ${user?.uid}")
                cont.resume(Unit)
            }

            override fun onError(code: String?, error: String?) {
                cont.resumeWithException(IllegalStateException("Login failed [$code]: $error"))
            }
        })
    }

    suspend fun getOrCreateHome(): HomeInfo = suspendCancellableCoroutine { cont ->
        ThingHomeSdk.getHomeManagerInstance().queryHomeList(object : IThingGetHomeListCallback {
            override fun onSuccess(homeBeans: MutableList<HomeBean>?) {
                val existing = homeBeans?.firstOrNull()
                if (existing != null) {
                    cont.resume(HomeInfo(existing.homeId, existing.name ?: "Default Home"))
                } else {
                    createDefaultHome(cont)
                }
            }

            override fun onError(errorCode: String?, error: String?) {
                cont.resumeWithException(IllegalStateException("Home list error [$errorCode]: $error"))
            }
        })
    }

    private fun createDefaultHome(cont: kotlin.coroutines.Continuation<HomeInfo>) {
        val geo = PlaceInfoBean().apply {
            name = "My Home"
            rooms = mutableListOf("Living Room")
        }
        ThingHomeSdk.getHomeManagerInstance().createHome(
            "My Home",
            0.0,
            0.0,
            "",
            listOf("Living Room"),
            object : IThingHomeResultCallback {
                override fun onSuccess(bean: HomeBean?) {
                    if (bean == null) {
                        cont.resumeWithException(IllegalStateException("createHome returned null"))
                        return
                    }
                    cont.resume(HomeInfo(bean.homeId, bean.name ?: "My Home"))
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    cont.resumeWithException(IllegalStateException("Create home failed [$errorCode]: $errorMsg"))
                }
            }
        )
    }

    fun startBleScan(): Flow<List<ScanDeviceItem>> = callbackFlow {
        val manager = ThingActivatorCoreKit.getScanDeviceManager()
        val listener = object : IThingDataCallback<List<ThingSearchDeviceBean>> {
            override fun onSuccess(result: List<ThingSearchDeviceBean>?) {
                val items = result.orEmpty().map {
                    ScanDeviceItem(
                        id = it.uuid ?: it.devId ?: it.mac ?: "unknown",
                        name = it.name ?: "Unnamed Device",
                        productId = it.productId,
                        raw = it
                    )
                }
                trySend(items)
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                close(IllegalStateException("Scan error [$errorCode]: $errorMessage"))
            }
        }

        // Required API pattern from Tuya docs.
        manager.startBlueToothDeviceSearch(ScanType.SINGLE, listener)

        awaitClose {
            manager.stopBlueToothDeviceSearch()
        }
    }

    fun stopBleScan() {
        ThingActivatorCoreKit.getScanDeviceManager().stopBlueToothDeviceSearch()
    }

    suspend fun pairBleOnly(scanBean: ScanDeviceItem, homeId: Long): PairedDevice =
        startPair(scanBean, homeId, ThingDeviceBlueActiveTypeEnum.SINGLE_BLE, null, null)

    suspend fun pairBleWifiCombo(
        scanBean: ScanDeviceItem,
        homeId: Long,
        ssid: String,
        password: String,
    ): PairedDevice = startPair(scanBean, homeId, ThingDeviceBlueActiveTypeEnum.BLE_WIFI, ssid, password)

    private suspend fun startPair(
        scanBean: ScanDeviceItem,
        homeId: Long,
        activeType: ThingDeviceBlueActiveTypeEnum,
        ssid: String?,
        password: String?,
    ): PairedDevice = suspendCancellableCoroutine { cont ->
        val bean = scanBean.raw as? ThingSearchDeviceBean
            ?: run {
                cont.resumeWithException(IllegalArgumentException("Invalid scan bean type"))
                return@suspendCancellableCoroutine
            }

        val builder = ThingDeviceActiveBuilder()
            .setContext(context)
            .setSearchDeviceBeans(listOf(bean))
            .setHomeId(homeId)
            .setTimeOut(120)
            .setActiveModel(activeType)

        if (activeType == ThingDeviceBlueActiveTypeEnum.BLE_WIFI) {
            // For combo devices: provide router credentials.
            builder.setSsid(ssid)
            builder.setPassword(password)
        }

        // Required API pattern from Tuya docs.
        ThingActivatorCoreKit.getActiveManager()
            .newThingActiveManager()
            .startActive(builder, object : IThingDeviceActiveListener {
                override fun onFind(thingSearchDeviceBean: ThingSearchDeviceBean?) {
                    Log.d("TuyaService", "onFind: ${thingSearchDeviceBean?.name}")
                }

                override fun onBind(thingSearchDeviceBean: ThingSearchDeviceBean?) {
                    Log.d("TuyaService", "onBind: ${thingSearchDeviceBean?.name}")
                }

                override fun onActiveSuccess(devResp: com.thingclips.smart.activator.core.bean.ThingActivatorDeviceRespBean?) {
                    val deviceId = devResp?.devId ?: ""
                    if (deviceId.isBlank()) {
                        cont.resumeWithException(IllegalStateException("Pair success callback without deviceId"))
                        return
                    }
                    cont.resume(PairedDevice(deviceId, devResp.name ?: "Tuya Switch"))
                }

                override fun onActiveError(errorCode: String?, errorMsg: String?) {
                    cont.resumeWithException(IllegalStateException("Pair failed [$errorCode]: $errorMsg"))
                }

                override fun onStep(step: String?, data: Any?) {
                    Log.d("TuyaService", "pair step=$step data=$data")
                }
            })
    }

    suspend fun toggleSwitch(deviceId: String, isOn: Boolean): Unit = suspendCancellableCoroutine { cont ->
        val device: IThingDevice = ThingHomeSdk.newDeviceInstance(deviceId)
        val dps = "{\"switch_1\":$isOn}"
        device.publishDps(dps, object : IResultCallback {
            override fun onError(code: String?, error: String?) {
                cont.resumeWithException(IllegalStateException("DP publish failed [$code]: $error"))
            }

            override fun onSuccess() {
                cont.resume(Unit)
            }
        })
    }
}
