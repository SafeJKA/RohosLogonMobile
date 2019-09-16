package com.rohos.logon1;

import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by yura on 11/17/15.
 */
public class BTService extends Service {
    //BT Service UUID to connect to
    //MUST match the one defined in the Windows App
    private static final UUID PT_SERVER_UUID = UUID
            .fromString("46D5833A-997C-4854-9139-8C3510622ACF");
    private final String TAG = "BTService";

    private BluetoothAdapter mBtAdapter;
    private Set<String> mFoundDevices = new HashSet<String>();
    private Socket mSocket = new Socket();
    private BluetoothSocket mBTSocket;
    private OutputStream mServerOutputStream = null;
    private Object sendLock = new Object();
    private AuthRecordsDb mRecordsDb;
    private ExecutorService mExecutor = Executors
            .newSingleThreadExecutor(new PrioThreadFactory());

    private boolean mIsEnabled = false;
    private volatile boolean mSendingData = false;

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            //Log.d(TAG, "Action - " + action);

            // When discovery finds a device
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Device name-" + device.getName());
                Log.d(TAG, "Device state-" + device.getBondState());
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    AuthRecord ar = mRecordsDb.getAuthRecordByHostName("/" + device.getName());
                    if (ar != null && ar.qr_host_name != null && ar.qr_host_name.length() > 0) {
                        String devName = ar.qr_host_name.substring(ar.qr_host_name.indexOf("/") + 1);

                        if (devName.equals(device.getName())) {
                            String encryptedAuthString = ar.getEncryptedDataString();
                            String str_data = String.format("%s.%s.%s", ar.qr_user,
                                    ar.qr_host_name, encryptedAuthString);

                            if (!isNetConnected()) connectToServer(device.getAddress());
                            Log.d(TAG, "Data to send: " + str_data);
                            mExecutor.submit(new SendRunnable(str_data + "\n"));

                            mSendingData = true;
                            Toast.makeText(BTService.this, "Trying to unlock " + device.getName(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    Log.d(TAG, "Bonded device-" + device.getName());
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                if (!mSendingData) BTService.this.stopSelf();
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                Toast.makeText(BTService.this, "BT device is disonnected", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onCreate() {
        mRecordsDb = new AuthRecordsDb(getApplicationContext());

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(mReceiver);
        disconnectFromServer();

        super.onDestroy();

        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        doDiscovery();

        if (mIsEnabled) unlockPC();

        return START_NOT_STICKY;
    }

    private void unlockPC() {
        try {
            if (mFoundDevices.size() > 0) {
                for (String s : mFoundDevices) {
                    Log.d(TAG, "Device name-" + s);
                }
            }

        } catch (Exception e) {
            Log.d(TAG, "unlockPC");
        }
    }

    private void sendData(byte[] array, int len) {
        try {
            synchronized (sendLock) {
                mServerOutputStream.write(array, 0, len);
                mServerOutputStream.flush();
                Log.d(TAG, "Bluetooth data sent...");
                Log.d(TAG, array.toString());
            }

        } catch (IOException e) {
            mExecutor.submit(new Runnable() {

                @Override
                public void run() {
                    disconnectFromServer();
                }
            });
            Log.d(TAG, "Error writing to output stream !!!");
            RohosApplication app = (RohosApplication) getApplication();
            if (app != null) app.logError(TAG + e.toString());
            BTService.this.stopSelf();
        }
    }

    private void connectToServer(String serverBTAddress) {
        try {
            mBtAdapter.cancelDiscovery();
            // Get the BluetoothDevice object
            BluetoothDevice device = mBtAdapter
                    .getRemoteDevice(serverBTAddress);

            mBTSocket = device
                    .createRfcommSocketToServiceRecord(PT_SERVER_UUID);
            mBTSocket.connect();

            mServerOutputStream = mBTSocket.getOutputStream();
        } catch (Exception e) {
            mSendingData = false;
            Log.e(TAG, e.toString());
            RohosApplication app = (RohosApplication) getApplication();
            if (app != null) app.logError(TAG + e.toString());
            BTService.this.stopSelf();
        }
    }

    public void disconnectFromServer() {
        try {
            if (mServerOutputStream != null) {
                mServerOutputStream.close();
                mServerOutputStream = null;
            }
            if (mBTSocket != null) {
                mBTSocket.close();
                mBTSocket = null;
            }

        } catch (Exception e) {
            RohosApplication app = (RohosApplication) getApplication();
            if (app != null) app.logError(TAG + e.toString());
            BTService.this.stopSelf();
            Log.e(TAG, "Error closing socket!!!");
            //e.printStackTrace();
        }
    }

    public boolean isNetConnected() {
        try {
            if (mBTSocket == null) {
                return false;
            } else {
                return mBTSocket.isConnected();
            }
        } catch (Exception e) {
            RohosApplication app = (RohosApplication) getApplication();
            if (app != null) app.logError(TAG + e.toString());
            BTService.this.stopSelf();
            Log.e(TAG, e.toString());
        }
        return false;
    }

    private void doDiscovery() {
        try {
            mIsEnabled = mBtAdapter.isEnabled();
            if (!mIsEnabled) return;

            mFoundDevices.clear();

            // If we're already discovering, stop it
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }

            // Request discover from BluetoothAdapter
            mBtAdapter.startDiscovery();
            Log.d(TAG, "doDiscovery");
        } catch (Exception e) {
            RohosApplication app = (RohosApplication) getApplication();
            if (app != null) app.logError(TAG + e.toString());
            Log.e(TAG, e.toString());
        }
    }

    public class MyBluetoothDevice {
        private BluetoothDevice mBTD = null;

        MyBluetoothDevice(BluetoothDevice btd) {
            mBTD = btd;
        }

        @Override
        public String toString() {
            return mBTD.getName();
        }

        public String getAddress() {
            return mBTD.getAddress();
        }
    }

    private class SendRunnable implements Runnable {
        String mSendData;

        public SendRunnable(String sendData) {
            mSendData = sendData;
        }

        @Override
        public void run() {
            sendData(mSendData.getBytes(), mSendData.length());
            BTService.this.stopSelf();
        }
    }

    private class PrioThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.NORM_PRIORITY);
            final int prio = thread.getPriority();
            return thread;
        }
    }
}
