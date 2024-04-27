package `in`.avimarine.waypointracing.utils

import android.content.SharedPreferences
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.route.Route

class Preferences (val sharedPreferences: SharedPreferences){

    /**
     * The status of race tracking. True if in active tracking, false otherwise
     */
    var status: Boolean
        get() = sharedPreferences.getBoolean(SettingsFragment.KEY_STATUS, false)
        set(value) = sharedPreferences.edit().putBoolean(SettingsFragment.KEY_STATUS, value).apply()
    /**
     * The status of continuous position tracking. True if in active tracking, false otherwise
     */
    var tracking: Boolean
        get() = sharedPreferences.getBoolean(SettingsFragment.KEY_TRACKING, false)
        set(value) = sharedPreferences.edit().putBoolean(SettingsFragment.KEY_TRACKING, value).apply()

    /**
     * Are we in expert mode? this mode allows the display of advanced settings.
     */
    var expertMode: Boolean
        get() = sharedPreferences.getBoolean(SettingsFragment.KEY_EXPERT_MODE, false)
        set(value) = sharedPreferences.edit().putBoolean(SettingsFragment.KEY_EXPERT_MODE, value).apply()

    /**
     * This the version of the loaded route. It is used to check if the route has been updated
     */
    var routeUpdatedVersion: Long
        get() = sharedPreferences.getLong(SettingsFragment.KEY_ROUTE_UPDATED_VERSION, 0)
        set(value) = sharedPreferences.edit().putLong(SettingsFragment.KEY_ROUTE_UPDATED_VERSION, value).apply()

    /**
     * This is the currently loaded route. It is stored as a string.
     */
    var currentRoute: String
        get() = sharedPreferences.getString(SettingsFragment.KEY_ROUTE, Route.emptyRoute().toString())!!
        set(value) = sharedPreferences.edit().putString(SettingsFragment.KEY_ROUTE, value).apply()

    /**
     * This is the name of the boat.
     */
    var boatName: String
        get() = sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "Undefined")!!
        set(value) = sharedPreferences.edit().putString(SettingsFragment.KEY_BOAT_NAME, value).apply()

    /**
     * The list location (in the route elements) of the selected waypoint
     */

    var nextWpt: Int
        get() = sharedPreferences.getInt(SettingsFragment.KEY_NEXT_WPT, 0)
        set(value) = sharedPreferences.edit().putInt(SettingsFragment.KEY_NEXT_WPT, value).apply()

    /**
     * The initial interval of GPS polling
     */
    var initialGPSInterval: String
        get() = sharedPreferences.getString(SettingsFragment.KEY_INITIAL_INTERVAL, "30")!!
        set(value) = sharedPreferences.edit().putString(SettingsFragment.KEY_INITIAL_INTERVAL, value).apply()

    /**
     * The current interval of GPS polling
     */
    var GPSInterval: String
        get() = sharedPreferences.getString(SettingsFragment.KEY_INTERVAL, "600")!!
        set(value) = sharedPreferences.edit().putString(SettingsFragment.KEY_INTERVAL, value).apply()

    /**
     * Is The app UI currently visible
     */
    var uiVisible: Boolean
        get() = sharedPreferences.getBoolean(SettingsFragment.KEY_IS_UI_VISIBLE, true)
        set(value) = sharedPreferences.edit().putBoolean(SettingsFragment.KEY_IS_UI_VISIBLE, value).apply()

    /**
     * The time of last location sending time
     */
    var lastSend: Long
        get() = sharedPreferences.getLong(SettingsFragment.KEY_LAST_SEND, Long.MAX_VALUE)
        set(value) = sharedPreferences.edit().putLong(SettingsFragment.KEY_LAST_SEND, value).apply()


    var wakeLock: Boolean
        get() = sharedPreferences.getBoolean(SettingsFragment.KEY_WAKELOCK, true)
        set(value) = sharedPreferences.edit().putBoolean(SettingsFragment.KEY_WAKELOCK, value).apply()

}