package com.rohos.logon1;

/*
 * Copyright 2014 Tesline-Service SRL. All Rights Reserved.
 * www.rohos.com
 * Rohos Logon Key
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */


import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

public class RohosApplication extends Application {
	
	//private ScreenStateReceiver mScreenState = null;
	
	@Override
	public void onCreate(){
		super.onCreate();
		/*
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        
        mScreenState = new ScreenStateReceiver();
        registerReceiver(mScreenState, intentFilter);
        */
	}
}
