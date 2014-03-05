package com.rohos.logon1;

/*
 * Copyright 2014 Tesline-Service SRL. All Rights Reserved.
 * www.rohos.com
 * Rohos Logon Key
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */



import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

public class ControlActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);	
		
		//Window win = getWindow();
		//win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON 
 		//	   | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		//View view = win.getDecorView();
		//view.setBackgroundColor(268435456);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON 
 			   | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		
		//WindowManager.LayoutParams lp = getWindow().getAttributes();
        //lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        //lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		
		setContentView(R.layout.control_activity);	
		
		ImageButton ib = (ImageButton)findViewById(R.id.btn_stop);
		ib.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ControlActivity.this.finish();
			}			
		});
		
		Button close = (Button)findViewById(R.id.btn_close);
		close.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				ControlActivity.this.finish();
			}			
		});
	}	
}
