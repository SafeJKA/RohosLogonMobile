package com.rohos.logon1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/* Main Activity
 * @author AlexShilon alex@rohos.com
 */
public class MainActivity extends ActionBarActivity {

    /**
     * Intent action to that tells this Activity to initiate the scanning of barcode to add an
     * account.
     */
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

    // Links
    public static final String ZXING_MARKET =
            "market://search?q=pname:com.google.zxing.client.android";
    public static final String ZXING_DIRECT =
            "https://zxing.googlecode.com/files/BarcodeScanner3.1.apk";

    private boolean mSaveKeyIntentConfirmationInProgress;


    private NetworkSender netSender;
    private AuthRecordsDb mRecordsDb;
    private ListView mRecordsList;
    private AuthRecord[] mAuthRecords = {};
    private RecordsListAdapter mRecordsAdapter;


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

        findViewById(R.id.aboutText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUpdates();
            }
        });

        /* todo - display version from XML
        String versionName = this.getPackageManager()
                .getPackageInfo(this.getPackageName(), 0).versionName;*/

        mRecordsDb = new AuthRecordsDb(getApplicationContext());
        mRecordsAdapter = new RecordsListAdapter(this, R.layout.list_row_view, mAuthRecords);
        mRecordsList = (ListView) findViewById(R.id.listView);
        mRecordsList.setAdapter(mRecordsAdapter);

        mRecordsList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> unusedParent, View row,
                                    int unusedPosition, long unusedId) {

                String accName = ((TextView)row.findViewById(R.id.recordName)).getText().toString();
                sendIpLogin(accName);

            }
        });

        refreshRecordsList(false);



        /*if ( ai = null || ai.qr_secret_key == null)
        {
            ((TextView) findViewById(R.id.textQRcode)).setText( "No records" );
        }*/


        if (savedInstanceState == null) {

            //  for testing purposes
            // interpretScanResult( Uri.parse("rohos1://192.168.1.8:1005/ZED?USER=Alex&KEY=XXXXXX&DATA=YYYYYY"), false);
            // interpretScanResult( Uri.parse("rohos1://192.168.1.8:1005/zed?USER=Alex(KEY=538a44883958c2961c3c10e419c931ab(DATA=6c345545645664a802938219371bb832e3cc503373aa0b6f6ed1b07f41e52005728e092d468dff6841f9d2c34e5f6ad6cc8aee67792f693c828a018e16bfa41ad08bb7132298afdfa9e70df29b023661"), false);
            // interpretScanResult( Uri.parse("rohos1://192.168.1.1:1005/zed123?USER=AlexAnder(KEY=538a44883958c2961c3c10e419c931ab(DATA=6c5345645645745672938219371bb832e3cc503373aa0b6f6ed1b07f41e52005728e092d468dff6841f9d2c34e5f6ad6cc8aee67792f693c828a018e16bfa41ad08bb7132298afdfa9e70df29b023661"), false);

        } else
        {

        }

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
                checkUpdates();
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    /*
    send Authentication data block to the network
     */
    private void sendIpLogin(String accountName) {

        AuthRecord ar = mRecordsDb.getAuthRecord(accountName);

        if (ar.qr_user == null || ar.qr_user.length()==0 )
        {
            ((TextView) findViewById(R.id.textQRcode)).setText(String.format("Please install Rohos Logon Key on the desktop and scan QR-code first."));
            return;
        }

        if (netSender != null)
        {
            netSender.cancel(true);
            netSender = null;
        }

        netSender = new NetworkSender(this.getApplicationContext());
        netSender.execute(ar);

    }

    private void scanBarcode() {

        Intent intentScan = new Intent("com.google.zxing.client.android.SCAN");
        intentScan.putExtra("SCAN_MODE", "QR_CODE_MODE");
        intentScan.putExtra("SAVE_HISTORY", false);
        try {
            startActivityForResult(intentScan, SCAN_REQUEST);
        } catch (ActivityNotFoundException error) {
            showDialog(0);
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
        Log.i(getString(R.string.app_name), LOCAL_TAG + ": onActivityResult");
        if (requestCode == SCAN_REQUEST && resultCode == Activity.RESULT_OK) {
            // Grab the scan results and convert it into a URI
            String scanResult = (intent != null) ? intent.getStringExtra("SCAN_RESULT") : null;
            Uri uri = (scanResult != null) ? Uri.parse(scanResult) : null;
            interpretScanResult(uri, false);
        }
    }


    /**
     * This method is deprecated in SDK level 8, but we have to use it because the
     * new method, which replaces this one, does not exist before SDK level 8
     */
    @Override
    protected Dialog onCreateDialog(final int id) {
        Dialog dialog = null;
        switch(id) {
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
                                }
                                catch (ActivityNotFoundException e) { // if no Market app
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
        public View getView(int position, View convertView, ViewGroup parent){
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
                registerForContextMenu(mRecordsList);
            }
        } else {
            mAuthRecords = new AuthRecord[0]; // clear any existing user PIN state
            mRecordsList.setVisibility(View.GONE);
        }


    }

    private void saveRecordAndRefreshList(AuthRecord ar)
    {
        mRecordsDb.update(ar);
        refreshRecordsList(true);
    }


    /**
     * Interprets the QR code that was scanned by the user.  Decides whether to
     * launch the key provisioning sequence or the OTP seed setting sequence.
     *
     * @param scanResult a URI holding the contents of the QR scan result
     * @param confirmBeforeSave a boolean to indicate if the user should be
     *                          prompted for confirmation before updating the otp
     *                          account information.
     */
    private void interpretScanResult(Uri scanResult, boolean confirmBeforeSave) {


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
            showDialog(INVALID_QR_CODE);
            return;
        }

        // See if the URL is an account setup URL containing a shared secret
        if (OTP_SCHEME.equals(scanResult.getScheme()) && scanResult.getAuthority() != null) {
            parseSecret(scanResult, confirmBeforeSave);
        } else {
            showDialog(INVALID_QR_CODE);
        }
    }


    /**
     * Parses a secret value from a URI. The format will be:
     *
     *   QR code format:
     *   rohos1://192.168.1.15:995/ZED?USER=Alex&KEY=XXXXXX&DATA=YYYYYY     *
     *
     *
     *    where:
     *     192.168.1.15:995 - host IP and Port
     *     ZED - host name (PC name)
     *     Alex = user name whos login is configured | OR | secret Rohos disk name
     *     KEY - encryption key (HEX)
     *     DATA - authentication data (HEX)
     *
     * @param uri The URI containing the secret key
     * @param confirmBeforeSave a boolean to indicate if the user should be
     *                          prompted for confirmation before updating the otp
     *                          account information.
     */
    private void parseSecret(Uri uri, boolean confirmBeforeSave) {

        try{
            /* final String scheme = uri.getScheme().toLowerCase(); */

        String url = uri.toString();
        url = url.replace('(', '&');
        uri = Uri.parse(url);

        AuthRecord ai = new AuthRecord();
        ai.url  = url;
        ai.qr_host_name = uri.getPath().substring(1);
        ai.qr_host_ip = uri.getAuthority();
        ai.qr_host_port = 1205;


        int i = ai.qr_host_ip.indexOf(":");
            if (i>0)
            {
                ai.qr_host_port = Integer.parseInt(ai.qr_host_ip.substring(i+1));
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

        /*if (secret_key == null || secret_key.length() == 0) {
            Log.e(getString(R.string.app_name), LOCAL_TAG +
                    ": Secret key not found in URI");
            showDialog(INVALID_QR_CODE);
            return;
        }*/


            String str;
            str = String.format("QR code:\nIP: %s (%d)\nHOST:%s\nUser: %s", ai.qr_host_ip, ai.qr_host_port, ai.qr_host_name, ai.qr_user );
            ((TextView) findViewById(R.id.textQRcode)).setText(str);


         ((Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100L);

            // save it now!
            saveRecordAndRefreshList(ai);

            // send back Authenticaion signal to confirm desktop that Setup is OK
            sendIpLogin(ai.qr_user);
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
        }

        catch (java.lang.NumberFormatException err2  )
        {
            ((TextView) findViewById(R.id.textQRcode)).setText( String.format(" %s",err2.toString() ));
            showDialog(INVALID_QR_CODE);
        }

        catch ( NullPointerException error )
        {
            ((TextView) findViewById(R.id.textQRcode)).setText( String.format(" %s",error.toString() ));
            showDialog(INVALID_QR_CODE);
        }


    }




    /*
    *
    *   Great Broadcast sender module. Send Authentication signal and receive an answer from desktop.
        *   @author AlexShilon
         *
     */
    private class NetworkSender extends AsyncTask<AuthRecord, Void, Long> {
        public Socket socket;
        private Context context;
        public String strResult;
        public String strHostIp;


        public NetworkSender(Context context) {
            this.context = context;

            socket = null;
        }

        InetAddress getBroadcastAddress() throws IOException {
            WifiManager wifi = (WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo();
            // handle null somehow

            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++)
                quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
            return InetAddress.getByAddress(quads);
        }

        /*InetAddress getLocalAddress() throws IOException {
            WifiManager wifi = (WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo();
            // handle null somehow

            return InetAddress.getByAddress(dhcp.ipAddress);
        }*/

        @Override
        protected Long doInBackground(AuthRecord... ai) {

            long result=0;
            DatagramSocket udp_socket = null;

            if (ai[0].qr_user == null || ai[0].qr_user.isEmpty())
            {
                return result;
            }


            try {

                //InetSocketAddress bindSocketAddress = new InetSocketAddress("localhost", service.getNetworkConfiguration().getBindSocketAddress().getPort());

                udp_socket = new DatagramSocket(ai[0].qr_host_port);
                udp_socket.setBroadcast(true);
                udp_socket.setReuseAddress(true);

                String encryptedAuthString = ai[0].getEncryptedDataString();
                String str_data = String.format("%s.%s.%s", ai[0].qr_user, ai[0].qr_host_name, encryptedAuthString );

                DatagramPacket packet = new DatagramPacket(str_data.getBytes(), str_data.length(),
                        getBroadcastAddress(), ai[0].qr_host_port);
                udp_socket.send(packet);


                udp_socket.setSoTimeout(1000);
                DatagramPacket recv_packet = new DatagramPacket(new byte[300], 300);
                String serverReply = "";

                try {
                    udp_socket.receive(recv_packet);

                    // if we have received ourself packet...
                    // receive once again server reply...
                    if (recv_packet.getData().length > 30)
                    {
                        recv_packet.setData(new byte[100], 0, 100);
                        //Thread.sleep( 300 );//1 sec
                        udp_socket.receive(recv_packet);
                    }

                    serverReply = new String(recv_packet.getData());

                }

                catch (SocketTimeoutException err)
                {
                    // ... oops no server reply
                }

                udp_socket.close();

                strResult = String.format("Authentication signal sent OK. %s %d\nUnlocked:%s",
                        str_data.substring(0, 20), encryptedAuthString.length(),
                        /*ai[0].plainHexAuthStr,*/
                        serverReply);

                return result;

                /*
                this is OLD version - Peer-to-Peer connection.
                doesnt work when HOST name cannot be resolved...
                socket = new Socket();

                InetAddress address = InetAddress.getByName(ai[0].qr_host_name );
                strHostIp = address.getHostAddress();

                if (strHostIp.length() == 0)
                    strHostIp = ai[0].qr_host_ip;

                SocketAddress remoteaddr=new InetSocketAddress(strHostIp, ai[0].qr_host_port);
                socket.connect(remoteaddr, 900);

                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()));

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                char [] received_data  = new char[500];
                int len = in.read(received_data);

                String hostHello = new StringBuffer()
                        .append(received_data, 0, len)
                        .toString();

                String encryptedAuthString = ai[0].getEncryptedDataString();
                String str_data = String.format("%s.%s.%s", ai[0].qr_user, ai[0].qr_host_name, encryptedAuthString );
                out.write(str_data, 0, str_data.length());
                out.flush();

                result = str_data.length();
                strResult = String.format("Send OK. %s %d\n%s \nto: %s (%s)",
                        str_data.substring(0, 20), encryptedAuthString.length(),
                        ai[0].plainHexAuthStr,
                        strHostIp,hostHello );

                //strResult = "";
                */

            } catch (IOException e) {

                strResult = String.format("IO Exception.. %s", e.toString());

            }

              catch ( Exception e)
               {
                   strResult = String.format("Exception.. %s", e.toString());
                   //if (udp_socket!=null) udp_socket.close();
               }

            finally {
                if (udp_socket!=null) udp_socket.close();

            }


         return result;
        }

        protected void onPostExecute(Long result) {

            ((TextView) findViewById(R.id.textQRcode)).setText( strResult);

            if ( strResult.indexOf("Rohos:", 0) >0)
            {
                // vibarate if result contains server reply

                ((Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100L);
            }


        }

    }
}


