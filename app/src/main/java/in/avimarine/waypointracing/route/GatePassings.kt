package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.activities.SettingsFragment
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class GatePassings {

    var eventId : String = ""
    val passes = arrayListOf<GatePassing>()

    constructor(id: String) {
        eventId = id
    }

    fun toJson():String{
        return Json.encodeToString(this)
    }

    companion object {
        fun fromJson(s: String): GatePassings{
            return Json.decodeFromString(s)
        }

        fun addGatePass(context: Context, gp: GatePassing) {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            val gps = sharedPreferences.getString(SettingsFragment.KEY_GATE_PASSES,"")?.let {
                try {
                    fromJson(
                        it
                    )
                }catch (e: Exception){
                    Log.d(TAG, "Error loading gate passes")
                    GatePassings(gp.routeId)
                }
            }
            gps?.passes?.add(gp)
            with(sharedPreferences.edit()) {
                putString(SettingsFragment.KEY_GATE_PASSES, gps?.toJson())
                commit()
            }
        }

        fun getLastGatePass(context: Context): GatePassing? {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            val gps = sharedPreferences.getString(SettingsFragment.KEY_GATE_PASSES,"")?.let {
                try {
                    fromJson(
                        it
                    )
                }catch (e: Exception){
                    Log.d(TAG, "Error loading gate passes")
                    return null
                }
            }
            if (gps?.passes?.size!! > 0)
                return gps.passes.last()
            return null
        }

        fun reset(context: Context, route: Route) {
            val gps = GatePassings(route.id)
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            with(sharedPreferences.edit()) {
                putString(
                    SettingsFragment.KEY_GATE_PASSES,
                    gps.toJson()
                )
                commit()
            }

        }
    }

}