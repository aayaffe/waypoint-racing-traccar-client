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
package in.avimarine.waypointracing.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import in.avimarine.waypointracing.MainApplication;
import in.avimarine.waypointracing.R;

import java.util.Arrays;
import java.util.Random;

public class MainFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {

    private static final String TAG = MainFragment.class.getSimpleName();

    public static final String KEY_DEVICE = "id";
    public static final String KEY_NAME = "boat_name";
    public static final String KEY_URL = "url";
    public static final String KEY_INTERVAL = "interval";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_ANGLE = "angle";
    public static final String KEY_ACCURACY = "accuracy";
    public static final String KEY_STATUS = "status";
    public static final String KEY_BUFFER = "buffer";
    public static final String KEY_WAKELOCK = "wakelock";


    private SharedPreferences sharedPreferences;

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        setPreferencesFromResource(R.xml.preferences, rootKey);
        initPreferences();
        findPreference(KEY_DEVICE).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return newValue != null && !newValue.equals("");
            }
        });
        findPreference(KEY_NAME).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return newValue != null && !newValue.equals("");
            }
        });
        findPreference(KEY_URL).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return (newValue != null) && validateServerURL(newValue.toString());
            }
        });

        findPreference(KEY_INTERVAL).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    try {
                        int value = Integer.parseInt((String) newValue);
                        return value > 0;
                    } catch (NumberFormatException e) {
                        Log.w(TAG, e);
                    }
                }
                return false;
            }
        });

        Preference.OnPreferenceChangeListener numberValidationListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    try {
                        int value = Integer.parseInt((String) newValue);
                        return value >= 0;
                    } catch (NumberFormatException e) {
                        Log.w(TAG, e);
                    }
                }
                return false;
            }
        };
        findPreference(KEY_DISTANCE).setOnPreferenceChangeListener(numberValidationListener);
        findPreference(KEY_ANGLE).setOnPreferenceChangeListener(numberValidationListener);
    }

    public static class NumericEditTextPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {

        public static NumericEditTextPreferenceDialogFragment newInstance(String key) {
            final NumericEditTextPreferenceDialogFragment fragment = new NumericEditTextPreferenceDialogFragment();
            final Bundle bundle = new Bundle();
            bundle.putString(ARG_KEY, key);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        protected void onBindDialogView(View view) {
            EditText editText = view.findViewById(android.R.id.edit);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            super.onBindDialogView(view);
        }

    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (Arrays.asList(KEY_INTERVAL, KEY_DISTANCE, KEY_ANGLE).contains(preference.getKey())) {
            final EditTextPreferenceDialogFragmentCompat f = NumericEditTextPreferenceDialogFragment.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setPreferencesEnabled(!sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false));
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setPreferencesEnabled(boolean enabled) {
        findPreference(KEY_DEVICE).setEnabled(enabled);
        findPreference(KEY_NAME).setEnabled(enabled);
        findPreference(KEY_URL).setEnabled(enabled);
        findPreference(KEY_INTERVAL).setEnabled(enabled);
        findPreference(KEY_DISTANCE).setEnabled(enabled);
        findPreference(KEY_ANGLE).setEnabled(enabled);
        findPreference(KEY_ACCURACY).setEnabled(enabled);
        findPreference(KEY_BUFFER).setEnabled(enabled);
        findPreference(KEY_WAKELOCK).setEnabled(enabled);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_DEVICE)) {
            findPreference(KEY_DEVICE).setSummary(sharedPreferences.getString(KEY_DEVICE, null));
        } else if (key.equals(KEY_NAME)) {
            findPreference(KEY_NAME).setSummary(sharedPreferences.getString(KEY_NAME, null));
        } else if  (key.equals(KEY_STATUS)) {
            setPreferencesEnabled(!sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false));
            ((MainApplication) getActivity().getApplication()).handleRatingFlow(getActivity());
        }

    }

    private void initPreferences() {
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        if (!sharedPreferences.contains(KEY_DEVICE)) {
            String id = String.valueOf(new Random().nextInt(900000) + 100000);
            sharedPreferences.edit().putString(KEY_DEVICE, id).apply();
            ((EditTextPreference) findPreference(KEY_DEVICE)).setText(id);
        }
        findPreference(KEY_DEVICE).setSummary(sharedPreferences.getString(KEY_DEVICE, null));
        if (!sharedPreferences.contains(KEY_NAME)) {
            String id = "Boat_" + sharedPreferences.getString(KEY_DEVICE, null);
            sharedPreferences.edit().putString(KEY_NAME, id).apply();
            ((EditTextPreference) findPreference(KEY_NAME)).setText(id);
        }
        findPreference(KEY_NAME).setSummary(sharedPreferences.getString(KEY_NAME, null));
    }

    private boolean validateServerURL(String userUrl) {
        int port = Uri.parse(userUrl).getPort();
        if (URLUtil.isValidUrl(userUrl) && (port == -1 || (port > 0 && port <= 65535))
                && (URLUtil.isHttpUrl(userUrl) || URLUtil.isHttpsUrl(userUrl))) {
            return true;
        }
        Toast.makeText(getActivity(), R.string.error_msg_invalid_url, Toast.LENGTH_LONG).show();
        return false;
    }

}
