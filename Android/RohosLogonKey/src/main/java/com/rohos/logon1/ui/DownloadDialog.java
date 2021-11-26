package com.rohos.logon1.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.rohos.logon1.MainActivity;
import com.rohos.logon1.R;

public class DownloadDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder dlBuilder = new AlertDialog.Builder(getActivity());
        dlBuilder.setTitle(R.string.install_dialog_title);
        dlBuilder.setMessage(R.string.install_dialog_message);
        dlBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        dlBuilder.setPositiveButton(R.string.install_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(MainActivity.ZXING_MARKET));
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) { // if no Market app
                            intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(MainActivity.ZXING_DIRECT));
                            startActivity(intent);
                        }
                    }
                }
        );
        dlBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        return dlBuilder.create();
    }
}
