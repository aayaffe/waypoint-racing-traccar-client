package `in`.avimarine.waypointracing.utils

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.waypointracing.BuildConfig
import `in`.avimarine.waypointracing.R

class RemoteConfig {
    //A class to setup firebase remote config and get the values
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
    private val configSettings = remoteConfigSettings {
        minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0L else 3600L
    }

    init {
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "Config params updated: ${it.result}")
            } else {
                Log.d(TAG, "Config params update failed")
            }
        }
    }

    fun getBool(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }
}