package com.rohsins.icetea

import android.content.Context
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

class Connectivity {
    private val mqtt = false;

    companion object {
        lateinit private var mqttClient : MqttClient;

        fun MqttPublish(mqttMessage: MqttMessage) {
            if (mqttConfigured) {
                mqttClient.publish(publishTopic, mqttMessage);
            }
        }

        fun MqttPublish(mqttmessage: ByteArray) {
            if (mqttConfigured) {
                mqttClient.publish(publishTopic, mqttmessage, 2, false);
            }
        }

        fun MqttSubscribe(topic: String) {
            if (mqttConfigured) {
                mqttClient.subscribe(topic);
            }
        }
    }

    fun ConfigureAndConnectMqtt() : Int {
        if (!mqttConfigured) {
            mqttConfigured = true;

            val brokerAddress = mqttURI;
            val clientId = mqttClientId;
            val persistence = MemoryPersistence();
            val connectOption = MqttConnectOptions();

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
                    mqttClient.disconnectForcibly(100, 1000, false);
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {

                }

            });

            try {
                mqttClient.connect(connectOption);
            } catch (e: MqttException) {
                e.printStackTrace();
                return -1;
            }
        }

        return 0;
    }

//    fun startMonitoring() {
//        val connectivityMonitor= context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager;
//        val activeNetwork: NetworkInfo? = connectivityMonitor.activeNetworkInfo;
//        val isConnected: Boolean = activeNetwork?.isConnected == true
//    }

}

