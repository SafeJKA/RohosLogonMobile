package com.rohos.logon1.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.rohos.logon1.AuthRecord;
import com.rohos.logon1.AuthRecordsDb;
import com.rohos.logon1.BTService;
import com.rohos.logon1.MainActivity;
import com.rohos.logon1.NetworkSender;
import com.rohos.logon1.R;
import com.rohos.logon1.RohosApplication;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LockPCService extends Service {

    private final String TAG = "LockPCService";

    private NotificationManager mNM = null;
    private ScheduledExecutorService mSendPackPeriod = null;
    private ScheduledFuture mSendPackPeriodHandler = null;
    private AuthRecordsDb mAuthRecordsDb;
    private WiFiReceiver mWifiReceiver = null;

    private int mNotification;

    private volatile boolean mWifiIsConnected = false;

    @Override
    public void onCreate(){
        try{
            mSendPackPeriod = Executors.newScheduledThreadPool(1);
            final Runnable sendingPackage = new Runnable(){
                public void run(){
                    if(mWifiIsConnected) sendPackagePeriodically();
                }
            };
            mSendPackPeriodHandler = mSendPackPeriod.scheduleWithFixedDelay(sendingPackage, 30, 30,
                    TimeUnit.SECONDS);

            mAuthRecordsDb = new AuthRecordsDb(getApplicationContext());

            mWifiReceiver = new WiFiReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
            registerReceiver(mWifiReceiver, intentFilter);

            mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            mNotification = R.string.service_started;
        }catch(Exception e){
            Log.e(TAG, e.toString());
        }

        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        try{
            if(mNM != null){
                mNM.cancel(mNotification);
                mNM = null;
            }

            if(mSendPackPeriodHandler != null){
                mSendPackPeriodHandler.cancel(true);
                mSendPackPeriodHandler = null;
                mSendPackPeriod.shutdown();
                mSendPackPeriod = null;
            }

            if(mWifiReceiver != null){
                unregisterReceiver(mWifiReceiver);
                mWifiReceiver = null;
            }

            mAuthRecordsDb = null;
        }catch(Exception e){
            Log.e(TAG, e.toString());
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendPackagePeriodically(){
        try{
            // Send unlock package via WiFi
            ArrayList<String> recordNames = new ArrayList<String>();
            mAuthRecordsDb.getNames(recordNames);

            for(int i = 0; i < recordNames.size(); i++){
                String name = recordNames.get(i).substring(0, recordNames.get(i).indexOf("|"));
                String hostName = recordNames.get(i).substring(recordNames.get(i).indexOf("|")+1);
                AuthRecord ar = mAuthRecordsDb.getAuthRecord(name, hostName);
                NetworkSender netSender = new NetworkSender(getApplicationContext());
                netSender.execute(ar);
            }

            // Send unlock data via Bluetooth
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            if(sp.getBoolean("use_bluetooth_unlock", true)) {
                startService(new Intent(LockPCService.this, BTService.class));
            }

            Log.d(TAG, "Package is sent");
        }catch(Exception e){
            Log.e(TAG, e.toString());
        }
    }

    private void showNotification(){
        try{
            CharSequence text = getText(R.string.service_started);
            PendingIntent contentIntent = null;
            if(Build.VERSION.SDK_INT >= 31){
                contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                                MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
            }else{
                contentIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class), 0);
            }
            Notification.Builder builder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setTicker(text)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle("LPCService")
                    .setContentText(getText(R.string.service_summ))
                    .setContentIntent(contentIntent);
            Notification notification;
            if(Build.VERSION.SDK_INT >= 16){ notification = builder.build(); }
            else{ notification = builder.getNotification(); }
            mNM.notify(mNotification, notification);
        }catch(Exception e){
            Log.e(TAG, e.toString());
        }
    }

    // inner classes
    private class WiFiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            if(intent == null) return;

            String action = intent.getAction();
            //Log.d("WifiReceiver", "action " + action);
            try{
                if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                    NetworkInfo ni = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                    if(ni.getType() == ConnectivityManager.TYPE_WIFI){
                        if(ni.isConnected()){
                            mWifiIsConnected = true;
                            Log.d("WifiReceiver", "WiFi is connected");
                        }else if(ni.getState() == NetworkInfo.State.DISCONNECTED && mWifiIsConnected){
                            mWifiIsConnected = false;
                            Log.d("WifReceiver", "WiFi is disconnected");
                        }
                        //Log.d("WifReceiver", "state " + ni.getState().toString());
                    }
                }
            }catch(Exception e){
                Log.e("WifiReceiver", e.toString());
            }
        }
    }
}
