package com.rohos.logon1;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class NativeKnockRecognizer {

    private final String TAG = "NativeKnockRecognizer";

    //private AuthRecordsDb mAuthRecordsDb;
    private NetworkSender mNetSender;
    private Context mContext;

    private int mKnocksToUnlock = 2;


    static {
        try {
            System.loadLibrary("knock");
        } catch (Exception e) {
            //Log.e("Load library", e.toString());
        }
    }

    public NativeKnockRecognizer(Context context) {
        try {
            //mAuthRecordsDb = new AuthRecordsDb(context.getApplicationContext());
            mContext = context;

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            String tmp = sp.getString("number_knocks", "2");
            mKnocksToUnlock = Integer.valueOf(tmp);
        } catch (Exception e) {
            // Log.e(TAG, e.toString());
        }
    }

    public native boolean initRecognizing(int bufferSize);

    public native void recognizeKnock(float[] buffer);

    private void callback(int knocksFound) {
        try {
            RohosApplication app = (RohosApplication) mContext;
            if (app.mTestKnockMod) {
                Toast.makeText(mContext, knocksFound + " - knocks found.", Toast.LENGTH_SHORT).show();
            } else if (knocksFound >= mKnocksToUnlock) {

                new Thread(new Runnable() {
                    public void run() {
                        sendIpLogin(null);
                        //sendIpLogin(new String(app.mHostName));
                    }
                }).start();

                ((Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100L);

                mContext.startService(new Intent(mContext, BTService.class));
                app.logError(TAG + ", unlocking via Knock Service");
            }
            //Log.d("callback", "knocks found " + knocksFound);
        } catch (Exception e) {
            //Log.e(TAG, e.toString());
        }
    }

    /*
    send Authentication data block to the network
     */
    private void sendIpLogin(String host) {
        try {
            AuthRecordsDb authRecordsDb = new AuthRecordsDb(mContext.getApplicationContext());

            // Send unlock package via WiFi
            ArrayList<String> recordNames = new ArrayList<String>();
            authRecordsDb.getNames(recordNames);

            for (int i = 0; i < recordNames.size(); i++) {
                AuthRecord ar = authRecordsDb.getAuthRecord(recordNames.get(i));
                NetworkSender netSender = new NetworkSender(mContext.getApplicationContext());
                netSender.execute(ar);
            }
        } catch (Exception e) {
            //Log.e(TAG, e.toString());
        }
    }
}
