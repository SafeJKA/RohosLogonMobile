package com.rohos.logon1;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import androidx.preference.PreferenceManager;

import com.rohos.logon1.services.KnockService;
import com.rohos.logon1.utils.AppLog;


public class RohosApplication extends Application {
	
	public final int START_DETECTING_UPD = 1001;
	public final int STOP_DETECTING_UPD = 1002;
	public static final int START_RECOGNIZING_SERVICE = 1003;
	public static final int STOP_RECOGNIZING_SERVICE = 1004;
	public final int INIT_NATIVE_RECOGNIZER = 1005;
	
	private final String TAG = "RohosApplication";	
	
	public static Handler mHandler = null;
	public NativeKnockRecognizer mNativeKnockRecog;
	
	public volatile String mHostName = null; // is set in UPDClient, is used in NativeKnockRecognizer
	                                         // and in UnlockPcService classes
	public volatile boolean mContinueDetecting = true;
	public volatile boolean mWaked = false;
	
	
	//public int mBuffSize = -1;
	
	public boolean mTestKnockMod = false;
		
	private WakeLock mWakeLock = null;
	//private ApiLog apiLog;
	
	//private ScreenStateReceiver mScreenState = null;
	private long mStopRecognizingDelay = 60000L * 5L; // delay 5 min
	
	private static RohosApplication mApp = null;
	public static RohosApplication getInstance(){
		return mApp;
	}	
	
	@Override
	public void onCreate(){
		super.onCreate();		
		try{
			mApp = this;

			AppLog.initAppLog(getApplicationContext());
			
			mNativeKnockRecog = new NativeKnockRecognizer(RohosApplication.this);
			//mNativeKnockRecog.initRecognizing();
			
			//apiLog = new ApiLog();
			
			//IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
	        //intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
	        //intentFilter.addAction(Intent.ACTION_USER_PRESENT);
	        
	        //mScreenState = new ScreenStateReceiver();
	        //registerReceiver(mScreenState, intentFilter);
	        
	        mHandler = new Handler(Looper.getMainLooper()){
	        	@Override
	        	public void handleMessage(Message msg){
	        		switch(msg.what){
	        		case START_DETECTING_UPD:
	        			startDetectingUPD();
	        			break;
	        		case STOP_DETECTING_UPD:
	        			break;
	        		case START_RECOGNIZING_SERVICE:
	        			if(isRecognizingSet()){
	        				startService(new Intent(RohosApplication.this, KnockService.class));
	        				
	        				mContinueDetecting = false;

							if(!mWaked){
                                wakeLock();
                                mWaked = true;
                            }
		        			
		        			//Message stopRecognizing = mHandler.obtainMessage(STOP_RECOGNIZING_SERVICE);
		        			//mHandler.sendMessageDelayed(stopRecognizing, mStopRecognizingDelay);
	        			}	        			
	        			break;
	        		case STOP_RECOGNIZING_SERVICE:
	        			if(mHandler.hasMessages(STOP_RECOGNIZING_SERVICE)){
	        				mHandler.removeMessages(STOP_RECOGNIZING_SERVICE);
	        			}
	        			
	        			
	        			stopService(new Intent(RohosApplication.this, KnockService.class));
	        			mContinueDetecting = true;
	        			//startDetectingUPD();
	        			
	        			wakeRelease();
	        			mWaked = false;
	        			break;
	        		case INIT_NATIVE_RECOGNIZER:
	        			//initNativeRecognizer();
	        			stopService(new Intent(RohosApplication.this, KnockService.class));
	        			break;
	        		}
	        	}
	        };
	        
	        startDetectingUPD();
	        
	        // Start KnockService to calculate buffer size
	        //startService(new Intent(RohosApplication.this, KnockService.class));
	        
	        logError("RohosApplication.onCreate launched");
		}catch(Exception e){
			//Log.e(TAG, e.toString());
		}        
	}
	
	public void wakeLock(){
		try{
			if(mWakeLock != null)
				mWakeLock = null;
			
			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RohosLogon:wake");
			mWakeLock.acquire();
		}catch(Exception e){
			//Log.e(TAG, e.toString());
		}
	}
	
	public void wakeRelease(){
		try{
			if(mWakeLock != null){
				mWakeLock.release();
				mWakeLock = null;
			}
		}catch(Exception e){
			//Log.e(TAG, e.toString());
		}
	}
	
	public void logError(final String message){
		AppLog.log(message);
		//final File path = getFilesDir();
		/*new Thread(new Runnable(){
			public void run(){
				apiLog.writeLog(message);
			}
		}).start();*/
	}
	
	private boolean isRecognizingSet(){
		boolean result;
		try{
            Resources res = getResources();
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			result = sp.getBoolean("knock_recog", res.getBoolean(R.bool.knock_recg_d));
		}catch(Exception e){
			result = true;
		}
		
		//Log.d(TAG, "Result is " + result);
		return result;
	}
	
	private void startDetectingUPD(){
		try{
			startService(new Intent(RohosApplication.this, UPDService.class));

			//Resources res = getResources();
			//SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			//if(sp.getBoolean("lock_if_leavs_conn", res.getBoolean(R.bool.lock_if_leav_d))){
			//	startService(new Intent(this, LockPCService.class));
			//}

            // This code is used for test only
			Message msg = mHandler.obtainMessage(START_RECOGNIZING_SERVICE);
			mHandler.sendMessage(msg);
		}catch(Exception e){
			//Log.e(TAG, e.toString());
		}
	}
	
	/*private void initNativeRecognizer(){
		try{
			mNativeKnockRecog.initRecognizing();
		}catch(Exception e){}
	}*/

}
