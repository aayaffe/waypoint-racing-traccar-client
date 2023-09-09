package `in`.avimarine.waypointracing.utils

import android.content.SharedPreferences
import android.util.Log
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.route.Route
import org.json.JSONException

class RouteParser {
    companion object{
        fun parseRoute(sp: SharedPreferences): Route {
            val s = sp.getString(SettingsFragment.KEY_ROUTE, null)
            return if (s!= null) {
                try {
                    Route.fromString(s)
                } catch (e: JSONException){
                    Log.e(TAG,"Error parsing route", e)
                    Route.emptyRoute()
                }
            } else {
                Log.e(TAG,"Error loading route from sharedpreferences (null)")
                Route.emptyRoute()
            }
        }
    }

}