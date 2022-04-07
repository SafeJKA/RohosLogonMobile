package com.rohos.logon1;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import com.rohos.logon1.utils.AppLog;

import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

public class QRcodeScannerActivity extends AppCompatActivity implements ZBarScannerView.ResultHandler {

    private ZBarScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_qrcode_scanner);
        mScannerView = new ZBarScannerView(this);    // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result rawResult) {
        // Do something with the result here
        AppLog.log("QR result: " + rawResult.getContents()); // Prints scan results

        Uri uri = Uri.parse(rawResult.getContents());
        Intent result = new Intent();
        result.setData(uri);
        setResult(Activity.RESULT_OK, result);

        new ToneGenerator(AudioManager.STREAM_MUSIC, 100).startTone(ToneGenerator.TONE_PROP_BEEP, 200);
        //AppLog.log("QR result: " + rawResult.getBarcodeFormat().getName()); // Prints the scan format (qrcode, pdf417 etc.)
        //AppLog.log("QR result: " + rawResult.getBarcodeFormat().toString());

        // Note:
        // * Wait 1 second to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        // * I don't know why this is the case but I don't have the time to figure out.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.resumeCameraPreview(QRcodeScannerActivity.this);
                QRcodeScannerActivity.this.finish();
            }
        }, 1000L);
    }
}