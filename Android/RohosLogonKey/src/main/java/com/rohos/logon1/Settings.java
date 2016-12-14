package com.rohos.logon1;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.app.FragmentManager;
import android.util.Log;

import com.rohos.logon1.services.KnockService;

public class Settings extends PreferenceActivity {

	private final String TAG = "Settings";
	
	@Override
	protected void onCreate(Bundle saveInstanceState){
		super.onCreate(saveInstanceState);
		try{
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();

			PrefsFragment pf = new PrefsFragment();
			ft.replace(android.R.id.content, pf);
			ft.commit();
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
	}

	@Override
	protected void onStop(){
		super.onStop();
		
		try{
			RohosApplication app = (RohosApplication)getApplication();
			app.mTestKnockMod = false;
		}catch(Exception e){}
	}

	public void showAlertDialog(){
		try{
			new AlertDialog.Builder(this)
					.setTitle("Test knock")
					.setMessage("Knock on the device")
					.setCancelable(false)
					.setPositiveButton("Finish", new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							RohosApplication app = (RohosApplication)getApplication();
							app.mTestKnockMod = false;

							// Prevent to stop KnockService if the service was started by UPDClient
							if(app.mContinueDetecting){
								stopService(new Intent(Settings.this, KnockService.class));
							}

							dialog.dismiss();
						}
					})
					.show();
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
	}
}
