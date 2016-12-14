package com.rohos.logon1.widget;

import java.util.ArrayList;

import com.rohos.logon1.AuthRecord;
import com.rohos.logon1.AuthRecordsDb;
import com.rohos.logon1.BTService;
import com.rohos.logon1.NetworkSender;
import com.rohos.logon1.R;
import com.rohos.logon1.RohosApplication;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class UnlockPcService extends Service {
	
	public static Handler mHandler = null;
	
	public static final int UNLOCK_PC = 1001;
	public static final int FINISH_SERVICE = 1002;
	
	private final String TAG = "UnlockPcService";
	
	private AuthRecordsDb mAuthRecordsDb;
	//private NetworkSender mNetSender;
	private boolean mIsStarted = false;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate(){
		mAuthRecordsDb  = new AuthRecordsDb(getApplicationContext());
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				super.handleMessage(msg);
				switch(msg.what){
				case UNLOCK_PC:
					sendIpLogin();
					unlockPC();
					break;
				case FINISH_SERVICE:
					stopSelf();
					break;
				}
			}
		};
		
		//Log.d(TAG, "onCreate");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		try{
			if(!mIsStarted){
				mIsStarted = true;
				
				Message msg = mHandler.obtainMessage(UNLOCK_PC);
					mHandler.sendMessageDelayed(msg, 100L);
				Log.d(TAG, "Unlock PC message is sent");
			}			
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
		Log.d(TAG, "onStartCommand");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		mHandler = null;
		mIsStarted = false;
		Log.d(TAG, "onDestroy");
	}
	
	/*
    send Authentication data block to the network
     */
	private void sendIpLogin(){
		try{
			//RohosApplication app = (RohosApplication)getApplication();
			//AuthRecord ar = mAuthRecordsDb.getAuthRecordByHostName(new String(app.mHostName));
    		//NetworkSender mNetSender = new NetworkSender(getApplicationContext());
    		//mNetSender.execute(ar);
			

			ArrayList<String> recordNames = new ArrayList<String>();
	        mAuthRecordsDb.getNames(recordNames);

	        int userCount = recordNames.size();

	        if(userCount > 0){
	            for(int i = 0; i < userCount; ++i){
	                String name = recordNames.get(i);
	                AuthRecord ar1  = mAuthRecordsDb.getAuthRecord(name);

	                NetworkSender  mNetSender1 = new NetworkSender(getApplicationContext());
	                mNetSender1.execute(ar1);
	            }
	        }

		}catch(Exception e){
            Log.e(TAG, Log.getStackTraceString(e));
			stopSelf();
		}
    }

	private void unlockPC(){
		try{
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			if(sp.getBoolean("use_bluetooth_unlock", getResources().getBoolean(R.bool.use_bluetooth_d))){
				startService(new Intent(UnlockPcService.this, BTService.class));
			}
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
	}
}
