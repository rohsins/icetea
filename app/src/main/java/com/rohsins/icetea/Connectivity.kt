package com.rohsins.icetea

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.lang.Exception
import java.net.ConnectException
import java.net.Socket

private const val mqttURI = "tcp://hardware.wscada.net:1883"
private const val mqttClientId = "rohsinsKotlinJ3"
private const val mqttUserName = "rtshardware"
private const val mqttPassword = "rtshardware"
private const val udi = "TestSequence1821"
private const val subscribeTopic = "RTSR&D/baanvak/sub/$udi"
private const val publishTopic = "RTSR&D/baanvak/pub/$udi"
private var mqttConfigured = false

class Connectivity : BroadcastReceiver() {
    private var mqtt = false
    private var networkStatus: Int = 0
    private var networkPrevStatus = 3
    private val connectOption = MqttConnectOptions()

    companion object {
        private lateinit var mqttClient : MqttClient

        fun mqttPublish(mqttMessage: MqttMessage) {
            Thread(PublishRunnable(mqttMessage)).start()
        }

        fun mqttPublish(mqttMessage: ByteArray) {
            Thread(PublishRunnable(mqttMessage)).start()
        }

        fun mqttSubscribe(topic: String) {
            try {
                if (mqttConfigured && mqttClient.isConnected) {
                    mqttClient.subscribe(topic, 2)
                } else {
                    Log.d("VTAG", "no network connection")
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }

        fun mqttUnsubscribe(topic: String) {
            try {
                if (mqttConfigured && mqttClient.isConnected) {
                    mqttClient.unsubscribe(topic)
                } else {
                    Log.d("VTAG", "no network connection")
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    private fun mqttConnect() {
        Thread(ServiceRunnable(true)).start() //connect mqtt
    }

    private fun mqttDisconnect() {
        Thread(ServiceRunnable(false)).start() //disconnect mqtt
    }

    fun mqttClose() {
        mqttDisconnect()
        mqttClient.close(true);
    }

    fun configureAndConnectMqtt() {
        if (!mqttConfigured) {
            mqttConfigured = true

            val brokerAddress = mqttURI
            val clientId = mqttClientId
            val persistence = MemoryPersistence()

            connectOption.userName = mqttUserName
            connectOption.password = mqttPassword.toCharArray()
            connectOption.isCleanSession = false // false important
            connectOption.isAutomaticReconnect = false // false important
            connectOption.keepAliveInterval = 10
            connectOption.connectionTimeout = 5
            connectOption.maxInflight = 100

            mqttClient = MqttClient(brokerAddress, clientId, persistence)
            mqttClient.setCallback(object: MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d("VTAG", "connection complete $reconnect")
                    if (!mqtt) {
//                        mqttUnsubscribe(subscribeTopic);
                        mqttSubscribe(subscribeTopic)
                        mqtt = true
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("VTAG", message.toString())
                    mqttPublish("what the hell is this".toByteArray())
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.d("VTAG", "connection has been lost. WTF!!!")
                    if (!mqttClient.isConnected) {
                        Handler().postDelayed({mqttConnect()}, 1000)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {

                }
            })
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val conn = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo = conn.activeNetworkInfo
        val wakeLock: PowerManager.WakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Connectivity::WakeLock")
        }

        if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
            networkStatus = 1
            Log.d("VTAG", "Wifi Connected")
        } else if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
            networkStatus = 2
            Log.d("VTAG", "Cellular Connected")
        } else {
            networkStatus = 3
            Log.d("VTAG", "No Network Connection")
        }
        if (networkStatus != networkPrevStatus) {
            if ((networkStatus == 1 || networkStatus == 2) && !mqttClient.isConnected) {
                Log.d("VTAG", "initializing connection sequence")
                mqttConnect()
                if (!wakeLock.isHeld) {
                    wakeLock.acquire(0)
                }
            } else if (networkStatus == 3) {
                Log.d("VTAG", "initializing disconnect sequence")
                mqttDisconnect()
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
            networkPrevStatus = networkStatus
        }
    }

    private inner class ServiceRunnable(var flag: Boolean): Runnable {
        override fun run() {
            try {
                if (flag && !mqttClient.isConnected && (networkStatus == 1 || networkStatus == 2)) {
                    Log.d("VTAG", "connecting thread: $networkPrevStatus, $networkStatus")
                    val socket = Socket("hardware.wscada.net", 1883)
                    socket.close()
                    if ((networkPrevStatus == 1 || networkPrevStatus == 2) && mqtt) {
                        mqttClient.disconnectForcibly(1, 1, false)
//                        mqttClient.close(true);
//                        mqttConfigured = false;
                    }
//                    configureAndConnectMqtt();
                    mqttClient.connect(connectOption)
                } else if (!flag && mqttClient.isConnected) {
                    mqttClient.unsubscribe(subscribeTopic)
                    mqttClient.disconnect()
//                    mqttClient.close(true);
//                    mqttConfigured = false;
                    Log.d("VTAG", "disconnected thread")
                } else if (!flag && !mqttClient.isConnected) {
                    mqttClient.disconnectForcibly(1, 1, false)
//                    mqttClient.close(true);
//                    mqttConfigured = false;
                    Log.d("VTAG", "forcefully disconnecting thread")
                }
            } catch (e: MqttException) {
                Log.d("VTAG", "okay cool thread whatever the error is $e")
                e.printStackTrace()
                try {
                    if (e.reasonCode == 0) {
                        if (networkStatus == 1 || networkStatus == 2) {
                            Log.d("VTAG", "Running again thread")
                            Thread(ServiceRunnable(true)).start()
                            Log.d("VTAG", "exiting this thing: ${e.reasonCode}")
                        }
                    } else if (e.reasonCode == 32110) {
                        // Connect already in progress
                        Thread(ServiceRunnable(true)).start()
                        Log.d("VTAG", "handling connection in progress")
                    } else {
                        Thread(ServiceRunnable(true)).start() // 32103 // java.net.ConnectException
                        Log.d("VTAG", "error cause is : ${e.reasonCode}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: ConnectException) {
                Log.d("VTAG", "No Internet Connection")
                e.printStackTrace()
                Thread(ServiceRunnable(true)).start()
            } catch (e: Exception) {
                Log.d("VTAG", "Unknown Error")
                e.printStackTrace()
            }
        }
    }

    private class PublishRunnable: Runnable {
        var mqttPayload = MqttMessage()

        constructor(mqttMessage: MqttMessage) {
            mqttPayload = mqttMessage
        }

        constructor(mqttMessage: ByteArray)  {
            mqttPayload.payload = mqttMessage
            mqttPayload.qos = 2
            mqttPayload.isRetained = false
        }

        override fun run() {
            try {
                if (mqttConfigured && mqttClient.isConnected) {
                    mqttClient.publish(publishTopic, mqttPayload)
                } else {
                    Log.d("VTAG", "Publish: no network connection")
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }
}
