package `in`.avimarine.waypointracing.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.PreferenceManager
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.toObject
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.database.Boat
import `in`.avimarine.waypointracing.database.FirestoreDatabase

class Auth {

    companion object {
        fun launchAuthenticationProcess(signInLauncher: ActivityResultLauncher<Intent>) {
            if (FirebaseAuth.getInstance().currentUser != null) {
                return
            }
            // Choose authentication providers
            val providers = arrayListOf(
                AuthUI.IdpConfig.GoogleBuilder().build()
            )

            // Create and launch sign-in intent
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
            signInLauncher.launch(signInIntent)
        }

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