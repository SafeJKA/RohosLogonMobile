package com.rohos.logon1;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.rohos.logon1.utils.AppLog;

import java.io.File;
import java.io.FileOutputStream;

public class ShowApiLog extends AppCompatActivity {

    private final String TAG = "ShowApiLog";
    private final int PERM_REQUEST_CODE = 111;

    private TextView mOutput;
    private AppLog mAppLog = null;
    private Handler mHandler = null;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        mAppLog = AppLog.getInstance();

        setContentView(R.layout.show_api_log);
        mOutput = (TextView)findViewById(R.id.logs_view);

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                // Messages are sent from AppLog.readLogLineByLine
                String line = (String)msg.obj;
                mOutput.append(line);
                mOutput.append("\n");
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mAppLog != null){
            mOutput.setText("");
            Message msg = Message.obtain(mHandler);
            mAppLog.readLogLineByLine(msg);
        }
    }

    @Override
    protected void onDestroy(){
        if(mHandler != null)
            mHandler = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_show_apilog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.send_logs:
                sendEmail();
                break;
            case R.id.copy_log_sdcard:
                ActivityCompat.requestPermissions( this,
                        new String[]{
                                "android.permission.WRITE_EXTERNAL_STORAGE",
                                "android.permission.READ_EXTERNAL_STORAGE"
                        }, PERM_REQUEST_CODE
                );

                copyLogToCard();
                break;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERM_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                copyLogToCard();
            }else{
                Toast.makeText(getApplicationContext(), "Application requires  permission to write on SD card",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendEmail(){
        try{
            CharSequence cs = mOutput.getText();

            String emailAddr = "rockm.devt@gmail.com";

            String subject = "Android " + Build.VERSION.RELEASE + " (" +
                    Settings.getApiVersion(ShowApiLog.this) + ") AppLog";

            StringBuilder sb = new StringBuilder();
            sb.append(subject.concat(", "));
            sb.append(Build.MANUFACTURER.toUpperCase().concat(" "));
            sb.append(Build.MODEL.toUpperCase());
            sb.append("\r\n\r\n");
            sb.append(cs);

            //File f = new File(getFilesDir(), "errors.txt");
            final Intent email = new Intent(Intent.ACTION_SEND);
            email.setType("text/plain");
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddr});
            email.putExtra(Intent.EXTRA_SUBJECT, subject);
            email.putExtra(Intent.EXTRA_TEXT, sb.toString());

            Intent intent = Intent.createChooser(email, "Send mail ...");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Context ctx = getApplicationContext();
            ctx.startActivity(intent);
        }catch(Exception e){
            AppLog.log(Log.getStackTraceString(e));
        }
    }

    private void copyLogToCard(){
        new Thread(new Runnable(){
            public void run(){
                StringBuilder sb = new StringBuilder();
                sb.append("Android ");
                sb.append(Build.VERSION.RELEASE + " (");
                sb.append(Settings.getApiVersion(getApplicationContext()));
                sb.append("), ");
                sb.append(Build.MANUFACTURER.toUpperCase().concat(" "));
                sb.append(Build.MODEL.toUpperCase());
                sb.append("\r\n\r\n");
                sb.append(mOutput.getText());

                byte[] buff = sb.toString().getBytes();
                if(buff == null) return;

                FileOutputStream fos = null;
                try{
                    File f = new File(Environment.getExternalStorageDirectory(), "rohoslogon.log");
                    fos = new FileOutputStream(f, false);
                    fos.write(buff);
                    fos.flush();
                    fos.close();

                    Runnable r = new Runnable(){
                        public void run() {
                            Context ctx = getApplicationContext();
                            Toast.makeText(ctx, "File is copied to SD card.", Toast.LENGTH_SHORT).show();
                        }
                    };
                    runOnUiThread(r);
                }catch(Exception e){
                    AppLog.log(Log.getStackTraceString(e));
                }
            }
        }).start();
    }

}
