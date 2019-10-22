package com.rohos.logon1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by yura on 12/21/15.
 */
public class BTConnReceiver extends BroadcastReceiver {

    private final String TAG = "BTConnReceiver";

    @Override
    public void onReceive(Context context, Intent intent){
        if(context == null || intent == null) return;

        try{
            RohosApplication app = (RohosApplication)context.getApplicationContext();
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
               // app.logError(TAG + ", BT device connected");
                Toast.makeText(context, "BT device connected", Toast.LENGTH_LONG);
            }else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
               // app.logError(TAG + ", BT device disconnected");
                Toast.makeText(context, "BT device disconnected", Toast.LENGTH_LONG);
            }else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
               // app.logError(TAG + ", BT bond state changed");
                Toast.makeText(context, "BT bond state changed", Toast.LENGTH_LONG);
            }else if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
              //  app.logError(TAG + ", BT connection state changed");
                Toast.makeText(context, "BT connection state changed", Toast.LENGTH_LONG);
            }
        }catch(Exception e){
           // Log.e(TAG, e.toString());
        }
    }
}
