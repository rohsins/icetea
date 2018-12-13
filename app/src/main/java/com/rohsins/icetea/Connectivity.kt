package com.rohsins.icetea

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.lang.Exception
import java.net.ConnectException
import java.net.Socket

private const val mqttURI = "tcp://hardware.wscada.net:1883" // fixed
private const val mqttClientId = "rohsinsOKotlinW0" // Arbitrary
private const val mqttUserName = "developers" // fixed
private const val mqttPassword = "rtsr&ddevelopers" // fixed
private const val udi = "TestSequence1800" // Arbitrary
private const val subscribeTopic = "dev/subscriber/$udi" // fixed
private const val publishTopic = "dev/publisher/$udi" // fixed
private var mqttConfigured = false

class Connectivity : BroadcastReceiver() {
    private var mqtt = false
    private var mqttIsConnecting = false
    private var networkStatus: Int = 0
    private var networkPrevStatus = 3
    private val connectOption = MqttConnectOptions()
    private val handler = Handler()

    private var mqttConnectThread: Thread = Thread(ServiceRunnable(true))
    private var mqttDisconnectThread: Thread = Thread(ServiceRunnable(false))

    private lateinit var wakeLock: PowerManager.WakeLock
    private var mqttApplicationContext: Context? = null

    companion object {
        private lateinit var mqttClient: MqttAndroidClient

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

    fun mqttConnect() {
        try {
            when (mqttConnectThread.state) {
                Thread.State.TERMINATED -> {
                    mqttConnectThread.interrupt()
                    mqttConnectThread = Thread(ServiceRunnable(true))
                    mqttConnectThread.start()
                }
                Thread.State.NEW -> mqttConnectThread.start()
                Thread.State.RUNNABLE -> Log.d("VTAG", "Thread is already Running")
                else -> Log.d("VTAG", "Pointer went to undefined state")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun mqttDisconnect() {
        try {
            when (mqttDisconnectThread.state) {
                Thread.State.TERMINATED -> {
                    mqttDisconnectThread.interrupt()
                    mqttDisconnectThread = Thread(ServiceRunnable(false))
                    mqttDisconnectThread.start()
                }
                Thread.State.NEW -> mqttDisconnectThread.start()
                Thread.State.RUNNABLE -> Log.d("VTAG", "Thread is already Running")
                else -> Log.d("VTAG", "Pointer went to undefined state")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun mqttClose() {
        try {
            mqttConnectThread.interrupt()
            if (mqttClient.isConnected) {
                mqttUnsubscribe(subscribeTopic)
                mqttClient.disconnect()
//                    mqttClient.close()
            } else {
                mqttClient.disconnectForcibly(10)
//            mqttClient.disconnectForcibly(1, 1, false)
//                    mqttClient.close()
//            mqttClient.close(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        try {
            mqttConnectThread.interrupt()
            mqttDisconnectThread.interrupt()
            if (mqttClient.isConnected) {
                mqttClient.unsubscribe(subscribeTopic)
                mqttClient.disconnect()
            } else {
                // mqttClient.disconnectForcibly()
            }
            mqttClient.close()
            mqttConfigured = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun configureAndConnectMqtt(mqttContext: Context? = mqttApplicationContext) {
        if (mqttContext != null) {
            mqttApplicationContext = mqttContext
        }
        if (!mqttConfigured) {
            mqttConfigured = true

            val brokerAddress = mqttURI
            val clientId = mqttClientId
            val persistence: MqttClientPersistence? = null

            connectOption.userName = mqttUserName
            connectOption.password = mqttPassword.toCharArray()
            connectOption.isCleanSession = false // false important
            connectOption.isAutomaticReconnect = false // false important
            connectOption.keepAliveInterval = 10
            connectOption.connectionTimeout = 5
            connectOption.maxInflight = 10

            mqttClient = MqttAndroidClient(mqttContext, brokerAddress, clientId, persistence)
            mqttClient.setCallback(object: MqttCallbackExtended {

                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d("VTAG", "Connection Complete")
                    if (!mqtt) {
                        mqttSubscribe(subscribeTopic)
                        mqtt = true
                    }
                    mqttClient.publish("RTSR&D/baanvak/pub/ConnectInfo", "Device: $udi Connected".toByteArray(), 2,false)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("VTAG", message.toString())
                    mqttPublish("Message Acknowledged from $udi".toByteArray())
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.d("VTAG", "Connection lost. WTF!!!")
                    if (!mqttClient.isConnected) {
                        handler.postDelayed({mqttConnect()}, 3000)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {

                }
            })
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val conn = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = conn.activeNetworkInfo
        wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Connectivity::WakeLock")
        }

        when (networkInfo?.type) {
            ConnectivityManager.TYPE_WIFI -> {networkStatus = 1; Log.d("VTAG", "Wifi Connected")}
            ConnectivityManager.TYPE_MOBILE -> {networkStatus = 2; Log.d("VTAG", "Cellular Connected")}
            else -> {networkStatus = 3; Log.d("VTAG", "No Network Connection")}
        }

        if (networkStatus != networkPrevStatus) {
            if ((networkStatus == 1 || networkStatus == 2) && !mqttClient.isConnected) {
                Log.d("VTAG", "Initializing Connect Sequence")
                mqttConnect()
                if (!wakeLock.isHeld) { wakeLock.acquire(0) }
                Log.d("VTAG", "Wake Lock: ${wakeLock.isHeld}")
            } else if (networkStatus == 3) {
                Log.d("VTAG", "Intializing Disconnect Sequence")
                mqttConnectThread.interrupt()
                mqttDisconnect()
                if (wakeLock.isHeld) { wakeLock.release() }
                Log.d("VTAG", "Wake Lock: ${wakeLock.isHeld}")
            }
            networkPrevStatus = networkStatus
        }

        Log.d("VTAG", "The state of network: ${networkInfo?.state}")
    }

    private inner class ServiceRunnable(var flag: Boolean): Runnable {
        override fun run() {
            try {
                if (flag && !mqttClient.isConnected && (networkStatus == 1 || networkStatus == 2) && !mqttIsConnecting) {
                    mqttIsConnecting = true
                    Log.d("VTAG", "Thread: Connecting: $networkPrevStatus, $networkStatus")
                    val socket = Socket("hardware.wscada.net", 1883)
                    socket.close()
                    mqttClient.connect(connectOption)
                    mqttIsConnecting = false
                } else if (!flag && mqttClient.isConnected) {
                    mqttIsConnecting = false
                    mqttClient.unsubscribe(subscribeTopic)
                    mqttClient.disconnect()
                    Log.d("VTAG", "Thread: Disconnected")
                } else if (!flag && !mqttClient.isConnected) {
                    mqttIsConnecting = false
                    mqttClient.disconnectForcibly(10)
//                    mqttClient.disconnectForcibly(1, 1, false)
                    Log.d("VTAG", "Thread: Forcefully Disconnected")
                }
            } catch (e: MqttException) {
                mqttIsConnecting = false
                Log.d("VTAG", "Mqtt Exception is $e")
                e.printStackTrace()
                try {
                    when (e.reasonCode) {
                        0 -> {
                            if (networkStatus == 1 || networkStatus == 2) {
                                Log.d("VTAG", "Thread: Running Again")
                                mqttConnect()
                            }
                        }
                        32110 -> { mqttConnect(); Log.d("VTAG", "Handling connection in progress") }
                        5 -> { mqttClient.close(); mqttConfigured = false; configureAndConnectMqtt(); mqttConnect() }
                        else -> { mqttConnect(); Log.d("VTAG", "Mqtt error code: ${e.reasonCode}") }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: ConnectException) {
                mqttIsConnecting = false
                Log.d("VTAG", "No Internet Connection")
                e.printStackTrace()
                mqttConnect()
            } catch (e: Exception) {
                mqttIsConnecting = false
                Log.d("VTAG", "Unknown Error")
                e.printStackTrace()
                handler.postDelayed({ mqttConnect() }, 2000)
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
