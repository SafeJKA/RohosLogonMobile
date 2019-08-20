package com.rohos.logon1;

import android.content.Context;
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

import java.util.concurrent.Semaphore;

public class MQTTSender extends AsyncTask<AuthRecord, Void, Long> {

    private Context context;
    private final String TAG = MQTTSender.class.getSimpleName();
    private MqttAndroidClient mqttClient;
    private MqttConnectOptions mqttConnOptions;
    private Semaphore s;

    public MQTTSender(Context ctx) {
        this.context = ctx;
        //create the connection and connection options here
        mqttConnOptions = createMqttConnectOptions();
        mqttClient = createMqttAndroidClient();

        s = new Semaphore(0);
    }

    //to decide: make the connection in the constructor or in another function, like onPreExcute() or before sending of the data
    @Override
    protected Long doInBackground(AuthRecord... ai) {

        //try to connect here
        connect(mqttClient, mqttConnOptions);

        //get the encrypted message and topic here
        //send the message and return the number of sent bytes
        String msgToSend = ai[0].getEncryptedDataString();
        String str_data = String.format("%s.%s.%s", ai[0].qr_user, ai[0].qr_host_name, msgToSend);
        String topicToSend = ai[0].qr_host_name;
        long s = msgToSend.length();

        sendMqttMessage(str_data, topicToSend);
        //disconnect gracefully here
        //should be make some pause here, about 1 sec to ensure that we delivered the data
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
                        s.release();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        System.err.println("Connection failure");
                        exception.printStackTrace();
                        s.release();
                    }
                });
                client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        if (reconnect) {
                            System.err.println("Reconnected to the server");
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
            message.setQos(2);
            message.setPayload(publishMessage.getBytes());

            mqttClient.publish(publishTopic, message).waitForCompletion();

        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private MqttConnectOptions createMqttConnectOptions() {
        //create and return options
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        connOpts.setAutomaticReconnect(true);
        connOpts.setCleanSession(true);
        return connOpts;
    }

    private MqttAndroidClient createMqttAndroidClient() {
        //create and return client
        String clientID = MqttClient.generateClientId();  //generate random client id
        //get the topic from the database - it will be generated during the "registration" phase
        return new MqttAndroidClient(context, "tcp://broker.hivemq.com:1883", clientID);
    }
}
