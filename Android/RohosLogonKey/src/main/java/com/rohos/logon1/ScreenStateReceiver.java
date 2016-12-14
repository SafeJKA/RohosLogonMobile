package com.rohos.logon1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.rohos.logon1.services.DetectMovementService;

public class ScreenStateReceiver extends BroadcastReceiver {
	
	private final String TAG = "ScreenState";
	
	@Override
	public void onReceive(Context context, Intent intent){
		if(context == null || intent == null)
			return;
		
		String action = intent.getAction();
		if(action == null)
			return;
		else if(action.equals(Intent.ACTION_SCREEN_OFF)){
			/*try{
				Context appContext = context.getApplicationContext();				
				Intent control = new Intent(appContext, ControlActivity.class);
				control.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				appContext.startActivity(control);				
			}catch(Exception e){
				Log.e(TAG, e.toString());
			}*/
			//Log.d(TAG, "SCREEN OFF");
		}else if(action.equals(Intent.ACTION_SCREEN_ON)){
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            if(sp.getBoolean("unlock_on_table", true)){
                context.startService(new Intent(context, DetectMovementService.class));
            }
			//Log.d(TAG, "SCRENN ON");
		}else if(action.equals(Intent.ACTION_USER_PRESENT)){
			//Log.d(TAG, "USER PRESENT");
		}
	}
}
