package com.rohos.logon1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


/* Main Activity
 * @author AlexShilon alex@rohos.com
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Intent action to that tells this Activity to initiate the scanning of barcode to add an
     * account.
     */
    public static Handler mHandler;

    public static final int SET_RESULT_TEXT = 1001;

    // @VisibleForTesting
    static final String ACTION_SCAN_BARCODE =
            MainActivity.class.getName() + ".ScanBarcode";

    // @VisibleForTesting
    static final int SCAN_REQUEST = 31337;

    public static final int DOWNLOAD_DIALOG = 0;
    public static final int INVALID_QR_CODE = 1;

    private static final String LOCAL_TAG = "RohosActivity";
    private static final String PREFS_NAME = "RohosPrefs1";

    private static final String OTP_SCHEME = "rohos1";

    public static final int REMOVE_ID = 2;

    // Links
    public static final String ZXING_MARKET =
            "market://search?q=pname:com.google.zxing.client.android";
    public static final String ZXING_DIRECT =
            "https://zxing.googlecode.com/files/BarcodeScanner3.1.apk";

    private final String TAG = "MainActivity";

    private boolean mSaveKeyIntentConfirmationInProgress;

    private NetworkSender netSender;
    private AuthRecordsDb mRecordsDb;
    private ListView mRecordsList;
    private AuthRecord[] mAuthRecords = {};
    private RecordsListAdapter mRecordsAdapter;
    private TextView mAboutText;
    private MQTTSender mSender;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        /*findViewById(R.id.unlock_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIpLogin("");
            }
        });*/

        findViewById(R.id.scan_barcode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBarcode();
            }
        });

        findViewById(R.id.helpButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHelp();
            }
        });

        mAboutText = (TextView) findViewById(R.id.aboutText);
        mAboutText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUpdates();
            }
        });

        Button unlocPC = (Button) findViewById(R.id.unlock_pc);
        unlocPC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlockPC();
            }
        });

        /* todo - display version from XML
        String versionName = this.getPackageManager()
                .getPackageInfo(this.getPackageName(), 0).versionName;*/

        mRecordsDb = new AuthRecordsDb(getApplicationContext());
        mRecordsAdapter = new RecordsListAdapter(this, R.layout.list_row_view, mAuthRecords);
        mRecordsList = (ListView) findViewById(R.id.listView);
        mRecordsList.setAdapter(mRecordsAdapter);

        mRecordsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> unusedParent, View row,
                                    int unusedPosition, long unusedId) {

                //get the data from the database here
                //decode the data using getEncryptedDataString function from AuthRecord class
                //Send this data using MQTT
                //Would be nice to create some form of dialog with log information when the user taps the record from the list
                String accName = ((TextView) row.findViewById(R.id.recordName)).getText().toString();

                sendMqttLoginRequest(accName);
            }
        });

        refreshRecordsList(false);

        fillAboutTextView();

        /*if ( ai = null || ai.qr_secret_key == null)
        {
            ((TextView) findViewById(R.id.textQRcode)).setText( "No records" );
        }*/


        if (savedInstanceState == null) {

            //  for testing purposes
            // interpretScanResult( Uri.parse("rohos1://192.168.1.8:1005/ZED?USER=Alex&KEY=XXXXXX&DATA=YYYYYY"), false);
            // interpretScanResult( Uri.parse("rohos1://192.168.1.8:1005/zed?USER=Alex(KEY=538a44883958c2961c3c10e419c931ab(DATA=6c345545645664a802938219371bb832e3cc503373aa0b6f6ed1b07f41e52005728e092d468dff6841f9d2c34e5f6ad6cc8aee67792f693c828a018e16bfa41ad08bb7132298afdfa9e70df29b023661"), false);
            // interpretScanResult( Uri.parse("rohos1://192.168.1.1:1005/zed123?USER=AlexAnder(KEY=538a44883958c2961c3c10e419c931ab(DATA=6c5345645645745672938219371bb832e3cc503373aa0b6f6ed1b07f41e52005728e092d468dff6841f9d2c34e5f6ad6cc8aee67792f693c828a018e16bfa41ad08bb7132298afdfa9e70df29b023661"), false);

        } else {

        }
    }

    @Override
    public void onResume() {

        System.err.println("Received onResume() event");
        super.onResume();
    }

    @Override
    public void onPause() {
        System.err.println("Received onPause() event");
        super.onPause();
    }

    @Override
    public void onStop() {
        System.err.println("Received stop event");
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

        mHandler = new Handler() {
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
        //startService(new Intent(MainActivity.this, UPDService.class));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                return true;
            case R.id.action_check_updates:
                checkUpdates();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {

        System.err.println("Received onDestroy() event");
        super.onDestroy();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }


    /*
    open Rohos URL.
     */
    private void checkUpdates() {

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.rohos.com/2013/12/login-unlock-computer-by-using-smartphone/"));
        startActivity(browserIntent);

    }

    //save the topic in the database
    //use it when sending the message
    //do the same and on the PC side
    /*
     * try to unlock using MQTT
     *
     *
     * */

    private void sendMqttLoginRequest(String accountName) {

        Resources res = getResources();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("use_bluetooth_unlock", res.getBoolean(R.bool.use_bluetooth_d))) {
            startService(new Intent(MainActivity.this, BTService.class));
            //Log.d(TAG, "Start BTService");
        } else {

        }

        AuthRecord ar = mRecordsDb.getAuthRecord(accountName);

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

    /*
    send Authentication data block to the network
     */
    private void sendIpLogin(String accountName) {
        // Try to unlock PC via bluetooth
      /*  Resources res = getResources();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("use_bluetooth_unlock", res.getBoolean(R.bool.use_bluetooth_d))) {
            startService(new Intent(MainActivity.this, BTService.class));
            //Log.d(TAG, "Start BTService");
        }*/
        AuthRecord ar = mRecordsDb.getAuthRecord(accountName);

        if (ar.qr_user == null || ar.qr_user.length() == 0) {
            ((TextView) findViewById(R.id.textQRcode)).setText(String.format("Please install Rohos Logon Key on the desktop and scan QR-code first."));
            return;
        }

        if (netSender != null) {
            netSender.cancel(true);
            netSender = null;
        }

        netSender = new NetworkSender(this.getApplicationContext());
        netSender.execute(ar);

    }

    private void showHelp() {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, HelpActivity.class);
        //Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    private void scanBarcode() {

        Intent intentScan = new Intent("com.google.zxing.client.android.SCAN");
        intentScan.putExtra("SCAN_MODE", "QR_CODE_MODE");
        intentScan.putExtra("SAVE_HISTORY", false);
        try {
            startActivityForResult(intentScan, SCAN_REQUEST);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            showDialog(0);
        }
    }

    private void fillAboutTextView() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            mAboutText = (TextView) findViewById(R.id.aboutText);
            mAboutText.setText(getString(R.string.about_text, pi.versionName));
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

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
        //Log.i(getString(R.string.app_name), LOCAL_TAG + ": onActivityResult");
        if (requestCode == SCAN_REQUEST && resultCode == Activity.RESULT_OK) {
            // Grab the scan results and convert it into a URI
            String scanResult = (intent != null) ? intent.getStringExtra("SCAN_RESULT") : null;
            Uri uri = (scanResult != null) ? Uri.parse(scanResult) : null;
            interpretScanResult(uri, false);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.listView) {
            super.onCreateContextMenu(menu, v, menuInfo);
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(mAuthRecords[info.position].qr_user + " " + mAuthRecords[info.position].qr_host_name);

            /*String[] menuItems = getResources().getStringArray(R.array.menu);
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }*/
            menu.add(0, REMOVE_ID, 0, R.string.remove);
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Intent intent;
        final String recordName = mAuthRecords[info.position].qr_user; // final so listener can see value
        final String recordHostName = mAuthRecords[info.position].qr_host_name; // final so listener can see value
        switch (item.getItemId()) {
          /*  case COPY_TO_CLIPBOARD_ID:
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(mUsers[(int) info.id].pin);
                return true;
            case CHECK_KEY_VALUE_ID:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(this, CheckCodeActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
                return true;
            case RENAME_ID:
                final Context context = this; // final so listener can see value
                final View frame = getLayoutInflater().inflate(R.layout.rename,
                        (ViewGroup) findViewById(R.id.rename_root));
                final EditText nameEdit = (EditText) frame.findViewById(R.id.rename_edittext);
                nameEdit.setText(user);
                new AlertDialog.Builder(this)
                        .setTitle(String.format(getString(R.string.rename_message), user))
                        .setView(frame)
                        .setPositiveButton(R.string.submit,
                                this.getRenameClickListener(context, user, nameEdit))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            case REMOVE_ID:
                // Use a WebView to display the prompt because it contains non-trivial markup, such as list
                View promptContentView =
                        getLayoutInflater().inflate(R.layout.remove_account_prompt, null, false);
                WebView webView = (WebView) promptContentView.findViewById(R.id.web_view);
                webView.setBackgroundColor(Color.TRANSPARENT);
                // Make the WebView use the same font size as for the mEnterPinPrompt field
                double pixelsPerDip =
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()) / 10d;
                webView.getSettings().setDefaultFontSize(
                        (int) (mEnterPinPrompt.getTextSize() / pixelsPerDip));
                Utilities.setWebViewHtml(
                        webView,
                        "<html><body style=\"background-color: transparent;\" text=\"white\">"
                                + getString(
                                mAccountDb.isGoogleAccount(user)
                                        ? R.string.remove_google_account_dialog_message
                                        : R.string.remove_account_dialog_message)
                                + "</body></html>");

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.remove_account_dialog_title, user))
                        .setView(promptContentView)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.remove_account_dialog_button_remove,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mAccountDb.delete(user);
                                        refreshUserList(true);
                                    }
                                }
                        )
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;*/
            case REMOVE_ID:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.remove_account_title, recordName))
                        .setMessage(getString(R.string.remove_account_info, recordHostName))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.remove,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mRecordsDb.delete(recordHostName, recordName);
                                        refreshRecordsList(true);
                                    }
                                }
                        )
                        .setNegativeButton(R.string.cancel, null)
                        .show();


                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * This method is deprecated in SDK level 8, but we have to use it because the
     * new method, which replaces this one, does not exist before SDK level 8
     */
    @Override
    protected Dialog onCreateDialog(final int id) {
        Dialog dialog = null;
        switch (id) {
            /**
             * Prompt to download ZXing from Market. If Market app is not installed,
             * such as on a development phone, open the HTTPS URI for the ZXing apk.
             */
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
    }

    private void
    unlockPC() {
        try {
            //Resources res = getResources();
            //SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            //if(sp.getBoolean("use_bluetooth_unlock", res.getBoolean(R.bool.use_bluetooth_d))){
            //    startService(new Intent(MainActivity.this, BTService.class));
            //Log.d(TAG, "Start BTService");
            //}
            //get the data from the database
            //send the data to the PC using MQTT
            //
            // send unlock package via WiFi

            //get the data from the database and try to send the data to the broker
            //or, which is better - get the selected item from the listview, fetch the selected data with the DB record and send the data to the broker
            AuthRecordsDb authRecordDb = new AuthRecordsDb(getApplicationContext());
            ArrayList<String> recordNames = new ArrayList<String>();
            authRecordDb.getNames(recordNames);

            for (int i = 0; i < recordNames.size(); i++) {
                AuthRecord ar = authRecordDb.getAuthRecord(recordNames.get(i));
                NetworkSender netSender = new NetworkSender(getApplicationContext());
                netSender.execute(ar);
            }
        } catch (Exception e) {
            Log.e(LOCAL_TAG, e.toString());
        }
    }

    /**
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

            TextView nameView = (TextView) row.findViewById(R.id.recordName);
            nameView.setText(currentRecord.qr_user);

            nameView = (TextView) row.findViewById(R.id.hostName);
            nameView.setText(currentRecord.qr_host_name);

            return row;
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
                String name = recordNames.get(i);
                mAuthRecords[i] = mRecordsDb.getAuthRecord(name);

            }

            if (newListRequired) {
                // Make the list display the data from the newly created array of records
                // This forces the list to scroll to top.
                mRecordsAdapter = new RecordsListAdapter(this, R.layout.list_row_view, mAuthRecords);
                mRecordsList.setAdapter(mRecordsAdapter);
            }

            mRecordsAdapter.notifyDataSetChanged();

            if (mRecordsList.getVisibility() != View.VISIBLE) {
                mRecordsList.setVisibility(View.VISIBLE);
                //((ScrollView) findViewById(R.id.scrollView)).setVisibility(View.GONE);
            }

            registerForContextMenu(mRecordsList);

            ((TextView) findViewById(R.id.textQRcode)).setText(R.string.click_to_unlock);

        } else {
            mAuthRecords = new AuthRecord[0]; // clear any existing user PIN state
            mRecordsList.setVisibility(View.GONE);

            /*
            // setting "How it Works" view when there is no records...

            ((ScrollView) findViewById(R.id.scrollView)).setVisibility(View.VISIBLE);

            View promptContentView =
                    getLayoutInflater().inflate(R.layout.activity_help , null, false);

            mRecordsList.vi
            ((ScrollView) findViewById(R.id.scrollView)).setVi
            */
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
    private void
    interpretScanResult(Uri scanResult, boolean confirmBeforeSave) {


        // The scan result is expected to be a URL that adds an account.
        ((TextView) findViewById(R.id.textQRcode)).setText(scanResult.toString());

        // If confirmBeforeSave is true, the user has to confirm/reject the action.
        // We need to ensure that new results are accepted only if the previous ones have been
        // confirmed/rejected by the user. This is to prevent the attacker from sending multiple results
        // in sequence to confuse/DoS the user.
        if (confirmBeforeSave) {
            if (mSaveKeyIntentConfirmationInProgress) {
                Log.w(LOCAL_TAG, "Ignoring save key Intent: previous Intent not yet confirmed by user");
                return;
            }
            // No matter what happens below, we'll show a prompt which, once dismissed, will reset the
            // flag below.
            mSaveKeyIntentConfirmationInProgress = true;
        }

        // Sanity check
        if (scanResult == null) {
            Log.e(TAG, "Scan result is null");
            showDialog(INVALID_QR_CODE);
            return;
        }

        // See if the URL is an account setup URL containing a shared secret
        if (OTP_SCHEME.equals(scanResult.getScheme()) && scanResult.getAuthority() != null) {
            parseSecret(scanResult, confirmBeforeSave);
        } else {
            Log.e(TAG, "getScheme " + scanResult.getScheme() + " getAuthority " + scanResult.getAuthority());
            showDialog(INVALID_QR_CODE);
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


            ((TextView) findViewById(R.id.textQRcode)).setText(uri.toString());

       /* if (!OTP_SCHEME.equals(scheme)) {
            Log.e(getString(R.string.app_name), LOCAL_TAG + ": Invalid or missing scheme in uri");
            showDialog(INVALID_QR_CODE);
            return;
        }*/

            ai.qr_user = uri.getQueryParameter("USER");
            ai.qr_secret_key = uri.getQueryParameter("KEY");
            ai.qr_data = uri.getQueryParameter("DATA");


            //get mqtt topic by taking some data from qr_secret_key or qr_data
            //add additional field in the AuthRecord and AuthRecordsDb


        /*if (secret_key == null || secret_key.length() == 0) {
            Log.e(getString(R.string.app_name), LOCAL_TAG +
                    ": Secret key not found in URI");
            showDialog(INVALID_QR_CODE);
            return;
        }*/


            String str;
            str = String.format("QR code:\nIP: %s (%d)\nHOST:%s\nUser: %s", ai.qr_host_ip, ai.qr_host_port, ai.qr_host_name, ai.qr_user);
            ((TextView) findViewById(R.id.textQRcode)).setText(str);


            ((Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100L);

            // save it now!
            saveRecordAndRefreshList(ai);

            // send back Authenticaion signal to confirm desktop that Setup is OK
            //sendIpLogin(ai.qr_user);
            sendMqttLoginRequest(ai.qr_user);
        /*

        if (secret.equals(mAccountDb.getSecret(user)) &&
                counter == mAccountDb.getCounter(user) &&
                type == mAccountDb.getType(user)) {
            return;  // nothing to update.
        }

        if (confirmBeforeSave) {
            mSaveKeyDialogParams = new SaveKeyDialogParams(user, secret, type, counter);
            showDialog(DIALOG_ID_SAVE_KEY);
        } else {
            saveSecretAndRefreshUserList(user, secret, null, type, counter);
        }*/
        } catch (java.lang.NumberFormatException err2) {
            ((TextView) findViewById(R.id.textQRcode)).setText(String.format(" %s", err2.toString()));
            Log.e(TAG, Log.getStackTraceString(err2));
            showDialog(INVALID_QR_CODE);
        } catch (NullPointerException error) {
            ((TextView) findViewById(R.id.textQRcode)).setText(String.format(" %s", error.toString()));
            Log.e(TAG, Log.getStackTraceString(error));
            showDialog(INVALID_QR_CODE);
        }
    }
}


