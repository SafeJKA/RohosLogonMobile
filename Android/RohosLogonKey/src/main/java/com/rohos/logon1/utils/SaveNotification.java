package com.rohos.logon1.utils;

import android.content.Context;
import android.util.Log;

import com.rohos.logon1.AuthRecordsDb;
import com.rohos.logon1.NotifyRecord;

public class SaveNotification implements Runnable {

    private Context mCtx = null;
    private String mBody = null;
    private String mTitle = null;
    private long mTimeSent = 0L;

    public SaveNotification(Context ctx){
        mCtx = ctx;
    }

    @Override
    public void run() {
        try{
            NotifyRecord nr = new NotifyRecord(mBody);
            nr.setTitle(mTitle);
            nr.setTimeSent(mTimeSent);

            AuthRecordsDb db = new AuthRecordsDb(mCtx);
            db.insertNotify(nr);
        }catch(Exception e){
            AppLog.log(Log.getStackTraceString(e));
        }
    }

    public void setBody(String body){
        mBody = new String(body);
    }

    public void setTitle(String title){
        mTitle = new String(title);
    }

    public void setTimeSent(long value){
        mTimeSent = value;
    }
}
