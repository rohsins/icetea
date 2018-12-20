package com.rohsins.icetea

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.lang.Exception
import java.net.ConnectException
import java.net.Socket

private const val mqttURI = "tcp://hardware.wscada.net:1883" // fixed
private const val mqttClientId = "rohsinsOKotlinW1" // Arbitrary
private const val mqttUserName = "rtshardware" // fixed
private const val mqttPassword = "rtshardware" // fixed
private const val udi = "TestSequence1801" // Arbitrary
private const val subscribeTopic = "RTSR&D/baanvak/sub/$udi" // fixed
private const val publishTopic = "RTSR&D/baanvak/pub/$udi" // fixed
private var mqttConfigured = false

class Connectivity : BroadcastReceiver() {
    private var useMqtt = true
    private var firstTimeMqttConnect = true
    private var mqttIsConnecting = false
    private var networkStatus: Int = 0
    private var networkPrevStatus = 3
    private val connectOption = MqttConnectOptions()
    private val handler = Handler()

    private var mqttConnectThread: Thread = Thread(ServiceRunnable(true))
    private var mqttDisconnectThread: Thread = Thread(ServiceRunnable(false))

    private lateinit var wakeLock: PowerManager.WakeLock
    private var mqttApplicationContext: Context? = null
    private var mqttConnectLock: Boolean = false
    private lateinit var mqttClient: MqttAndroidClient

    fun mqttPublish(mqttMessage: MqttMessage) {
        Thread(PublishRunnable(mqttMessage)).start()
    }

    fun mqttPublish(mqttMessage: ByteArray) {
        Thread(PublishRunnable(mqttMessage)).start()
    }

    private fun mqttSubscribe(topic: String) {
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

    private fun mqttUnsubscribe(topic: String) {
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

    private fun mqttConnect() {
        if (!mqttConnectLock) {
            try {
                when (mqttConnectThread.state) {
                    Thread.State.TERMINATED -> {
                        mqttConnectThread.interrupt()
                        mqttConnectThread = Thread(ServiceRunnable(true))
                        mqttConnectThread.start()
                    }
                    Thread.State.NEW -> mqttConnectThread.start()
                    Thread.State.RUNNABLE -> Log.d("VTAG", "Connect thread is already Running")
                    else -> Log.d("VTAG", "Pointer went to undefined state")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.d("VTAG", "Mqtt Connect Lock is activated: Not connecting")
        }
    }

    private fun mqttDisconnect() {
        try {
            when (mqttDisconnectThread.state) {
                Thread.State.TERMINATED -> {
                    mqttDisconnectThread.interrupt()
                    mqttDisconnectThread = Thread(ServiceRunnable(false))
                    mqttDisconnectThread.start()
                }
                Thread.State.NEW -> mqttDisconnectThread.start()
                Thread.State.RUNNABLE -> Log.d("VTAG", "Disconnect thread is already Running")
                else -> Log.d("VTAG", "Pointer went to undefined state")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mqttDestroy() {
        try {
            mqttConnectLock = true
            mqttConnectThread.interrupt()
            mqttDisconnectThread.interrupt()
            if (mqttClient.isConnected) {
                Log.d("VTAG", "Mqtt Client Connection: ${mqttClient.isConnected}")
                mqttClient.unsubscribe(subscribeTopic)
                mqttClient.disconnect(this, object: IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("VTAG", "Mqtt client disconnect successful: token thing")
                        mqttClient.unregisterResources()
                        mqttClient.close()
                        handler.postDelayed({mqttConnectLock = false}, 0)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d("VTAG", "Mqtt client disconnect failed: token thing")
//                        mqttClient.disconnectForcibly()
                        mqttClient.unregisterResources()
                        mqttClient.close()
                        handler.postDelayed({mqttConnectLock = false}, 0)
                    }
                })
            } else {
//                mqttClient.disconnectForcibly()
                mqttClient.unregisterResources()
                mqttClient.close()
                handler.postDelayed({mqttConnectLock = false}, 0)
            }
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
            connectOption.keepAliveInterval = 30
            connectOption.connectionTimeout = 5
            connectOption.maxInflight = 50

            mqttClient = MqttAndroidClient(mqttContext, brokerAddress, clientId, persistence)
            mqttClient.setCallback(object: MqttCallbackExtended {

                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d("VTAG", "Connection Complete")
                    if (firstTimeMqttConnect) {
                        mqttSubscribe(subscribeTopic)
                        firstTimeMqttConnect = false
                    }
                    mqttClient.publish("RTSR&D/baanvak/pub/ConnectInfo", "Device: $udi Connected".toByteArray(), 2,false)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d("VTAG", message.toString())
                    mqttPublish("Message Acknowledged from $udi".toByteArray())
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.d("VTAG", "Connection lost. WTF!!!")
                    if (!mqttClient.isConnected && !mqttConnectLock) {
                        Log.d("VTAG", "Entered in post delay handle execution: $mqttConnectLock")
                        handler.postDelayed({mqttConnect()}, 2000)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {

                }
            })
            mqttApplicationContext!!.registerReceiver(this, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
//            mqttConnect()
        }
    }

    fun unconfigureAndDisconnectMqtt() {
        if (mqttConfigured) {
            mqttConfigured = false
            mqttApplicationContext!!.unregisterReceiver(this)
            mqttDestroy()
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (useMqtt) {
            val conn = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = conn.activeNetworkInfo
            wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Connectivity::WakeLock")
            }

            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> {
                    networkStatus = 1; Log.d("VTAG", "Wifi Connected")
                }
                ConnectivityManager.TYPE_MOBILE -> {
                    networkStatus = 2; Log.d("VTAG", "Cellular Connected")
                }
                else -> {
                    networkStatus = 3; Log.d("VTAG", "No Network Connection")
                }
            }

            if (networkStatus != networkPrevStatus) {
                if ((networkStatus == 1 || networkStatus == 2) && !mqttClient.isConnected) {
                    Log.d("VTAG", "Initializing Connect Sequence")
                    if (!wakeLock.isHeld) {
                        wakeLock.acquire(0)
                    }
                    mqttConnect()
                    Log.d("VTAG", "Wake Lock: ${wakeLock.isHeld}")
                } else if (networkStatus == 3) {
                    Log.d("VTAG", "Intializing Disconnect Sequence")
                    mqttConnectThread.interrupt()
                    mqttDisconnect()
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                    Log.d("VTAG", "Wake Lock: ${wakeLock.isHeld}")
                }
                networkPrevStatus = networkStatus
            }

            Log.d("VTAG", "The state of network: ${networkInfo?.state}")
        }
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
//                    mqttClient.disconnectForcibly()
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

    private inner class PublishRunnable: Runnable {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
