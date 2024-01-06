package `in`.avimarine.waypointracing.utils

import android.content.SharedPreferences
import android.util.Log
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.route.Route
import org.json.JSONException

class RouteParser {
    companion object{

        fun parseRoute(routeJson: String): Route {
            return try {
                    Route.fromString(routeJson)
                } catch (e: JSONException){
                    Log.e(TAG,"Error parsing route", e)
                    Route.emptyRoute()
                }
        }
    }

}