package com.rohos.logon1;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;

import android.os.Bundle;

import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rohos.logon1.fragments.NotificationsFragment;
import com.rohos.logon1.fragments.PlaceholderFragment;
import com.rohos.logon1.interfaces.IBooleanChanged;
import com.rohos.logon1.services.RoWorker;
import com.rohos.logon1.ui.ErrorDialog;
import com.rohos.logon1.ui.RemoveAccountDialog;
import com.rohos.logon1.utils.AppLog;

import java.util.ArrayList;
import java.util.List;


/* Main Activity
 * @author AlexShilon alex@rohos.com
 */
public class MainActivity extends AppCompatActivity implements IBooleanChanged {

    /**
     * Intent action to that tells this Activity to initiate the scanning of barcode to add an
     * account.
     */
    public static Handler mHandler;

    public static final int SET_RESULT_TEXT = 1001;

    // @VisibleForTesting
    private static final String ACTION_SCAN_BARCODE =
            MainActivity.class.getName() + ".ScanBarcode";

    // @VisibleForTesting
    private static final int SCAN_REQUEST = 31337;

    //public static final int DOWNLOAD_DIALOG = 0;
    //public static final int INVALID_QR_CODE = 1;

    private static final String LOCAL_TAG = "RohosActivity";
    private static final String PREFS_NAME = "RohosPrefs1";

    private static final String OTP_SCHEME = "rohos1";

    public static final int REMOVE_ID = 2;

    // Links
    //public static final String ZXING_MARKET =
    //        "market://search?q=pname:com.google.zxing.client.android";
    //public static final String ZXING_DIRECT =
    //        "https://zxing.googlecode.com/files/BarcodeScanner3.1.apk";

    private final String TAG = "MainActivity";

    private boolean mSaveKeyIntentConfirmationInProgress;

    private AuthRecordsDb mRecordsDb;
    private ListView mRecordsList;
    private AuthRecord[] mAuthRecords = {};
    private RecordsListAdapter mRecordsAdapter;
    //private TextView mAboutText;
    private MQTTSender mSender;

    // Callback for activity QRcodeScannerActivity to get QR code.
    private ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        Uri uri = intent.getData();
                        interpretScanResult(uri, false);

