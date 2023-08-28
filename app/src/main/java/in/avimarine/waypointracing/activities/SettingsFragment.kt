/*
 * Copyright 2012 - 2020 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.MainApplication
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.Toast
import androidx.preference.*
import com.google.firebase.auth.FirebaseAuth
import `in`.avimarine.androidutils.TAG
import java.util.*

class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private var expertPressed = 0
    private var sharedPreferences: SharedPreferences? = null
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            requireContext()
        )
        setPreferencesFromResource(R.xml.preferences, rootKey)
        initPreferences()
        findPreference<Preference>(KEY_URL)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            newValue != null && validateServerURL(newValue.toString())
        }
        findPreference<Preference>(KEY_INITIAL_INTERVAL)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                try {
                    newValue != null && (newValue as String).toInt() > 0
                } catch (e: NumberFormatException) {
                    Log.w(TAG, e)
                    false
                }
            }
        val numberValidationListener = Preference.OnPreferenceChangeListener { _, newValue ->
            try {
                newValue != null && (newValue as String).toInt() >= 0
            } catch (e: NumberFormatException) {
                Log.w(TAG, e)
                false
            }
        }
        val nonEmptyStringValidationListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? -> newValue != null && newValue != "" }
        findPreference<Preference>(KEY_DEVICE)!!.onPreferenceChangeListener =
            nonEmptyStringValidationListener
        findPreference<Preference>(KEY_BOAT_NAME)!!.onPreferenceChangeListener =
            nonEmptyStringValidationListener
        findPreference<Preference>(KEY_DISTANCE)!!.onPreferenceChangeListener =
            numberValidationListener
        findPreference<Preference>(KEY_ANGLE)!!.onPreferenceChangeListener =
            numberValidationListener
        findPreference<Preference>(KEY_EXPERT_MODE)!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                expertMode(newValue as Boolean?)
                true
            }

        findPreference<Preference>(KEY_EXPERT_MODE)?.setOnPreferenceClickListener {
            Log.d(TAG, "Pressed Expert")
            if (expertPressed == 7) {
                setTestMode(true)
            } else {
                expertPressed+=1
            }
            true
        }
        setTestMode(false)
    }

    private fun setTestMode(b: Boolean) {
        findPreference<Preference>(KEY_ACCURACY)!!.isVisible = b
        findPreference<Preference>(KEY_ANGLE)!!.isVisible = b
        findPreference<Preference>(KEY_DISTANCE)!!.isVisible = b
        findPreference<Preference>(KEY_INITIAL_INTERVAL)!!.isVisible = b
        findPreference<Preference>(KEY_ADAPTIVE_INTERVAL)!!.isVisible = b
    }

    class NumericEditTextPreferenceDialogFragment : EditTextPreferenceDialogFragmentCompat() {
        override fun onBindDialogView(view: View) {
            val editText = view.findViewById<EditText>(android.R.id.edit)
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            super.onBindDialogView(view)
        }

        companion object {
            fun newInstance(key: String?): NumericEditTextPreferenceDialogFragment {
                val fragment = NumericEditTextPreferenceDialogFragment()
                val bundle = Bundle()
                bundle.putString(ARG_KEY, key)
                fragment.arguments = bundle
                return fragment
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (listOf(KEY_INITIAL_INTERVAL, KEY_DISTANCE, KEY_ANGLE).contains(preference.key)) {
            val f: EditTextPreferenceDialogFragmentCompat =
                NumericEditTextPreferenceDialogFragment.newInstance(preference.key)
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
        expertMode(sharedPreferences!!.getBoolean(KEY_EXPERT_MODE, false))
        setPreferencesEnabled(!sharedPreferences!!.getBoolean(KEY_STATUS, false))
        expertPressed = 0
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun setPreferencesEnabled(enabled: Boolean) {
        if (sharedPreferences!!.getBoolean(KEY_EXPERT_MODE, false)) {
            findPreference<Preference>(KEY_DEVICE)?.isEnabled = enabled
        }
        findPreference<Preference>(KEY_BOAT_NAME)?.isEnabled = enabled
        findPreference<Preference>(KEY_URL)?.isEnabled = enabled
        findPreference<Preference>(KEY_INITIAL_INTERVAL)?.isEnabled = enabled
        findPreference<Preference>(KEY_DISTANCE)?.isEnabled = enabled
        findPreference<Preference>(KEY_ANGLE)?.isEnabled = enabled
        findPreference<Preference>(KEY_ACCURACY)?.isEnabled = enabled
        findPreference<Preference>(KEY_BUFFER)?.isEnabled = enabled
        findPreference<Preference>(KEY_WAKELOCK)?.isEnabled = enabled
        findPreference<Preference>(KEY_TRACKING)?.isEnabled = enabled
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences== null) {
            return
        }
        when (key) {
            KEY_DEVICE -> findPreference<Preference>(KEY_DEVICE)!!.summary =
                sharedPreferences.getString(KEY_DEVICE, null)
            KEY_BOAT_NAME -> {
                findPreference<Preference>(KEY_BOAT_NAME)!!.summary = sharedPreferences.getString(
                    KEY_BOAT_NAME, null
                )
                if (FirebaseAuth.getInstance().currentUser != null) {
                    sharedPreferences.getString(
                        KEY_BOAT_NAME, ""
                    )?.let {
                        FirestoreDatabase.updateBoatName(
                            it, FirebaseAuth.getInstance().currentUser!!
                                .uid
                        )
                    }
                }
            }
            KEY_STATUS -> {
                setPreferencesEnabled(!sharedPreferences.getBoolean(KEY_STATUS, false))
                activity?.let {
                    (requireActivity().application as MainApplication).handleRatingFlow(
                        it
                    )
                }
            }
        }
    }

    private fun initPreferences() {
        PreferenceManager.setDefaultValues(requireActivity(), R.xml.preferences, false)
        if (!sharedPreferences!!.contains(KEY_DEVICE)) {
            val id = (Random().nextInt(900000) + 100000).toString()
            sharedPreferences!!.edit().putString(KEY_DEVICE, id).apply()
            (findPreference<Preference>(KEY_DEVICE) as EditTextPreference?)!!.text = id
        }
        findPreference<Preference>(KEY_DEVICE)!!.summary =
            sharedPreferences!!.getString(KEY_DEVICE, null)
        if (!sharedPreferences!!.contains(KEY_BOAT_NAME)) {
            val id = "Boat_" + sharedPreferences!!.getString(KEY_DEVICE, null)
            sharedPreferences!!.edit().putString(KEY_BOAT_NAME, id).apply()
            (findPreference<Preference>(KEY_BOAT_NAME) as EditTextPreference?)!!.text = id
        }
        findPreference<Preference>(KEY_BOAT_NAME)!!.summary = sharedPreferences!!.getString(
            KEY_BOAT_NAME, ""
        )
    }

    private fun validateServerURL(userUrl: String): Boolean {
        val port = Uri.parse(userUrl).port
        if (
            URLUtil.isValidUrl(userUrl) &&
            (port == -1 || port in 1..65535) &&
            (URLUtil.isHttpUrl(userUrl) || URLUtil.isHttpsUrl(userUrl))
        ) {
            return true
        }
        Toast.makeText(activity, R.string.error_msg_invalid_url, Toast.LENGTH_LONG).show()
        return false
    }

    private fun expertMode(b: Boolean?) {
        findPreference<Preference>(KEY_DEVICE)!!.isEnabled = b!!
        findPreference<Preference>(KEY_URL)!!.isVisible = b
        findPreference<Preference>(KEY_BUFFER)!!.isVisible = b
        findPreference<Preference>(KEY_WAKELOCK)!!.isVisible = b
        findPreference<Preference>(KEY_TRACKING)!!.isVisible = b
    }

    companion object {
        const val KEY_DEVICE = "id"
        const val KEY_BOAT_NAME = "boat_name"
        const val KEY_URL = "url"
        const val KEY_URL_GATES = "urlgates"
        const val KEY_INITIAL_INTERVAL = "initialinterval"
        const val KEY_INTERVAL = "interval"
        const val KEY_DISTANCE = "distance"
        const val KEY_ANGLE = "angle"
        const val KEY_ACCURACY = "accuracy"
        const val KEY_STATUS = "status" //Tracking status
        const val KEY_BUFFER = "buffer"
        const val KEY_WAKELOCK = "wakelock"
        const val KEY_NEXT_WPT = "nextwpt"
        const val KEY_ROUTE = "route"
        const val KEY_LAST_SEND = "lastsend"
        const val KEY_EXPERT_MODE = "expert"
        const val KEY_GATE_PASSES = "gatepasses"
        const val KEY_TRACKING = "trackingenabled"
        const val KEY_MAGNETIC = "magnetic"
        const val KEY_IS_UI_VISIBLE = "uivisibility"
        const val KEY_ADAPTIVE_INTERVAL = "adaptiveinterval"
        const val KEY_ROUTE_UPDATED_VERSION = "routeupdatedversion" // used to check to what version route updated and that user refused to update
    }
}