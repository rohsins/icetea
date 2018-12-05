package com.rohsins.icetea

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

private val mqttURI = "tcp://hardware.wscada.net:1883";
private val mqttClientId = "rohsisnKotlin";
private val mqttUserName = "rtshardware";
private val mqttPassword = "rtshardware";
private val udi = "TestSequence1801";
private val subscribeTopic = "RTSR&D/baanvak/sub/" + udi;
private val publishTopic = "RTSR&D/baanvak/pub/" + udi;
private var mqttConfigured = false;

class Connectivity : BroadcastReceiver() {
    private val mqtt = false;
    private var NetworkStatus = 0;
    private var NetworkPrevStatus = 3;
    private val connectOption = MqttConnectOptions();

    companion object {
        private lateinit var mqttClient : MqttClient;

        fun MqttPublish(mqttMessage: MqttMessage) {
            Thread(PublishRunnable(mqttMessage)).start();
        }

        fun MqttPublish(mqttMessage: ByteArray) {
            Thread(PublishRunnable(mqttMessage)).start();
        }

        fun MqttSubscribe(topic: String) {
            try {
                if (mqttConfigured && mqttClient.isConnected) {
                    mqttClient.subscribe(topic);
                } else {
                    Log.d("VTAG", "no network connection");
                }
            } catch (e: MqttException) {
                e.printStackTrace();
            }
        }
    }

    fun MqttConnect() {
        Thread(ServiceRunnable(true)).start() //connect mqtt
    }

    fun MqttDisconnect() {
        Thread(ServiceRunnable(false)).start() //disconnect mqtt
    }

    fun ConfigureAndConnectMqtt() {
        if (!mqttConfigured) {
            mqttConfigured = true;

            val brokerAddress = mqttURI;
            val clientId = mqttClientId;
            val persistence = MemoryPersistence();

            connectOption.userName = mqttUserName;
            connectOption.password = mqttPassword.toCharArray();
            connectOption.isCleanSession = false;
            connectOption.isAutomaticReconnect = true;
            connectOption.keepAliveInterval = 10;
            connectOption.connectionTimeout = 10;

            mqttClient = MqttClient(brokerAddress, clientId, persistence);
            mqttClient.setCallback(object: MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d("VTAG", "connection complete");
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("VTAG", message.toString());
                    MqttPublish("what the hell is this".toByteArray());
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.d("VTAG", "connection has been lost. WTF!!!");
                    //mqttClient.disconnectForcibly(100, 1000, false);
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {

                }
            });
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val conn = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = conn.activeNetworkInfo

        if (networkInfo?.type == ConnectivityManager.TYPE_WIFI) {
            NetworkStatus = 1;
            Log.d("VTAG", "Wifi Connected");
        } else if (networkInfo?.type == ConnectivityManager.TYPE_MOBILE) {
            NetworkStatus = 2;
            Log.d("VTAG", "Cellular Connected");
        } else {
            NetworkStatus = 3;
            Log.d("VTAG", "No Network Connection");
        }
        if (NetworkStatus != NetworkPrevStatus) {
            NetworkPrevStatus = NetworkStatus;
            if ((NetworkStatus == 1 || NetworkStatus == 2) && !mqttClient.isConnected) {
                Log.d("VTAG", "initializing connection sequence");
                MqttConnect();
            } else if (NetworkStatus == 3) {
                Log.d("VTAG", "initializing disconnect sequence");
                MqttDisconnect();
            }
        }
    }

    private inner class ServiceRunnable(var flag: Boolean): Runnable {
        override fun run() {
            try {
                if (flag && !mqttClient.isConnected) {
                    mqttClient.connect(connectOption);
                } else if (!flag && mqttClient.isConnected) {
                    mqttClient.disconnect();
                    Log.d("VTAG", "disconnected");
                } else if (!flag && !mqttClient.isConnected) {
                    mqttClient.disconnectForcibly(1, 1, false);
                    Log.d("VTAG", "forcefully disconnecting");
                }
            } catch (e: MqttException) {
                Log.d("VTAG", "okay cool whatever the error is $e");
                e.printStackTrace();
                if (NetworkStatus == 1 || NetworkStatus == 2) {
                    Log.d("VTAG", "Running again");
                    mqttClient.disconnectForcibly(1, 1, false);
                    this.run();
                }
            }
        }
    }

    private class PublishRunnable: Runnable {
        var mqttPayload = MqttMessage();

        constructor(mqttMessage: MqttMessage) {
            mqttPayload = mqttMessage;
        }

        constructor(mqttMessage: ByteArray)  {
            mqttPayload.payload = mqttMessage;
            mqttPayload.qos = 2;
            mqttPayload.isRetained = false;
        }

        override fun run() {
            try {
                if (mqttConfigured && mqttClient.isConnected) {
                    mqttClient.publish(publishTopic, mqttPayload);
                } else {
                    Log.d("VTAG", "Publish: no network connection");
                }
            } catch (e: MqttException) {
                e.printStackTrace();
            }
        }
    }
}
