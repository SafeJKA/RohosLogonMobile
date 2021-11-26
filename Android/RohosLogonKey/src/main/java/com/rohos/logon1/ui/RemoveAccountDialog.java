package com.rohos.logon1.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.rohos.logon1.AuthRecord;
import com.rohos.logon1.AuthRecordsDb;
import com.rohos.logon1.R;
import com.rohos.logon1.interfaces.IBooleanChanged;
import com.rohos.logon1.utils.AppLog;

public class RemoveAccountDialog extends DialogFragment {

    private AuthRecord[] mAuthRecords = null;
    private AuthRecordsDb mAuthRecordsDb = null;
    private int mPosition = 0;
    private IBooleanChanged mIBooleanChanged = null;

    public RemoveAccountDialog(AuthRecord[] authRecords, AuthRecordsDb authRecordsDb, int position){
        mAuthRecords = authRecords;
        mAuthRecordsDb = authRecordsDb;
        mPosition = position;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String recordName = mAuthRecords[mPosition].qr_user; // final so listener can see value
        String recordHostName = mAuthRecords[mPosition].qr_host_name; // final so listener can see value

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.remove_account_title, recordName));
        builder.setMessage(getString(R.string.remove_account_info, recordHostName));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int whichButton) {
                mAuthRecordsDb.delete(recordHostName, recordName);
                if(mIBooleanChanged != null){
                    mIBooleanChanged.onBooleanChanged(true);
                }
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try{
            Activity activity = getActivity();
            mIBooleanChanged = (IBooleanChanged)activity;
        }catch(ClassCastException e){
            AppLog.log("RemoveAccountDialog; Activity doesn't implements interface IBooleanChanged");
        }
    }
}
