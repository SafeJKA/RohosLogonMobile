package com.rohos.logon1;

//import android.app.Notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.rohos.logon1.widget.UnlockPcService;

public class UPDService extends Service {

    static private UPDService mUPDService = null;

    private final String TAG = "UPDService";

    private final int NOTIFICATION_ID = 1001;

    public static UPDService getInstance() {
        return mUPDService;
    }

    @Override
    public void onCreate() {
        try {
            mUPDService = this;

            boolean showNotifi = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("show_icon", getResources().getBoolean(R.bool.show_icon_on_tb));
            if (showNotifi) showNotification();

            new Thread(new UPDClient((RohosApplication) getApplication())).start();
        } catch (Exception e) {
            // Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            new Thread(new UPDClient((RohosApplication) getApplication())).start();
        } catch (Exception e) {
            //Log.e(TAG, Log.getStackTraceString(e));
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mUPDService = null;
        super.onDestroy();
    }

    public void showNotification() {
        try {
            //Context context = getApplicationContext();
            Resources res = getResources();

            NotificationCompat.Builder ncBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(res.getString(R.string.app_name))
                    .setContentText(res.getString(R.string.notifi_title));
            PendingIntent pi = PendingIntent
                    .getService(this, 0, new Intent(this, UnlockPcService.class), 0);

            ncBuilder.setContentIntent(pi);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, ncBuilder.getNotification());

        } catch (Exception e) {
            //Log.e(TAG, e.toString());
        }
    }

    public void cancelNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(NOTIFICATION_ID);
        } catch (Exception e) {
            // Log.e(TAG, e.toString());
        }
    }
}
