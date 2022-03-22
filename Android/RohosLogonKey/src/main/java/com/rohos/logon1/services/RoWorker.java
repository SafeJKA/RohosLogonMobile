package com.rohos.logon1.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rohos.logon1.AuthRecordsDb;
import com.rohos.logon1.HexEncoding;
import com.rohos.logon1.utils.AppLog;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class RoWorker extends Worker {
    private static final String mServerUriPattern = "(tcp://)(?:(\\S+):(\\S+)@)?(\\S+):(\\d+)(?:@(\\S+))?";

    private static final String mDefaultUserName = "rohos";
    private static final String mDefaultPassword = "fZ7Vq93BuWLx";
    private static final String mDefaultBrokerURI = "tcp://node02.myqtthub.com:1883";
    private static final String mDefaultClientID = "rohos.logon";

    private final String TAG = "Worker";

    private MqttAndroidClient mMqttClient;
    private MqttConnectOptions mMqttConnOptions;
    private String mUserURI;
    private Context mCtx = null;
    private Handler mHandler = null;
    private Semaphore mSemaphore;
    private String mHostToSendToken = null;
    private long mSendingSession = 0L;

    public RoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        mCtx = context;
        mHandler = new Handler(Looper.getMainLooper());

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        mUserURI = sp.getString("broker", "");

        mMqttConnOptions = createMqttConnectOptions();
        mMqttClient = createMqttAndroidClient();
        mSemaphore = new Semaphore(0);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppLog.log(TAG + ", Performing long running task in scheduled job");
        // TODO(developer): add long running task here.
        Data data = getInputData();
        String token = data.getString("token");
        if(token == null){
            AppLog.log(TAG + "; couldn't get token from data");
            return Result.failure();
        }

        ArrayList<String[]> hostList = new AuthRecordsDb(mCtx).getHostList();

        if (hostList != null) {
            //UserManager um = (UserManager)mCtx.getSystemService(Context.USER_SERVICE);
            //String uName = um.getUserName();

            TelephonyManager tm = (TelephonyManager)mCtx.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = android.provider.Settings.Secure.getString(mCtx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);


            String phoneId = "";
            if(imei != null){
                phoneId = imei.substring(0, 8).toUpperCase();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("token=");
            sb.append(token);
            sb.append(" devid=");
            //sb.append(uName.concat(" "));
            sb.append(Build.MANUFACTURER.toUpperCase().concat(" "));
            sb.append(Build.MODEL.toUpperCase().concat(" "));
            sb.append(phoneId);

            AppLog.log(sb.toString());

            int userName = 0;
            int secretKey = 1;
            int hostName = 2;
            mSendingSession = System.currentTimeMillis();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mCtx);
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong("sending_session", mSendingSession);
            editor.commit();

            connect(mMqttClient, mMqttConnOptions);

            for(String[] host : hostList){
                AppLog.log("user name:" + host[userName] + ", key:" + host[secretKey] + ", host:" + host[hostName]);
                String encToken = getEncryptedString(sb.toString(), host[secretKey]);
                if(encToken == null){
                    AppLog.log("RoWorker; Couldn't encrypt token");
                    break;
                }

                mHostToSendToken = host[hostName];
                StringBuilder mesToSend = new StringBuilder();
                mesToSend.append(host[userName].concat("."));
                mesToSend.append(mHostToSendToken);
                mesToSend.append("-PUSHUPDATE".concat("."));
                mesToSend.append(encToken);

                sendMqttMessage(mesToSend.toString(), mHostToSendToken.concat("-PUSHUPDATE"));
                try {
                    Thread.sleep(1000);
                } catch (Exception unused) {}
            }

            disconnect();
        }

        //Log.d(TAG, "Token received from data: " + token);
        return Result.success();
    }

    private void sendMqttMessage(String publishMessage, String publishTopic) {
        try {
            if (!mMqttClient.isConnected()) {
                //make something here - display log information
                AppLog.log("RoWorker; MQTT client is not connected");
                //Toast.makeText(this, "Unable to send message.Client disconnected", Toast.LENGTH_SHORT).show();
                return;
            }
            AppLog.log("RoWorker; Sending message to: " + publishTopic);

            MqttMessage message = new MqttMessage();
            message.setQos(0);
            message.setPayload(publishMessage.getBytes());

            mMqttClient.publish(publishTopic, message);
        } catch (MqttException e) {
            AppLog.log(Log.getStackTraceString(e));
        }
    }

    private MqttConnectOptions createMqttConnectOptions() {
        //create and return options
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        if (mUserURI.length() > 1) {
            final Pattern pattern = Pattern.compile(mServerUriPattern);
            final Matcher matcher = pattern.matcher(mUserURI);
            if (matcher.find()) {
                if (matcher.group(2) != null && matcher.group(3) != null) {
                    if (!matcher.group(2).isEmpty() && !matcher.group(3).isEmpty()) {
                        connOpts.setUserName(matcher.group(2));
                        connOpts.setPassword(matcher.group(3).toCharArray());
                    }
                }
            }
        } else {
            connOpts.setUserName(mDefaultUserName);
            connOpts.setPassword(mDefaultPassword.toCharArray());
        }
        connOpts.setAutomaticReconnect(true);
        connOpts.setCleanSession(true);
        return connOpts;
    }

    private MqttAndroidClient createMqttAndroidClient() {

        String clientID;
        String serverURL = "";
        //get the topic from the database - it will be generated during the "registration" phase
        if (mUserURI.length() > 1) {
            //get the server uri
            final Pattern pattern = Pattern.compile(mServerUriPattern);
            final Matcher matcher = pattern.matcher(mUserURI);
            if (matcher.find()) {
                if ((matcher.group(1) != null && !matcher.group(1).isEmpty()) &&
                        (matcher.group(4) != null && !matcher.group(4).isEmpty()) &&
                        (matcher.group(5) != null && !matcher.group(5).isEmpty())) {
                    serverURL = matcher.group(1) + matcher.group(4) + ":" + matcher.group(5);
                }
                if (matcher.group(6) != null && !matcher.group(6).isEmpty()) {
                    clientID = matcher.group(6);
                } else {
                    clientID = MqttClient.generateClientId();  //generate random client id
                }
                return new MqttAndroidClient(mCtx, serverURL, clientID);
            } else {
                //do some alarm that you want to connect with default values
                if (mCtx != null) {
                    toastMessage("Connecting with default values");
                }
                return generateDefaultClientId();
            }
        } else {
            if (mCtx != null) {
                toastMessage("Connecting with default values");
            }
            return generateDefaultClientId();
        }
    }

    private MqttAndroidClient generateDefaultClientId() {
        return new MqttAndroidClient(mCtx, mDefaultBrokerURI, mDefaultClientID);
    }

    private void connect(final MqttAndroidClient client, MqttConnectOptions options) {
        try {
            if (!client.isConnected()) {
                IMqttToken token = client.connect(options);
                //on successful connection, publish or subscribe as usual
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        AppLog.log("RoWorker; Connection OK");
                        //send message here
                        //try to resend it in case of error
                        if (mCtx != null) {
                            toastMessage("Connection successful");
                        }
                        mSemaphore.release();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        AppLog.log("RoWorker; Connection failure");
                        if (mCtx != null) {
                            toastMessage("Connection failure");
                        }
                        exception.printStackTrace();
                        mSemaphore.release();
                    }
                });

                client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        if (reconnect) {
                            AppLog.log("RoWorker; Reconnected to the server");
                            if (mCtx != null) {
                                toastMessage("Reconnected to the server");
                            }
                        }
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        AppLog.log("RoWorker; Connection to the server lost");
                        // cause.printStackTrace();
                    }

                    //left for future use
                    @Override
                    public void messageArrived(String topic, MqttMessage message) {

                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        AppLog.log("Message delivered to: " + mHostToSendToken);
                        if (mCtx != null) {
                            toastMessage("Message delivered");
                        }
                    }
                });
            }
        } catch (MqttException e) {
            mSemaphore.release();
            AppLog.log(Log.getStackTraceString(e));
        }

        try {
            mSemaphore.acquire();
        } catch (InterruptedException e) {
            AppLog.log(Log.getStackTraceString(e));
        }
    }

    private void disconnect() {
        try {
            IMqttToken disconnectToken = mMqttClient.disconnect();
            disconnectToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //we are now successfully disconnected
                    AppLog.log("RoWorker; Disconnection success");
                    mSemaphore.release();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    //something went wrong, but probably we are disconnected anyway
                    AppLog.log("RoWorker; Disconnection failure");
                    mSemaphore.release();
                }
            });
        } catch (MqttException e) {
            mSemaphore.release();
            AppLog.log(Log.getStackTraceString(e));
        }

        try {
            mSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mMqttClient.close();
        mMqttClient = null;
        mMqttConnOptions = null;
    }

    private void toastMessage(final String text){
        Runnable r = new Runnable(){
            @Override
            public void run() {
                Toast.makeText(mCtx, text, Toast.LENGTH_SHORT).show();
            }
        };
        mHandler.post(r);
    }

    public String getEncryptedString(String strToEnc, String secKey) {
        try {
            // for example 0x52B4284E - represent 2014 year.
            // "2014-1970 = 44years"
            // so 'int' should be OK to store at least (44 * 3) years of seconds
            int intSec = (int) (System.currentTimeMillis() / 1000);

            Random r = new Random();
            int randomInt = r.nextInt();
            //char loByteRand = (char)((randomInt << 28) >> 28);

            byte[] dataToEnc = strToEnc.getBytes();

            // 14 = int + int + char + char + datalen
            // '01' means - protocol version
            //
            byte[] byteData = ByteBuffer.allocate(14 + dataToEnc.length)
                    .putInt(randomInt) // Random, adding entropy to first 16 bytes of data block
                    .putInt(intSec) // OTP parameter
                    .putChar('0')  // protocol signature '01'
                    .putChar('1')
                    .putChar((char)dataToEnc.length) // data len
                    .put(dataToEnc) // data itself
                    .array();

            // create key - 16 bytes only for AES128
            String keyStr = secKey.substring(0, 32);

            if (keyStr.length() > 32) // AES128 encryption key should be 16 bytes only.
                keyStr = secKey.substring(0, 32);

            SecretKeySpec secretKey = new SecretKeySpec(HexEncoding.decode(keyStr), "AES");

            // AES128 encryption
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedByteData = cipher.doFinal(byteData);

            return HexEncoding.encode(encryptedByteData);
        } catch (Exception e) {
            AppLog.log(Log.getStackTraceString(e));
        }
        return null;
    }
}
