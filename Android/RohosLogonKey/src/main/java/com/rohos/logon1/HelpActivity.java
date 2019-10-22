package com.rohos.logon1;

/*
 * Copyright 2014 Tesline-Service SRL. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


/**
 * Created by Alex on 03.01.14.
 * Rohos Loogn Key How it works help
 *
 * @author AlexShilon
 */
public class HelpActivity extends Activity {

    private final String TAG = "HelpActiviy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_help);

        Button learnMore = findViewById(R.id.learn_more);
        learnMore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openWebPage();
            }
        });

        //setPageContentView(R.layout.activity_help);
        //setTextViewHtmlFromResource(R.id.details, R.string.howitworks_page_enter_code_details);
    }

    private void openWebPage() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.rohos.com/2013/12/login-unlock-computer-by-using-smartphone/"));
            startActivity(intent);
        } catch (Exception e) {
           // Log.e(TAG, e.toString());
        }
    }

    /*@Override
    protected void onRightButtonPressed() {
        //startPageActivity(IntroVerifyDeviceActivity.class);
    }*/
}
