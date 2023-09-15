package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.activities.SettingsFragment
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import `in`.avimarine.androidutils.TAG
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class GatePassings {

    var eventId: String = ""
    val passes = arrayListOf<GatePassing>()

    constructor(id: String) {
        eventId = id
    }

    fun toJson(): String {
        return Json.encodeToString(this)
    }
    private fun filterByRouteId(routeId: String) {
        passes.retainAll { gp -> gp.routeId == routeId }
    }
    private fun filterAllButRouteId(routeId: String) {
        passes.retainAll { gp -> gp.routeId != routeId }
    }

    fun getLatestGatePassForGate(id: Int): GatePassing? {
        val thisGps = passes.filter{ it.gateId == id}
        return thisGps.maxByOrNull { it.time.time }
    }

    companion object {
        fun getCurrentRouteGatePassings(appContext: Context, routeId: String = ""): GatePassings {
            val gatePassings = getGatePassings(appContext, routeId)
            if (routeId.isNotEmpty()) {
                gatePassings.filterByRouteId(routeId)
            }
            return gatePassings
        }

        private fun getGatePassings(appContext: Context, routeId: String = ""): GatePassings {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext.applicationContext)
            val s = sharedPreferences.getString(SettingsFragment.KEY_GATE_PASSES, "") ?: ""
            val gatePassings = try {
                fromJson(s)
            } catch (e: Exception) {
                Log.d(TAG, "Failed to load gate passings", e)
                GatePassings(routeId)
            }
            return gatePassings
        }


        private fun fromJson(s: String): GatePassings {
            return Json.decodeFromString(s)
        }

        fun addGatePass(context: Context, gp: GatePassing) {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            val gps = getGatePassings(context, gp.routeId)
            gps.passes.removeAll{ item -> (item.routeId == gp.routeId) && (item.gateId == gp.gateId) }
            gps.passes.add(gp)
            with(sharedPreferences.edit()) {
                putString(SettingsFragment.KEY_GATE_PASSES, gps.toJson())
                commit()
            }
        }

        fun getLastGatePass(context: Context, routeId: String): GatePassing? {
            val gps = getCurrentRouteGatePassings(context,routeId)
            if (gps.passes.size > 0)
                return gps.passes.last()
            return null
        }

        fun reset(context: Context, route: Route, resetCurrentRouteOnly: Boolean = true) {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            val gps = if (resetCurrentRouteOnly) {
                val temp = getCurrentRouteGatePassings(context)
                temp.filterAllButRouteId(route.id)
                temp
            } else {
                GatePassings(route.id)
            }
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