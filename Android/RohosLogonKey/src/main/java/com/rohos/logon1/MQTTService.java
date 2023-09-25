package com.rohos.logon1;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;

import com.rohos.logon1.utils.AppLog;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;

public class MQTTService extends Service {

    IBinder mBinder = new LocalBinder();
   // private static final String TAG = MQTTService.class.getSimpleName();
    private MqttAndroidClient mqttClient;
    private MqttConnectOptions mqttConnOptions;
    private static int stateService = MqttConstants.STATE_SERVICE.NOT_CONNECTED;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public MQTTService getMqttServiceInstance() {
            return MQTTService.this;
        }
    }

    public void sendMqttMessage(String publishMessage, String publishTopic) {

        try {
            if (!mqttClient.isConnected()) {
               // Log.d(TAG, "MQTT client is not connected");
                Toast.makeText(this, "Unable to send message.Client disconnected", Toast.LENGTH_SHORT).show();
                return;
            }
            MqttMessage message = new MqttMessage();
            message.setQos(2);
            message.setPayload(publishMessage.getBytes());
            mqttClient.publish(publishTopic, message);//play with this function overloads

        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        mqttClient = createMqttAndroidClient();
        mqttConnOptions = createMqttConnectOptions();
        //connect to the broker here
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        //disconnect here
        System.err.println("Received onDestroy() event in MQTTService");
        try {
            IMqttToken disconnectToken = mqttClient.disconnect();
            disconnectToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //we are now successfully disconnected
                    System.err.println("Disconnection success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    //something went wrong, but probably we are disconnected anyway
                    System.err.println("Disconnection failure");
                }
            });
        } catch (MqttException e) {
            System.err.println("Disconnection exception");
            e.printStackTrace();
        }
        mqttClient.close();
        mqttConnOptions = null;
        mqttClient = null;
        stateService = MqttConstants.STATE_SERVICE.NOT_CONNECTED;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        } else {
            // if user starts the service
            switch (intent.getAction()) {
                case MqttConstants.ACTION.START_ACTION: {
                    //  Log.d(TAG, "Received user starts foreground intent");
                    try{
                        startForeground(MqttConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
                    }catch(java.lang.Throwable e){
                        AppLog.log(Log.getStackTraceString(e));
                    }

                    this.connect(mqttClient, mqttConnOptions);
                    break;
                }
                case MqttConstants.ACTION.STOP_ACTION: {
                    stopForeground(true);
                    stopSelf();
                    break;
                }
                default: {
                    stopForeground(true);
                    stopSelf();
                }
            }
        }
        return START_NOT_STICKY;
    }

    private Notification prepareNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = null;
        if(Build.VERSION.SDK_INT >= 31){
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE);
        }else{
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        }

        return new NotificationCompat.Builder(this, "65854225")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("MQTT Service")
                .setContentText("Performing required operations...")
                .setContentIntent(pendingIntent)
                .build();
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
        return new MqttAndroidClient(getApplicationContext(), "tcp://broker.hivemq.com:1883", clientID);
    }

    public void connect(final MqttAndroidClient client, MqttConnectOptions options) {

        try {
            if (!client.isConnected()) {
                IMqttToken token = client.connect(options);
                //on successful connection, publish or subscribe as usual
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        System.err.println("Connection OK");
                        stateService = MqttConstants.STATE_SERVICE.CONNECTED;
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        System.err.println("Connection failure");
                        exception.printStackTrace();
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
                        cause.printStackTrace();
                    }

                    //left for future use
                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {

                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
            }
        } catch (MqttException e) {
            //handle e
        }
    }
}
