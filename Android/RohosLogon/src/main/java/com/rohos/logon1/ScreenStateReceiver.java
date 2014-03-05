package com.rohos.logon1;

/*
 * Copyright 2014 Tesline-Service SRL. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * ScreenOFF events listener
 *
 *
 *
 *
 * @author AlexShilon
 */

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
			try{
				Context appContext = context.getApplicationContext();				
				Intent control = new Intent(appContext, ControlActivity.class);
				control.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				appContext.startActivity(control);				
			}catch(Exception e){
				Log.e(TAG, e.toString());
			}
			Log.d(TAG, "SCREEN OFF");
		}else if(action.equals(Intent.ACTION_SCREEN_ON)){
			/*try{
				Context appContext = context.getApplicationContext();
				Intent control = new Intent(appContext, ControlActivity.class);
				control.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				appContext.startActivity(control);
			}catch(Exception e){
				Log.e(TAG, e.toString());
			}*/
			Log.d(TAG, "SCRENN ON");
		}else if(action.equals(Intent.ACTION_USER_PRESENT)){
			Log.d(TAG, "USER PRESENT");
		}
	}
}
