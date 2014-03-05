package com.rohos.logon1;

/*
 * Copyright 2014 Tesline-Service SRL. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

import android.app.Activity;
import android.os.Bundle;


/**
 * Created by Alex on 03.01.14.
 * Rohos Loogn Key How it works help
 *
 *
 *
 * @author AlexShilon
 */
public class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_help);

        //setPageContentView(R.layout.activity_help);
        //setTextViewHtmlFromResource(R.id.details, R.string.howitworks_page_enter_code_details);
    }

    /*@Override
    protected void onRightButtonPressed() {
        //startPageActivity(IntroVerifyDeviceActivity.class);
    }*/
}
