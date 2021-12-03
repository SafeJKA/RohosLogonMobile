package com.rohos.logon1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.concurrent.Semaphore;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class MQTTSender extends AsyncTask<AuthRecord, Void, Long> {

    private Context context;
    // private final String TAG = MQTTSender.class.getSimpleName();
    private MqttAndroidClient mqttClient;
    private MqttConnectOptions mqttConnOptions;
    private Semaphore s;
    private String userURI;
    private static final String serverUriPattern = "(tcp://)(?:(\\S+):(\\S+)@)?(\\S+):(\\d+)(?:@(\\S+))?";

    private static final String defaultUserName = "****";
    private static final String defaultPassword = "******";
    private static final String defaultBrokerURI = "*******";
    private static final String defaultClientID = "********";

    private Handler mHandler = null;

    public MQTTSender(Context ctx) {
        this.context = ctx;
        mHandler = new Handler(Looper.getMainLooper());

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        userURI = sp.getString("broker", "");

        mqttConnOptions = createMqttConnectOptions();
        mqttClient = createMqttAndroidClient();

        s = new Semaphore(0);


    }

    //to decide: make the connection in the constructor or in another function, like onPreExcute() or before sending of the data
    @Override
    protected Long doInBackground(AuthRecord... ai) {

        connect(mqttClient, mqttConnOptions);
        String msgToSend = ai[0].getEncryptedDataString();
        String str_data = String.format("%s.%s.%s", ai[0].qr_user, ai[0].qr_host_name, msgToSend);
        String topicToSend = ai[0].qr_host_name;
        long s = msgToSend.length();

        sendMqttMessage(str_data, topicToSend);
        disconnect();
        return s;
    }

    private void connect(final MqttAndroidClient client, MqttConnectOptions options) {

        try {
            if (!client.isConnected()) {
                IMqttToken token = client.connect(options);
                //on successful connection, publish or subscribe as usual
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        System.err.println("Connection OK");
                        //send message here
                        //try to resend it in case of error
                        if (context != null) {
                            Toast.makeText(context, "Connection successful", Toast.LENGTH_SHORT).show();
                        }
                        s.release();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        System.err.println("Connection failure");
                        if (context != null) {
                            Toast.makeText(context, "Connection failure", Toast.LENGTH_SHORT).show();
                        }
                        exception.printStackTrace();
                        s.release();
                    }
                });
                client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        if (reconnect) {
                            System.err.println("Reconnected to the server");
                            if (context != null) {
                                Toast.makeText(context, "Reconnected to the server", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        System.err.println("Connection to the server lost");
                        // cause.printStackTrace();
                    }

                    //left for future use
                    @Override
                    public void messageArrived(String topic, MqttMessage message) {

                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        if (context != null) {
                            Toast.makeText(context, "Message delivered", Toast.LENGTH_SHORT).show();
                        }
                        System.err.println("Message delivery complete");
                    }
                });
            }
        } catch (MqttException e) {
            //handle e
            e.printStackTrace();
            s.release();
        }
        try {
            s.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        try {
            IMqttToken disconnectToken = mqttClient.disconnect();
            disconnectToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //we are now successfully disconnected
                    System.err.println("Disconnection success");
                    s.release();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    //something went wrong, but probably we are disconnected anyway
                    System.err.println("Disconnection failure");
                    s.release();
                }
            });
        } catch (MqttException e) {
            System.err.println("Disconnection exception");
            e.printStackTrace();
            s.release();
        }
        try {
            s.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mqttClient.close();
        mqttConnOptions = null;
        mqttClient = null;
        context = null;
    }

    private void sendMqttMessage(String publishMessage, String publishTopic) {
        try {
            if (!mqttClient.isConnected()) {
                //make something here - display log information
                System.err.println("MQTT client is not connected");
                //Toast.makeText(this, "Unable to send message.Client disconnected", Toast.LENGTH_SHORT).show();
                return;
            }
            System.err.println("Client connected. Sending message...");
            MqttMessage message = new MqttMessage();
            message.setQos(0);
            message.setPayload(publishMessage.getBytes());

            mqttClient.publish(publishTopic, message);
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private MqttConnectOptions createMqttConnectOptions() {
        //create and return options
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        if (userURI.length() > 1) {
            final Pattern pattern = Pattern.compile(serverUriPattern);
            final Matcher matcher = pattern.matcher(userURI);
            if (matcher.find()) {
                if (matcher.group(2) != null && matcher.group(3) != null) {
                    if (!matcher.group(2).isEmpty() && !matcher.group(3).isEmpty()) {
                        connOpts.setUserName(matcher.group(2));
                        connOpts.setPassword(matcher.group(3).toCharArray());
                    }
                }
            }
        } else {
            connOpts.setUserName(defaultUserName);
            connOpts.setPassword(defaultPassword.toCharArray());
        }
        connOpts.setAutomaticReconnect(true);
        connOpts.setCleanSession(true);
        return connOpts;
    }

    private MqttAndroidClient generateDefaultClientId() {

        return new MqttAndroidClient(context, defaultBrokerURI, defaultClientID);
    }

    private MqttAndroidClient createMqttAndroidClient() {

        String clientID;
        String serverURL = "";
        //get the topic from the database - it will be generated during the "registration" phase
        if (userURI.length() > 1) {
            //get the server uri
            final Pattern pattern = Pattern.compile(serverUriPattern);
            final Matcher matcher = pattern.matcher(userURI);
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
                return new MqttAndroidClient(context, serverURL, clientID);
            } else {
                //do some alarm that you want to connect with default values
                if (context != null) {
                    toastMessage("Connecting with default values");
                }
                return generateDefaultClientId();
            }
        } else {
            if (context != null) {
                //Toast.makeText(context, "Connecting with default values", Toast.LENGTH_SHORT).show();
                toastMessage("Connecting with default values");
            }
            return generateDefaultClientId();
        }
    }

    private void toastMessage(final String text){
        Runnable r = new Runnable(){
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        };
        mHandler.post(r);
    }
}
