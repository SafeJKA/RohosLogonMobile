package com.rohos.logon1;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.rohos.logon1.services.KnockService;
import com.rohos.logon1.services.LockPCService;
import com.rohos.logon1.utils.AppLog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings extends AppCompatActivity {
    private final String TAG = "Settings";

    public static String getApiVersion(Context context){
        PackageInfo pi = null;
        try{
            pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        }catch(PackageManager.NameNotFoundException e){
            AppLog.log(Log.getStackTraceString(e));
        }
        return (pi == null ? " " : pi.versionName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
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

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
        private final String TAG = "PrefesFragment";
        private final String LOCK_PC_IF_LEAVE_CONN = "lock_if_leavs_conn";
        private final String KEY_EDIT_TEXT_PREFERENCE = "broker";

        private SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(KEY_EDIT_TEXT_PREFERENCE)) {

                    String sharedPreferenceNewData = sharedPreferences.getString(key, "");
                    if (sharedPreferenceNewData != null && sharedPreferenceNewData.length() > 2) {

                        final String regex = "(tcp://)(?:(\\S+):(\\S+)@)?(\\S+):(\\d+)(?:@(\\S+))?";
                        final Pattern pattern = Pattern.compile(regex);
                        final Matcher matcher = pattern.matcher(sharedPreferenceNewData);

                        if (matcher.matches()) {
                            Preference preference = findPreference(key);
                            if (preference instanceof EditTextPreference) {
                                Toast.makeText(getActivity().getApplicationContext(), "URI accepted", Toast.LENGTH_SHORT).show();
                                preference.setSummary("Broker URI: " + sharedPreferenceNewData);
                            }
                        } else {
                            //System.err.println("The provided URL does not match the required pattern");
                            Toast.makeText(getActivity().getApplicationContext(), "Pattern mismatch.Introduce URI again", Toast.LENGTH_SHORT).show();  //show some information to the user, either as toast or some dialog
                            Preference preference = findPreference(key);
                            if (preference instanceof EditTextPreference) {
                                preference.setSummary("Provided broker URI:" + sharedPreferenceNewData);
                            }
                        }
                    } else {
                        Preference preference = findPreference(key);
                        if (preference instanceof EditTextPreference) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.clear().apply();   //clear the store data
                            preference.setSummary("Set MQTT broker URI");
                        }
                    }
                }
            }
        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            try {
                ListPreference numberKnocks = (ListPreference) findPreference("number_knocks");
                numberKnocks.setOnPreferenceChangeListener(this);

                CheckBoxPreference startKnockServ = (CheckBoxPreference) findPreference("knock_recog");
                startKnockServ.setOnPreferenceChangeListener(this);

                final Settings activity = (Settings) getActivity();
                Preference testRecog = findPreference("test_knock");
                testRecog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Context context = preference.getContext();

                        RohosApplication app = (RohosApplication) activity.getApplication();
                        app.mTestKnockMod = true;

                        activity.startService(new Intent(context, KnockService.class));

                        activity.showAlertDialog();
                        return false;
                    }
                });

                //CheckBoxPreference unlockIfLeavConn = (CheckBoxPreference) findPreference("lock_if_leavs_conn");
                //unlockIfLeavConn.setOnPreferenceChangeListener(this);

                CheckBoxPreference showIcon = (CheckBoxPreference) findPreference("show_icon");
                showIcon.setOnPreferenceChangeListener(this);

                Preference apiVersion = findPreference("api_version");
                apiVersion.setTitle(getString(R.string.about_text, getApiVersion(activity)));

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                sp.registerOnSharedPreferenceChangeListener(mListener);

                updatePreference(KEY_EDIT_TEXT_PREFERENCE);
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        @Override
        public void onStop(){
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.unregisterOnSharedPreferenceChangeListener(mListener);
            super.onStop();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            try {
                String key = preference.getKey();
                Log.d(TAG, "preference changed, key:" + key);

                if (key.equals(LOCK_PC_IF_LEAVE_CONN)) {
                    updatePreferences(((Boolean) newValue).booleanValue());
                    return true;
                } else if (key.equals("number_knocks")) {
                    //Log.d(TAG, "New knocks " + (String)newValue);
                    return true;
                } else if (key.equals("knock_recog")) {
                    RohosApplication app = (RohosApplication) getActivity().getApplication();
                    if (((Boolean) newValue).booleanValue()) {
                        Message msg = RohosApplication.mHandler.obtainMessage(RohosApplication.START_RECOGNIZING_SERVICE);
                        RohosApplication.mHandler.sendMessage(msg);
                    } else {
                        Message msg = RohosApplication.mHandler.obtainMessage(RohosApplication.STOP_RECOGNIZING_SERVICE);
                        RohosApplication.mHandler.sendMessage(msg);
                    }
                    return true;
                } else if (key.equals("show_icon")) {
                    boolean showIc = ((Boolean) newValue).booleanValue();
                    UPDService service = UPDService.getInstance();
                    if (service == null) return true;

                    if (showIc) service.showNotification();
                    else service.cancelNotification();
                    return true;
                }
            } catch (Exception e) {
                // Log.e(TAG, e.toString());
            }

            return false;
        }

        private void updatePreference(String key) {
            if (key.equals(KEY_EDIT_TEXT_PREFERENCE)) {
                Preference preference = findPreference(key);
                if (preference instanceof EditTextPreference) {
                    EditTextPreference editTextPreference = (EditTextPreference) preference;
                    String text = editTextPreference.getText();
                    if (text != null && text.trim().length() > 0) {
                        editTextPreference.setSummary("Broker URI: " + editTextPreference.getText());
                    } else {
                        editTextPreference.setSummary("Set MQTT broker URI");
                    }
                }
            }
        }

        private void updatePreferences(boolean enabled) {
            try {
                Activity activity = getActivity();
                if (enabled) {
                    activity.startService(new Intent(activity, LockPCService.class));
                } else {
                    activity.stopService(new Intent(activity, LockPCService.class));
                }
            } catch (Exception e) {
                // Log.e(TAG, e.toString());
            }
        }
    }
}