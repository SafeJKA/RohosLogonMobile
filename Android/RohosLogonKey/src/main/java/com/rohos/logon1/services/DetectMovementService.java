package com.rohos.logon1.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.rohos.logon1.AuthRecord;
import com.rohos.logon1.AuthRecordsDb;
import com.rohos.logon1.BTService;
import com.rohos.logon1.NetworkSender;
import com.rohos.logon1.R;
import com.rohos.logon1.RohosApplication;

import java.util.ArrayList;

public class DetectMovementService extends Service implements SensorEventListener {

    private static Handler mHandler = null;

    private final String TAG = "DetectMovService";

    private final int BEGIN_DETECTING = 1001;
    private final int FINISH_DETECTING = 1002;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private float[] mGravity = null;
    private float[] mGeomagnetic = null;

    private int mCount = 0;

    @Override
    public void onCreate(){
        try{
            mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);

            mHandler = new Handler(){
                public void handleMessage(Message msg){
                    switch(msg.what){
                        case BEGIN_DETECTING:
                            break;
                        case FINISH_DETECTING:
                            unregisterSensorListener();
                            if(mCount > 2){// phone do not move
                                new Thread(new Runnable(){
                                    public void run(){
                                        sendPackage();
                                    }
                                }).start();
                            }else stopSelf();

                            Log.d(TAG, "count " + mCount);
                            break;
                    }
                }
            };

            Message finish = mHandler.obtainMessage(FINISH_DETECTING);
            mHandler.sendMessageDelayed(finish, 3000L);
        }catch(Exception e){
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onDestroy(){
        mHandler = null;
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){
        //try{
            //Log.d(TAG, "Min delay " + sensor.getMinDelay() +
            //        ", max event range " + sensor.getMaximumRange() +
            //        ", resolution " + sensor.getResolution() +
            //        ", vendor " + sensor.getVendor());
        //}catch(Exception e){
        //    Log.e(TAG, e.toString());
        //}
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        try{
            //Log.d(TAG, "Type " + event.sensor.getType());

            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;
            if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;

            if(mGravity != null && mGeomagnetic != null){
                float[] r = new float[9];
                float[] i = new float[9];
                boolean success = SensorManager.getRotationMatrix(r, i, mGravity, mGeomagnetic);

                if(success){
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(r, orientation);

                    float pitch = Math.abs(orientation[1]);
                    float roll = Math.abs(orientation[2]);

                    if(pitch < 0.1f && roll < 0.1f){
                        mCount++;
                    }else{
                        mCount = 0;
                    }

                    //Log.d(TAG, "pitch " + pitch + ", roll " + roll);
                }else{
                    //Log.d(TAG, "Couldn't get orientation");
                    RohosApplication app = (RohosApplication)getApplication();
                    app.logError(TAG + ", couldn't get orientation");
                }
            }
        }catch(Exception e){
            Log.e(TAG, e.toString());
        }
    }

    private void sendPackage(){
        RohosApplication app = null;
        try{
            app = (RohosApplication)getApplication();
            AuthRecordsDb authRecordsDb = new AuthRecordsDb(getApplicationContext());

            // Send unlock package via WiFi
            ArrayList<String> recordNames = new ArrayList<String>();
            authRecordsDb.getNames(recordNames);

            for(int i = 0; i < recordNames.size(); i++){
                AuthRecord ar = authRecordsDb.getAuthRecord(recordNames.get(i));
                NetworkSender netSender = new NetworkSender(getApplicationContext());
                netSender.execute(ar);
            }

            // Send unlock data via Bluetooth
            Resources res = getResources();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            if(sp.getBoolean("use_bluetooth_unlock", res.getBoolean(R.bool.unlock_on_table_d))) {
                startService(new Intent(DetectMovementService.this, BTService.class));
            }

            //Log.d(TAG, "Package is sent");

            app.logError(TAG + ", Unlocking PC package is sent");
        }catch(Exception e){
            if(app != null) app.logError(TAG + ", " + e.toString());
            //Log.e(TAG, e.toString());
        }finally{
            DetectMovementService.this.stopSelf();
        }
    }

    private void unregisterSensorListener(){
        try{
            mSensorManager.unregisterListener(this);
        }catch(Exception e){
            Log.e(TAG, e.toString());
        }
    }
}
