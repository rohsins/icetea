package com.rohsins.icetea

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.PowerManager
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.lang.Exception
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket

private val mqttURI = "tcp://hardware.wscada.net:1883";
private val mqttClientId = "rohsinsKotlinJ3";
private val mqttUserName = "rtshardware";
private val mqttPassword = "rtshardware";
private val udi = "TestSequence1821";
private val subscribeTopic = "RTSR&D/baanvak/sub/" + udi;
private val publishTopic = "RTSR&D/baanvak/pub/" + udi;
private var mqttConfigured = false;

class Connectivity : BroadcastReceiver() {
    private var mqtt = false;
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
                    mqttClient.subscribe(topic, 2);
                } else {
                    Log.d("VTAG", "no network connection");
                }
            } catch (e: MqttException) {
                e.printStackTrace();
            }
        }

        fun MqttUnsubscribe(topic: String) {
            try {
                if (mqttConfigured && mqttClient.isConnected) {
                    mqttClient.unsubscribe(topic);
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
            connectOption.isCleanSession = false; // false important
            connectOption.isAutomaticReconnect = false; // false important
            connectOption.keepAliveInterval = 10;
            connectOption.connectionTimeout = 5;
            connectOption.maxInflight = 100;

            mqttClient = MqttClient(brokerAddress, clientId, persistence);
            mqttClient.setCallback(object: MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d("VTAG", "connection complete $reconnect");
                    mqtt = true;
                    if (!reconnect) {
                        MqttUnsubscribe(subscribeTopic);
                        MqttSubscribe(subscribeTopic);
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("VTAG", message.toString());
                    MqttPublish("what the hell is this".toByteArray());
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.d("VTAG", "connection has been lost. WTF!!!");
                    MqttConnect();
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {

                }
            });
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val conn = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = conn.activeNetworkInfo
        val wakeLock: PowerManager.WakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Connectivity::WakeLock");
        };

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
            if ((NetworkStatus == 1 || NetworkStatus == 2) && !mqttClient.isConnected) {
                Log.d("VTAG", "initializing connection sequence");
                MqttConnect();
                if (!wakeLock.isHeld) {
                    wakeLock.acquire(0);
                }
            } else if (NetworkStatus == 3) {
                Log.d("VTAG", "initializing disconnect sequence");
                MqttDisconnect();
                if (wakeLock.isHeld) {
                    wakeLock.release();
                }
            }
            NetworkPrevStatus = NetworkStatus;
        }
    }

    private inner class ServiceRunnable(var flag: Boolean): Runnable {
        override fun run() {
            try {
                if (flag && !mqttClient.isConnected && (NetworkStatus == 1 || NetworkStatus == 2)) {
                    Log.d("VTAG", "connecting thread: $NetworkPrevStatus, $NetworkStatus");
                    var socket = Socket("hardware.wscada.net", 1883);
                    socket.close();
                    if ((NetworkPrevStatus == 1 || NetworkPrevStatus == 2) && mqtt == true) {
                        mqttClient.disconnectForcibly(1, 1, false);
                        mqttClient.close(true);
                        mqttConfigured = false;
                    }
                    ConfigureAndConnectMqtt();
                    mqttClient.connect(connectOption);
                } else if (!flag && mqttClient.isConnected) {
                    mqttClient.unsubscribe(subscribeTopic);
                    mqttClient.disconnect();
                    mqttClient.close(true);
                    mqttConfigured = false;
                    Log.d("VTAG", "disconnected thread");
                } else if (!flag && !mqttClient.isConnected) {
                    mqttClient.disconnectForcibly(1, 1, false);
                    mqttClient.close(true);
                    mqttConfigured = false;
                    Log.d("VTAG", "forcefully disconnecting thread");
                }
            } catch (e: MqttException) {
                Log.d("VTAG", "okay cool thread whatever the error is $e");
                e.printStackTrace();
                try {
                    if (e.reasonCode == 0) {
                        if (NetworkStatus == 1 || NetworkStatus == 2) {
                            Log.d("VTAG", "Running again thread");
                            Log.d("VTAG", "address: ${InetAddress.getByName("hardware.wscada.net").hostAddress}");
//                            if (InetAddress.getByName("hardware.wscada.net").hostAddress.equals("10.0.0.1")) {
//                                Log.d("VTAG", "No fucking Internet connection, Killing Service");
//                            } else {
                                Thread(ServiceRunnable(true)).start();
//                            }
                            Log.d("VTAG", "exiting this thing: ${e.reasonCode}");
                        }
                    } else if (e.reasonCode == 32110) {
                        // Connect already in progress
                        Thread(ServiceRunnable(true)).start();
                        Log.d("VTAG", "handling connection in progress");
                    } else {
                        Thread(ServiceRunnable(true)).start(); // 32103 // java.net.ConnectException
                        Log.d("VTAG", "error cause is : ${e.reasonCode}");

                    }
                } catch (e: Exception) {
                    e.printStackTrace();
                }
            } catch (e: ConnectException) {
                Log.d("VTAG", "No Internet Connection");
                e.printStackTrace();
                Thread(ServiceRunnable(true)).start();
            } catch (e: Exception) {
                Log.d("VTAG", "Unknown Error");
                e.printStackTrace();
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
