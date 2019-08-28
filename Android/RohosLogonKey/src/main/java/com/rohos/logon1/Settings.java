package com.rohos.logon1;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;

import android.preference.PreferenceActivity;
import android.app.FragmentManager;
import android.util.Log;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.rohos.logon1.services.KnockService;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private final String TAG = "Settings";
    private static final String KEY_EDIT_TEXT_PREFERENCE = "broker";

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        addPreferencesFromResource(R.xml.settings);
        try {
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        } catch (NullPointerException exc) {
            exc.printStackTrace();
        }
        try {
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();

            PrefsFragment pf = new PrefsFragment();
            ft.replace(android.R.id.content, pf);
            ft.commit();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        } catch (NullPointerException exc) {
            exc.printStackTrace();
        }
        updatePreference(KEY_EDIT_TEXT_PREFERENCE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        try {
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        } catch (NullPointerException exc) {
            exc.printStackTrace();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // Toast.makeText(this, sharedPreferenceNewData, Toast.LENGTH_LONG).show();
        if (key.equals(KEY_EDIT_TEXT_PREFERENCE)) {
            Preference preference = findPreference(key);
            if (preference instanceof EditTextPreference) {
                String sharedPreferenceNewData = sharedPreferences.getString(key, "");
                if (sharedPreferenceNewData != null && sharedPreferenceNewData.length() > 2) {
                    preference.setSummary("Broker URI: " + sharedPreferenceNewData);
                } else {
                    preference.setSummary("Set Mqtt broker URI");
                }
            }
        }
    }

    private void updatePreference(String key) {
        if (key.equals(KEY_EDIT_TEXT_PREFERENCE)) {
            Preference preference = findPreference(key);
            if (preference instanceof EditTextPreference) {
                EditTextPreference editTextPreference = (EditTextPreference) preference;
                if (editTextPreference.getText().trim().length() > 0) {
                    editTextPreference.setSummary("Broker URI: " + editTextPreference.getText());
                } else {
                    editTextPreference.setSummary("Set MQTT broker URI");
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            RohosApplication app = (RohosApplication) getApplication();
            app.mTestKnockMod = false;
        } catch (Exception e) {
        }
    }

    public void showAlertDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Test knock")
                    .setMessage("Knock on the device")
                    .setCancelable(false)
                    .setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RohosApplication app = (RohosApplication) getApplication();
                            app.mTestKnockMod = false;

                            // Prevent to stop KnockService if the service was started by UPDClient
                            if (app.mContinueDetecting) {
                                stopService(new Intent(Settings.this, KnockService.class));
                            }

                            dialog.dismiss();
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}
