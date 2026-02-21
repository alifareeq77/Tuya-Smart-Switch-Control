package com.example.tuyaswitch

import android.app.Application
import android.util.Log
import com.example.tuyaswitch.tuya.TuyaConfig
import com.thingclips.smart.home.sdk.ThingHomeSdk

class TuyaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Smart Life App SDK initialization pattern.
        ThingHomeSdk.init(this, TuyaConfig.TUYA_APP_KEY, TuyaConfig.TUYA_APP_SECRET)
        ThingHomeSdk.setDebugMode(true)
        Log.i("TuyaApp", "ThingHomeSdk initialized")
    }
}
