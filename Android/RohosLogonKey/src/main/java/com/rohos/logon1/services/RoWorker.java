package com.rohos.logon1.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rohos.logon1.AuthRecord;
import com.rohos.logon1.AuthRecordsDb;
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

import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoWorker extends Worker {
    private static final String mServerUriPattern = "(tcp://)(?:(\\S+):(\\S+)@)?(\\S+):(\\d+)(?:@(\\S+))?";
    private static final String mDefaultUserName = "*****";
    private static final String mDefaultPassword = "******";
    private static final String mDefaultBrokerURI = "********";
    private static final String mDefaultClientID = "********";

    private final String TAG = "Worker";

    private MqttAndroidClient mMqttClient;
    private MqttConnectOptions mMqttConnOptions;
    private String mUserURI;
    private Context mCtx = null;
    private Handler mHandler = null;
    private Semaphore mSemaphore;

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

        String accountName = data.getString("acc_name");
        String hostName = data.getString("host_name");

        AuthRecordsDb recordsDb = new AuthRecordsDb(mCtx);
        AuthRecord ar = recordsDb.getAuthRecord(accountName, hostName);

        connect(mMqttClient, mMqttConnOptions);
        String msgToSend = ar.getEncryptedDataString();
        String str_data = String.format("%s.%s.%s", ar.qr_user, ar.qr_host_name, msgToSend);
        String topicToSend = ar.qr_host_name;

        sendMqttMessage(str_data, topicToSend);
        disconnect();

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
            AppLog.log("RoWorker; Client connected. Sending message...");
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
                        if (mCtx != null) {
                            toastMessage("Message delivered");
                        }
                        AppLog.log("RoWorker; Message delivery complete");
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
}
