package `in`.avimarine.waypointracing.utils

import `in`.avimarine.waypointracing.database.Boat
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.firebase.firestore.ktx.toObject
import `in`.avimarine.androidutils.TAG

class Auth {
    companion object {
        fun loadSettingsFromServer(uid: String, context: Context) {
            FirestoreDatabase.getBoat(uid, {
                if (it != null) {
                    val boat = it.toObject<Boat>()
                    if (boat != null) {
                        val sharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(context)
                        with(sharedPreferences.edit()) {
                            putString(SettingsFragment.KEY_BOAT_NAME, boat.name)
                            commit()
                        }
                    }
                }
            }, {
                Log.w(TAG, "Failed to load boat", it)
            }
            )
        }

    }

}