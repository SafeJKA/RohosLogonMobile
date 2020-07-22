package com.rohos.logon1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

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

		}else if(action.equals(Intent.ACTION_SCREEN_ON)){
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            if(sp.getBoolean("unlock_on_table", true)){
				// This does not work in latest Android versions
                // context.startService(new Intent(context, DetectMovementService.class));
            }
			//Log.d(TAG, "SCRENN ON");
		}else if(action.equals(Intent.ACTION_USER_PRESENT)){
			//Log.d(TAG, "USER PRESENT");
		}
	}
}
