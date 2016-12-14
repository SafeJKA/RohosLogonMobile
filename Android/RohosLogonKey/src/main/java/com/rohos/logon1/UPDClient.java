package com.rohos.logon1;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import android.os.Message;
import android.util.Log;

public class UPDClient implements Runnable {
	
	private final String TAG = "UPDClient";
	
	private DatagramSocket mDatagramSocket;
	private DatagramPacket mDatagramPacket;
	
	private byte[] mMessage = new byte[100];
	
	private RohosApplication mApp;
	
	public UPDClient(RohosApplication app){
		mApp = app;
	}
	
	@Override
	public void run(){			
		AuthRecordsDb recordsDb = null;
		try{
			String text = new String();
			int server_port = 1206;
			mDatagramPacket = new DatagramPacket(mMessage, mMessage.length);				
			//mDatagramSocket = new DatagramSocket(server_port);
			mDatagramSocket = new DatagramSocket(null);
			mDatagramSocket.setSoTimeout(10000);
			mDatagramSocket.setBroadcast(true);
			mDatagramSocket.setReuseAddress(true);
			mDatagramSocket.bind(new InetSocketAddress(server_port));
			mDatagramSocket.receive(mDatagramPacket);
			text = new String(mMessage, 0, mDatagramPacket.getLength());
			
			recordsDb = new AuthRecordsDb(mApp.getApplicationContext());
			String host = "/" + text.substring(text.indexOf(":") + 1, text.length() - 1);
			
			mApp.logError("Get UPD, host " + host);
			
			if(recordsDb.hostExists(host)){
				//if(!mApp.mWaked){
				//	mApp.mWaked = true;
				//	mApp.wakeLock();
				//}
				
				mApp.mHostName = new String(host);
				
				//Message msg = mApp.mHandler.obtainMessage(mApp.START_RECOGNIZING_SERVICE);
				//mApp.mHandler.sendMessage(msg);
			}
			
			Log.d(TAG, "message " + text);						
		}catch(SocketTimeoutException se){
			//Log.d(TAG, "Timeout");
		}catch(Exception e){
			mApp.logError(e.toString());
			Log.e(TAG, Log.getStackTraceString(e));
		}finally{
			if(recordsDb != null){
				recordsDb.close();
				recordsDb = null;
			}
			
			if(mDatagramSocket != null){
				mDatagramSocket.close();
				mDatagramSocket = null;
			}
			
			//if(mApp.mContinueDetecting){
				new Thread(new UPDClient(mApp)).start();
			//}
		}
	}		
}
