package com.rohos.logon1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.rohos.logon1.services.KnockService;
import com.rohos.logon1.services.LockPCService;

/**
 * Created by yura on 11/20/15.
 */
public class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private final String TAG = "PrefesFragment";
    private final String LOCK_PC_IF_LEAVE_CONN = "lock_if_leavs_conn";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            addPreferencesFromResource(R.xml.settings);

            ListPreference numberKnocks = (ListPreference) findPreference("number_knocks");
            numberKnocks.setOnPreferenceChangeListener(this);

            CheckBoxPreference startKnockServ = (CheckBoxPreference) findPreference("knock_recog");
            startKnockServ.setOnPreferenceChangeListener(this);

            final Settings activity = (Settings) getActivity();
            Preference testRecog = (Preference) findPreference("test_knock");
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

            CheckBoxPreference unlockIfLeavConn = (CheckBoxPreference) findPreference("lock_if_leavs_conn");
            unlockIfLeavConn.setOnPreferenceChangeListener(this);

            CheckBoxPreference showIcon = (CheckBoxPreference) findPreference("show_icon");
            showIcon.setOnPreferenceChangeListener(this);

        } catch (Exception e) {
            // Log.e(TAG, e.toString());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            String key = preference.getKey();

            if (key.equals(LOCK_PC_IF_LEAVE_CONN)) {
                updatePreferences(((Boolean) newValue).booleanValue());
                return true;
            } else if (key.equals("number_knocks")) {
                //Log.d(TAG, "New knocks " + (String)newValue);
                return true;
            } else if (key.equals("knock_recog")) {
                RohosApplication app = (RohosApplication) getActivity().getApplication();
                if (((Boolean) newValue).booleanValue()) {
                    Message msg = app.mHandler.obtainMessage(RohosApplication.START_RECOGNIZING_SERVICE);
                    app.mHandler.sendMessage(msg);
                } else {
                    Message msg = app.mHandler.obtainMessage(RohosApplication.STOP_RECOGNIZING_SERVICE);
                    app.mHandler.sendMessage(msg);
                }
                return true;
            } else if (key.equals("show_icon")) {
                boolean showIc = ((Boolean) newValue).booleanValue();
                UPDService service = UPDService.getInstance();
                if (service == null) return false;
                if (showIc) service.showNotification();
                else service.cancelNotification();
                return true;
            }
        } catch (Exception e) {
            // Log.e(TAG, e.toString());
        }

        return false;
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
