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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.Toast;

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
            // Log.e(TAG, e.toString());
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

        if (key.equals(KEY_EDIT_TEXT_PREFERENCE)) {

            String sharedPreferenceNewData = sharedPreferences.getString(key, "");
            if (sharedPreferenceNewData != null && sharedPreferenceNewData.length() > 2) {

                final String regex = "(tcp://)(?:(\\S+):(\\S+)@)?(\\S+):(\\d+)(?:@(\\S+))?";
                final Pattern pattern = Pattern.compile(regex);
                final Matcher matcher = pattern.matcher(sharedPreferenceNewData);

                if (matcher.matches()) {
                    Preference preference = findPreference(key);
                    if (preference instanceof EditTextPreference) {
                        Toast.makeText(this, "URI accepted", Toast.LENGTH_SHORT).show();
                        preference.setSummary("Broker URI: " + sharedPreferenceNewData);
                    }
                } else {
                    System.err.println("The provided URL does not match the required pattern");
                    Toast.makeText(this, "Pattern mismatch.Introduce URI again", Toast.LENGTH_SHORT).show();  //show some information to the user, either as toast or some dialog
                    Preference preference = findPreference(key);
                    if (preference instanceof EditTextPreference) {
                        preference.setSummary("Provided broker URI:" + sharedPreferenceNewData);
                    }
                }
            } else {
                Preference preference = findPreference(key);
                if (preference instanceof EditTextPreference) {
                    SharedPreferences.Editor editor = preference.getEditor();
                    editor.clear().apply();   //clear the store data
                    preference.setSummary("Set MQTT broker URI");
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
            // Log.e(TAG, e.toString());
        }
    }
}
