package com.rohos.logon1.widget;

import java.util.ArrayList;

import com.rohos.logon1.AuthRecord;
import com.rohos.logon1.AuthRecordsDb;
import com.rohos.logon1.NetworkSender;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class UnlockPcService extends Service {
	
	public static Handler mHandler = null;
	
	public static final int UNLOCK_PC = 1001;
	public static final int FINISH_SERVICE = 1002;
	
	private final String TAG = "UnlockPcService";
	
	private AuthRecordsDb mAuthRecordsDb;
	private NetworkSender mNetSender;
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
					String name = (String)msg.obj;
					if(name != null)
						sendIpLogin(name);
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
				ArrayList<String> recordNames = new ArrayList<String>();
				mAuthRecordsDb.getNames(recordNames);
				
				if(recordNames.size() > 0){
					String name = recordNames.get(0);
					Message msg = mHandler.obtainMessage(UNLOCK_PC, name);
					mHandler.sendMessageDelayed(msg, 100L);
					//Log.d(TAG, name);
				}
			}			
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
		//Log.d(TAG, "onStartCommand");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		mHandler = null;
		mIsStarted = false;
		//Log.d(TAG, "onDestroy");
	}
	
	/*
    send Authentication data block to the network
     */
    private void sendIpLogin(String accountName){
    	//Log.d(TAG, "sendIpLogin, param accountName " + accountName);
        /*
        AuthRecord ar = mAuthRecordsDb.getAuthRecord(accountName);

        if (ar.qr_user == null || ar.qr_user.length()==0){
        	Log.e(TAG, "Please install Rohos Logon Key on the desktop and scan QR-code first.");
            //((TextView) findViewById(R.id.textQRcode)).setText(String.format("Please install Rohos Logon Key on the desktop and scan QR-code first."));
            return;
        }

        if (mNetSender != null){
            mNetSender.cancel(true);
            mNetSender = null;
        }

        mNetSender = new NetworkSender(getApplicationContext());
        mNetSender.execute(ar);
*/

        ArrayList<String> recordNames = new ArrayList<String>();
        mAuthRecordsDb.getNames(recordNames);

        int userCount = recordNames.size();

        if (userCount > 0) {

            for (int i = 0; i < userCount; ++i) {
                String name = recordNames.get(i);
                AuthRecord ar1  = mAuthRecordsDb.getAuthRecord(name);

                NetworkSender  mNetSender1 = new NetworkSender(getApplicationContext());
                mNetSender1.execute(ar1);
            }
        }

    }
}
