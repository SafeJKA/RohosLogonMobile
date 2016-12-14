package com.rohos.logon1.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import com.rohos.logon1.NativeKnockRecognizer;
import com.rohos.logon1.RohosApplication;

public class KnockService extends Service implements SensorEventListener {
	
	private final String TAG = "KnockService";
		
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private NativeKnockRecognizer mNativeKnockRecog;
	private RohosApplication mApp = null;
	
	private float[][] mBuffer = null;
	
	//private long mTS = 0L;
	
	//private float mX = 0.0f;
	//private float mY = 0.0f;
	//private float mZ = 0.0f;
	
	private int mBuffSize = 100;
	private int mI = 0;
			
	@Override
	public void onCreate(){
		try{
			mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
			mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			
			if(mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0){
				mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
				//mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
			}			
			
			
			//mNativeKnockRecog = new NativeKnockRecognizer(KnockService.this);
			//mNativeKnockRecog.initRecognizing();
			
			mApp = (RohosApplication)getApplication();
			mNativeKnockRecog = mApp.mNativeKnockRecog;
            mNativeKnockRecog.initRecognizing(mBuffSize);

            mBuffer = new float[mBuffSize][3];
            //mApp.mBuffSize = 100;
            //mNativeKnockRecog.initRecognizing(mApp.mBuffSize);
			
			//if(mApp.mBuffSize > 0){
			//	mBuffer = new float[mApp.mBuffSize][3];
			//}
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		if(mApp != null){
			mApp.logError("KnockService.onStartCommand method launched");
		}
		
		//Log.d(TAG, "onStartCommand");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy(){
		try{
			mSensorManager.unregisterListener(this);
		}catch(Exception e){
            Log.d(TAG, e.toString());
        }
		
		super.onDestroy();
        Log.d(TAG, "onDestroy");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy){
		/*
		try{
			if(mApp.mBuffSize < 0){
				int maxRange = (int)sensor.getMaximumRange();

				if(maxRange > 38){
					mApp.mBuffSize = 100;
				}else if(maxRange > 18 && maxRange < 38){
					mApp.mBuffSize = 100;
				}

				mNativeKnockRecog.initRecognizing(mApp.mBuffSize);

                //mApp.mBuffSize = 100;
                //mNativeKnockRecog.initRecognizing(100);

				
				Log.d(TAG, "buff size is " + mApp.mBuffSize);
				stopSelf();
			}
			
			Log.d(TAG, "Min delay " + sensor.getMinDelay() + 
					", max event range " + sensor.getMaximumRange() +
					", resolution " + sensor.getResolution() +
					", vendor " + sensor.getVendor());
			
			
		}catch(Exception e){
            Log.e(TAG, e.toString());
        }
        */
	}

	@Override
	public void onSensorChanged(SensorEvent event){
		try{
			if(mBuffer == null) return;
			
			if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
				mBuffer[mI] = new float[]{event.values[0], event.values[1], event.values[2]};
				mI++;
				if(mI == mBuffSize){
					mI = 0;
					final float[] buffer = copyArray2(mBuffer);
					new Thread(new Runnable(){
						@Override
						public void run(){
							//showArray2(buffer);
							mNativeKnockRecog.recognizeKnock(buffer);
						}
					}).run();
				}
			}
			//long ts = System.currentTimeMillis();
			//Log.d(TAG, "time " + (ts - mTS));
			//mTS = ts;
			//Log.d(TAG, "Sensor " + event.sensor.getType());
			/*
			if(mX == 0 && mY == 0 && mZ == 0){
				mX = event.values[0];
				mY = event.values[1];
				mZ = event.values[2];
				return;
			}
			
			float x = Math.abs(mX - event.values[0]);
			float y = Math.abs(mY - event.values[1]);
			float z = Math.abs(mZ - event.values[2]);
			
			mX = event.values[0];
			mY = event.values[1];
			mZ = event.values[2];
			
			mBuffer[mI] = new float[]{x, y, z};
			mI++;
			if(mI == mBufferSize){
				mI = 0;
				final float[] buffer = copyArray2(mBuffer);
				new Thread(new Runnable(){
					@Override
					public void run(){
						//showArray2(buffer);
						mNativeKnockRecog.recognizeKnock(buffer);
					}
				}).run();
			}
			*/
			//Log.d(TAG, x + " " + y + " " + z);
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}		
	}
	/*
	private void showArray2(float[] buffer){
		try{
			for(int i = 0; i < buffer.length; i++){
				Log.d(TAG, " " + buffer[i]);
			}
			Log.d(TAG, "buffer length " + buffer.length);
		}catch(Exception e){
			Log.e(TAG, "showArray2 " + e.toString());
		}
	}
	
	private void showArray(float[][] buffer){
		try{
			for(int i = 0; i < buffer.length; i++){
				Log.d(TAG, buffer[i][0] + " - " + buffer[i][1] + " - " + buffer[i][2]);				
			}
			Log.d(TAG, "buffer length " + buffer.length);
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
	}
	*/
	private float[] copyArray2(float[][] buff){
		try{			
			float[] resBuff = new float[buff[0].length * buff.length];
			int index = 0;
			for(int i = 0; i < buff.length; i++){
				for(int k = 0; k < buff[0].length; k++){
					resBuff[index++] = buff[i][k];
				}
			}
			return resBuff;
		}catch(Exception e){
			Log.e(TAG, "copyArray2 " + e.toString());
			return new float[]{};
		}
	}
	/*
	private float[][] copyArray(float[][] buff){		
		try{
			//float[][] resBuff = new float[buff.length][buff[0].length];
			//for(int i = 0; i < buff.length; i++){
			//	System.arraycopy(buff[i], 0, resBuff[i], 0, buff[i].length);
			//}
			
			float[][] resBuff = new float[buff.length][];
			System.arraycopy(buff, 0, resBuff, 0, buff.length);
			
			return resBuff;
		}catch(Exception e){
			Log.e(TAG, e.toString());
			return new float[][]{};
		}
	}
	*/	
}
