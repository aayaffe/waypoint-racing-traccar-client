package `in`.avimarine.waypointracing.utils

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.waypointracing.BuildConfig
import `in`.avimarine.waypointracing.R

object RemoteConfig {
    private val configSettings = remoteConfigSettings {
        minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0L else 3600L
    }

    init {
        Firebase.remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        Firebase.remoteConfig.setConfigSettingsAsync(configSettings)
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "Config params updated: ${it.result}")
            } else {
                Log.d(TAG, "Config params update failed")
            }
        }
        Firebase.remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate : ConfigUpdate) {
                Log.d(TAG, "Updated keys: " + configUpdate.updatedKeys);
                Firebase.remoteConfig.activate()
            }

            override fun onError(error : FirebaseRemoteConfigException) {
                Log.w(TAG, "Config update error with code: " + error.code, error)
            }
        })
    }

    fun getBool(key: String): Boolean {
        return Firebase.remoteConfig.getBoolean(key)
    }

    fun getLong(key: String): Long {
        return Firebase.remoteConfig.getLong(key)
    }
}