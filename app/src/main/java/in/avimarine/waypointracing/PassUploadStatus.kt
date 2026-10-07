package `in`.avimarine.waypointracing

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import `in`.avimarine.waypointracing.route.GatePassing

object PassUploadStatus {
    private const val KEY_PREFIX = "pass_upload_status:"

    enum class State { PENDING, UPLOADED, FAILED }

    fun token(pass: GatePassing): String =
        "${pass.routeId}:${pass.gateId}:${pass.time.time}:${pass.sourceEventId}"

    fun key(pass: GatePassing): String = KEY_PREFIX + token(pass)

    fun isStatusKey(key: String?): Boolean = key?.startsWith(KEY_PREFIX) == true

    fun get(context: Context, pass: GatePassing): State {
        val value = PreferenceManager.getDefaultSharedPreferences(context).getString(key(pass), null)
        return runCatching { State.valueOf(value ?: "") }.getOrDefault(State.PENDING)
    }

    fun set(context: Context, pass: GatePassing, state: State) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(key(pass), state.name)
        }
    }
}
