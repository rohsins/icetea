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
    private val connectOption = MqttConnectOptions();

    companion object {
        private lateinit var mqttClient : MqttClient;

        fun MqttPublish(mqttMessage: MqttMessage) {
            if (mqttConfigured && mqttClient.isConnected) {
                mqttClient.publish(publishTopic, mqttMessage);
            }
        }

        fun MqttPublish(mqttmessage: ByteArray) {
            if (mqttConfigured && mqttClient.isConnected) {
                mqttClient.publish(publishTopic, mqttmessage, 2, false);
            }
        }

        fun MqttSubscribe(topic: String) {
            if (mqttConfigured && mqttClient.isConnected) {
                mqttClient.subscribe(topic);
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
            connectOption.keepAliveInterval = 2000;
            connectOption.connectionTimeout = 1000;

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
            MqttConnect();
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val conn = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = conn.activeNetworkInfo

        if (networkInfo?.type == ConnectivityManager.TYPE_WIFI) {
            Log.d("VTAG", "Wifi Connected");
        } else if (networkInfo?.type == ConnectivityManager.TYPE_MOBILE) {
            Log.d("VTAG", "Cellular Connected");
        } else {
            Log.d("VTAG", "No Network Connection");
        }
    }

    inner class ServiceRunnable(var flag: Boolean): Runnable {
        override fun run() {
            try {
                if (flag) {
                    mqttClient.connect(connectOption);
                } else {
                    mqttClient.disconnect();
                    Log.d("VTAG", "disconnected");
                }
            } catch (e: MqttException) {
                e.printStackTrace();
            }
        }
    }
}