                        AppLog.log("QR received: " + uri.toString());
                        // Handle the Intent
                    }
                }
            });

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Button scanCode = findViewById(R.id.scan_barcode);
        scanCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBarcode();
            }
        });

        /*
        findViewById(R.id.helpButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHelp();
            }
        });

        mAboutText = findViewById(R.id.aboutText);
        mAboutText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUpdates();
            }
        });
        */

        Button unlockPCbtn = findViewById(R.id.unlock_pc);
        unlockPCbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    unlockPC();
                }catch(java.lang.Throwable e){
                    AppLog.log(Log.getStackTraceString(e));
                }
            }
        });

        mRecordsDb = new AuthRecordsDb(getApplicationContext());
        mRecordsAdapter = new RecordsListAdapter(this, R.layout.list_row_view, mAuthRecords);
        mRecordsList = findViewById(R.id.listView);
        mRecordsList.setAdapter(mRecordsAdapter);

        mRecordsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> unusedParent, View row,
                                    int unusedPosition, long unusedId) {

                String accName = ((TextView) row.findViewById(R.id.recordName)).getText().toString();
                String accHost = ((TextView) row.findViewById(R.id.hostName)).getText().toString();
                sendMqttLoginRequest(accName, accHost);
            }
        });

        refreshRecordsList(false);

        //fillAboutTextView();
    }

    @Override
    public void onResume() {

        //System.err.println("Received onResume() event");
        super.onResume();
    }

    @Override
    public void onPause() {
        //System.err.println("Received onPause() event");
        super.onPause();
    }

    @Override
    public void onStop() {
        //System.err.println("Received stop event");
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case SET_RESULT_TEXT:
                        String result = (String) msg.obj;
                        if (result != null)
                            ((TextView) findViewById(R.id.textQRcode)).setText(result);
                        break;
                }
            }
        };

        checkPermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showNotify:
                startActivity(new Intent(getApplicationContext(), NotificationsActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                return true;
            case R.id.action_check_updates:
                checkUpdates();
                return true;
            case R.id.show_api_log:
                startActivity(new Intent(this, ShowApiLog.class));
                return true;
            case R.id.get_token:
                copyFCMtokenToClipboard();
                //getFMStoken();
                return true;
            case R.id.send_token:
                sendTokenToPCs();
                return true;
            case R.id.howtoUse:
                showHelp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {

        //System.err.println("Received onDestroy() event");
        super.onDestroy();
    }

    // method of .interfaces.IBooleanChanged interface
    @Override
    public void onBooleanChanged(boolean newValue) {
        if(newValue){
            refreshRecordsList(true);
        }
    }

    public void returnToMain(){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new PlaceholderFragment())
                .commit();
    }


    private void checkUpdates() {

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.rohos.com/2013/12/login-unlock-computer-by-using-smartphone/"));
        startActivity(browserIntent);
    }

    private void sendMqttLoginRequest(String accountName, String hostName) {

        Resources res = getResources();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("use_bluetooth_unlock", res.getBoolean(R.bool.use_bluetooth_d))) {
            startService(new Intent(MainActivity.this, BTService.class));
            // Log.d(TAG, "Start BTService");
        }

        AuthRecord ar = mRecordsDb.getAuthRecord(accountName, hostName);

        if (ar.qr_user == null || ar.qr_user.length() == 0) {
            ((TextView) findViewById(R.id.textQRcode)).setText(String.format("Please install Rohos Logon Key on the desktop and scan QR-code first."));
            return;
        }

        if (mSender != null) {
            mSender.cancel(true);
            mSender = null;
        }

        mSender = new MQTTSender(this.getApplicationContext());
        mSender.execute(ar);
    }

    private void showHelp() {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, HelpActivity.class);
        startActivity(intent);
    }

    private void scanBarcode() {
        mStartForResult.launch(new Intent(getApplicationContext(), QRcodeScannerActivity.class));

        /*Intent intentScan = new Intent("com.google.zxing.client.android.SCAN");
        intentScan.putExtra("SCAN_MODE", "QR_CODE_MODE");
        intentScan.putExtra("SAVE_HISTORY", false);
        try {
            startActivityForResult(intentScan, SCAN_REQUEST);
        } catch (ActivityNotFoundException e) {
            // Log.e(TAG, Log.getStackTraceString(e));
            showDialog(0);
        }*/
    }

    /*private void fillAboutTextView() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            mAboutText = findViewById(R.id.aboutText);
            mAboutText.setText(getString(R.string.about_text, pi.versionName));
        } catch (Exception e) {
            // Log.e(TAG, e.toString());
        }
    }*/

    /**
     * Reacts to the {@link Intent} that started this activity or arrived to this activity without
     * restarting it (i.e., arrived via {@link #onNewIntent(Intent)}). Does nothing if the provided
     * intent is {@code null}.
     */
    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (ACTION_SCAN_BARCODE.equals(action)) {
            scanBarcode();
        } else if (intent.getData() != null) {
            interpretScanResult(intent.getData(), true);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        //Log.i(getString(R.string.app_name), LOCAL_TAG + ": onActivityResult");
        if (requestCode == SCAN_REQUEST && resultCode == Activity.RESULT_OK) {
            // Grab the scan results and convert it into a URI
            String scanResult = (intent != null) ? intent.getStringExtra("SCAN_RESULT") : null;
            Uri uri = (scanResult != null) ? Uri.parse(scanResult) : null;
            if (uri != null) {
                interpretScanResult(uri, false);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.listView) {
            super.onCreateContextMenu(menu, v, menuInfo);
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            String name = mAuthRecords[info.position].qr_user;
            String hostName = mAuthRecords[info.position].qr_host_name;
            menu.setHeaderTitle(name + " " + hostName);
            menu.add(0, REMOVE_ID, 0, R.string.remove);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case REMOVE_ID:
                RemoveAccountDialog dialog = new RemoveAccountDialog(mAuthRecords, mRecordsDb, info.position);
                dialog.show(getSupportFragmentManager(), "remove_account");
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /*
    @Override
    public void onBackPressed() {
        FragmentManager manager = getSupportFragmentManager();
        List<Fragment> fragmentList = manager.getFragments();
        if (fragmentList != null) {
            for (int i = 0; i < fragmentList.size(); i++) {
                Fragment f = fragmentList.get(i);
                if (f != null && f.isVisible()) {
                    String className = f.getClass().getSimpleName();
                    if(className.equals("NotificationsFragment")){
                        manager.popBackStack();
                    }else{
                        super.onBackPressed();
                    }
                    //AppLog.log("Fragment: " + f.getClass().getSimpleName() + ", " + f.isVisible());
                }
            }
        }
    }*/

    /**
     * This method is deprecated in SDK level 8, but we have to use it because the
     * new method, which replaces this one, does not exist before SDK level 8
     */
    /*@Override
    protected Dialog onCreateDialog(final int id) {
        Dialog dialog = null;
        switch (id) {
            //Prompt to download ZXing from Market. If Market app is not installed,
            //such as on a development phone, open the HTTPS URI for the ZXing apk.
            case DOWNLOAD_DIALOG:
                AlertDialog.Builder dlBuilder = new AlertDialog.Builder(this);
                dlBuilder.setTitle(R.string.install_dialog_title);
                dlBuilder.setMessage(R.string.install_dialog_message);
                dlBuilder.setIcon(android.R.drawable.ic_dialog_alert);
                dlBuilder.setPositiveButton(R.string.install_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(ZXING_MARKET));
                                try {
                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) { // if no Market app
                                    intent = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(ZXING_DIRECT));
                                    startActivity(intent);
                                }
                            }
                        }
                );
                dlBuilder.setNegativeButton(R.string.cancel, null);
                dialog = dlBuilder.create();
                break;

            case INVALID_QR_CODE:

                dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.error_title)
                        .setMessage(R.string.error_qr)
                        .create();
                break;

            default:
                break;
        }
        return dialog;
    }*/

    private void unlockPC() {
        try {
            Resources res = getResources();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            if (sp.getBoolean("use_bluetooth_unlock", res.getBoolean(R.bool.use_bluetooth_d))) {
                startService(new Intent(MainActivity.this, BTService.class));
                // Log.d(TAG, "Start BTService");
            }

            Runnable r = new Runnable(){
                @Override
                public void run() {
                    try{
                        Context ctx = getApplicationContext();
                        AuthRecordsDb authRecordDb = new AuthRecordsDb(ctx);

                        ArrayList<String> recordNames = new ArrayList<String>();
                        authRecordDb.getNames(recordNames);

                        for (int i = 0; i < recordNames.size(); i++) {
                            String name = recordNames.get(i).substring(0, recordNames.get(i).indexOf("|"));
                            String hostName = recordNames.get(i).substring(recordNames.get(i).indexOf("|")+1);
                            AuthRecord ar = authRecordDb.getAuthRecord(name, hostName);
                            MQTTSender sender = new MQTTSender(ctx);
                            sender.execute(ar);
                            Thread.sleep(400L);
                        }
                    }catch(java.lang.Throwable e){
                        AppLog.log(Log.getStackTraceString(e));
                    }
                }
            };
            new Thread(r).start();
        } catch (Exception e) {
            // Log.e(LOCAL_TAG, e.toString());
        }
    }

    public void refreshRecordsList(boolean isAccountModified) {
        ArrayList<String> recordNames = new ArrayList<String>();
        mRecordsDb.getNames(recordNames);

        int userCount = recordNames.size();

        if (userCount > 0) {
            boolean newListRequired = isAccountModified || mAuthRecords.length != userCount;
            if (newListRequired) {
                mAuthRecords = new AuthRecord[userCount];
            }

            for (int i = 0; i < userCount; ++i) {
                String name = recordNames.get(i).substring(0, recordNames.get(i).indexOf("|"));
                String host = recordNames.get(i).substring(recordNames.get(i).indexOf("|")+1);
                mAuthRecords[i] = mRecordsDb.getAuthRecord(name, host);
            }

            if (newListRequired) {
                mRecordsAdapter = new RecordsListAdapter(this, R.layout.list_row_view, mAuthRecords);
                mRecordsList.setAdapter(mRecordsAdapter);
            }

            mRecordsAdapter.notifyDataSetChanged();

            if (mRecordsList.getVisibility() != View.VISIBLE) {
                mRecordsList.setVisibility(View.VISIBLE);
            }

            registerForContextMenu(mRecordsList);

            ((TextView) findViewById(R.id.textQRcode)).setText(R.string.click_to_unlock);

        } else {
            mAuthRecords = new AuthRecord[0]; // clear any existing user PIN state
            mRecordsList.setVisibility(View.GONE);

        }
    }

    private void saveRecordAndRefreshList(AuthRecord ar) {
        mRecordsDb.update(ar);
        refreshRecordsList(true);
    }

    /**
     * Interprets the QR code that was scanned by the user.  Decides whether to
     * launch the key provisioning sequence or the OTP seed setting sequence.
     *
     * @param scanResult        a URI holding the contents of the QR scan result
     * @param confirmBeforeSave a boolean to indicate if the user should be
     *                          prompted for confirmation before updating the otp
     *                          account information.
     */
    private void interpretScanResult(Uri scanResult, boolean confirmBeforeSave) {
        ((TextView) findViewById(R.id.textQRcode)).setText(scanResult.toString());
        if (confirmBeforeSave) {
            if (mSaveKeyIntentConfirmationInProgress) {
                //  Log.w(LOCAL_TAG, "Ignoring save key Intent: previous Intent not yet confirmed by user");
                return;
            }
            mSaveKeyIntentConfirmationInProgress = true;
        }

        ErrorDialog dialog = new ErrorDialog();
        // Sanity check
        if (scanResult == null) {
            // Log.e(TAG, "Scan result is null");
            dialog.show(getSupportFragmentManager(), "error");
            //showDialog(INVALID_QR_CODE);
            return;
        }

        // See if the URL is an account setup URL containing a shared secret
        if (OTP_SCHEME.equals(scanResult.getScheme()) && scanResult.getAuthority() != null) {
            parseSecret(scanResult, confirmBeforeSave);
        } else {
            // Log.e(TAG, "getScheme " + scanResult.getScheme() + " getAuthority " + scanResult.getAuthority());
            dialog.show(getSupportFragmentManager(), "error");
            //showDialog(INVALID_QR_CODE);
        }
    }

    /**
     * Parses a secret value from a URI. The format will be:
     * <p>
     * QR code format:
     * rohos1://192.168.1.15:995/ZED?USER=Alex&KEY=XXXXXX&DATA=YYYYYY     *
     * <p>
     * <p>
     * where:
     * 192.168.1.15:995 - host IP and Port
     * ZED - host name (PC name)
     * Alex = user name whos login is configured | OR | secret Rohos disk name
     * KEY - encryption key (HEX)
     * DATA - authentication data (HEX)
     *
     * @param uri               The URI containing the secret key
     * @param confirmBeforeSave a boolean to indicate if the user should be
     *                          prompted for confirmation before updating the otp
     *                          account information.
     */
    private void parseSecret(Uri uri, boolean confirmBeforeSave) {
        try {
            /* final String scheme = uri.getScheme().toLowerCase(); */
            String url = uri.toString();
            url = url.replace('(', '&');
            uri = Uri.parse(url);

            AuthRecord ai = new AuthRecord();
            ai.url = url;
            ai.qr_host_name = uri.getPath().toUpperCase();
            ai.qr_host_ip = uri.getAuthority();
            ai.qr_host_port = 1205;


            int i = ai.qr_host_ip.indexOf(":");
            if (i > 0) {
                ai.qr_host_port = Integer.parseInt(ai.qr_host_ip.substring(i + 1));
                ai.qr_host_ip = ai.qr_host_ip.substring(0, i);
            }

            //ai.qr_host_ip = "1.1.1.1"; // for test only


            ((TextView) findViewById(R.id.textQRcode)).setText(uri.toString());

            ai.qr_user = uri.getQueryParameter("USER");
            ai.qr_secret_key = uri.getQueryParameter("KEY");
            ai.qr_data = uri.getQueryParameter("DATA");

            String str;
            str = String.format("QR code:\nIP: %s (%d)\nHOST:%s\nUser: %s", ai.qr_host_ip, ai.qr_host_port, ai.qr_host_name, ai.qr_user);
            ((TextView) findViewById(R.id.textQRcode)).setText(str);

            ((Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100L);

            saveRecordAndRefreshList(ai);

            sendMqttLoginRequest(ai.qr_user, ai.qr_host_name);

        } catch (java.lang.NumberFormatException err2) {
            ((TextView) findViewById(R.id.textQRcode)).setText(String.format(" %s", err2.toString()));
            // Log.e(TAG, Log.getStackTraceString(err2));
            ErrorDialog dialog = new ErrorDialog();
            dialog.show(getSupportFragmentManager(), "error");
            //showDialog(INVALID_QR_CODE);
        } catch (NullPointerException error) {
            ((TextView) findViewById(R.id.textQRcode)).setText(String.format(" %s", error.toString()));
            // Log.e(TAG, Log.getStackTraceString(error));
            ErrorDialog dialog = new ErrorDialog();
            dialog.show(getSupportFragmentManager(), "error");
            //showDialog(INVALID_QR_CODE);
        }
    }

    private void copyFCMtokenToClipboard(){
        try{
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String token = sp.getString("fcm_token", null);
            if(token == null){
                AppLog.log("Couldn't get token from preferences");
                Toast.makeText(getApplicationContext(), "Couldn't get token, try later", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("https://fcm.googleapis.com/fcm/send?key=AAAAM4Hs8K8:APA91bHsXcvArVjS3awAepIGzw-rFcR3YFKhOpwOrVpCoL5Q7oUyRgCRnZkfSLMfg19HKM0aQuyKV_e7qIdFCA_pI48cSSJaA8MpfO5CqNZJQyG2eEXHJXTorhczk7EakOSClIpQi_d3&to=");
            sb.append(token);
            //sb.append("&body=2FA bypass on PC.");


            ClipboardManager clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("token", sb.toString());
            clipboardManager.setPrimaryClip(clipData);

            Toast.makeText(getApplicationContext(), "Token is copied to Clipboard", Toast.LENGTH_SHORT).show();
        }catch(Exception e){
            AppLog.log(Log.getStackTraceString(e));
        }
    }

    private void sendTokenToPCs(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String token = sp.getString("fcm_token", null);
        if(token == null)
            return;

        Data.Builder builder = new Data.Builder();
        builder.putString("token", token);
        Data data = builder.build();

        OneTimeWorkRequest.Builder requestBuilder = new OneTimeWorkRequest.Builder(RoWorker.class);
        requestBuilder.setInputData(data);
        OneTimeWorkRequest workRequest = requestBuilder.build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.beginWith(workRequest).enqueue();
        workManager.getWorkInfoByIdLiveData(workRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if(workInfo != null){
                    Log.d(TAG, "Is worker finished: " + workInfo.getState().isFinished());
                }
            }
        });
    }

    private boolean hasPermissions(String[] permissions){
        boolean hasPerm = false;
        try{
            for(String permission : permissions){
                if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                    return false;
                }else{
                    hasPerm = true;
                }
            }
        }catch(Exception e){
            AppLog.log(Log.getStackTraceString(e));
        }
        return hasPerm;
    }

    private void getFMStoken(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();

                // Log and toast
                String msg = "Token:" + token;
                Log.d(TAG, msg);
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissions(){
        String[] permissions = new String[]{
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.POST_NOTIFICATIONS
        };

        if(!hasPermissions(permissions)){
            ActivityCompat.requestPermissions(this, permissions, permissions.length);
        }
    }

    /****** Inner classes ************************************************************************
     *
     * Displays the list of authentication records
     *
     * @author AlexShilon
     */
    private class RecordsListAdapter extends ArrayAdapter<AuthRecord> {

        public RecordsListAdapter(Context context, int userRowId, AuthRecord[] items) {
            super(context, userRowId, items);
        }

        /**
         * Displays the user and host name
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            AuthRecord currentRecord = getItem(position);

            View row;
            if (convertView != null) {
                // Reuse an existing view
                row = convertView;
            } else {
                // Create a new view
                row = inflater.inflate(R.layout.list_row_view, null);
            }

            TextView hostView = row.findViewById(R.id.hostName);
            hostView.setText(currentRecord.qr_host_name);

            ImageView imageView = row.findViewById(R.id.imageView);
            if(currentRecord.qr_host_ip.equals("1.1.1.1")){
                imageView.setImageResource(R.drawable.encrypt_folder);
                hostView.setVisibility(View.GONE);
            }else{
                imageView.setImageResource(R.drawable.small_pc);
                hostView.setVisibility(View.VISIBLE);
            }

            TextView nameView = row.findViewById(R.id.recordName);
            nameView.setText(currentRecord.qr_user);

            return row;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    /*public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }*/
}