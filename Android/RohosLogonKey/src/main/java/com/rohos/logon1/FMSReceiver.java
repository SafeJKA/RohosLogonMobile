package com.rohos.logon1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.rohos.logon1.utils.AppLog;

import java.util.Iterator;
import java.util.Set;

public class FMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if(bundle != null){
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            String key;
            while(it.hasNext()){
                key = it.next();
                AppLog.log("FMSReceiver, key:" + key + " - value:" + bundle.get(key));
            }
        }else{
            AppLog.log("FMSReceiver, Bundle is null");
        }
    }
}