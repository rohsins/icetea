package com.rohsins.icetea

import android.annotation.SuppressLint
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.lang.Exception
import java.net.ConnectException
import java.net.Socket
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject

private const val mqttURI = "tcp://rohitsingh.com.np:1883" // fixed
private const val mqttUserName = "rohsins" // fixed
private const val mqttPassword = "escapecharacters" // fixed
var UDI = "null" // Arbitrary
const val THINGCODE = 12001 //androidMobile
private var subscribeTopic = "RTSR&D/baanvak/sub/$UDI" // fixed
private var publishTopic = "RTSR&D/baanvak/pub/$UDI" // fixed
private var mqttConfigured = false
private var connectRequest = false
private const val registrationTopic = "RTSR&D/baanvak/ureq"

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

    private lateinit var sharedPreferences: SharedPreferences

    private fun setUDI(udi: String) {
        UDI = udi
        subscribeTopic = "RTSR&D/baanvak/sub/$UDI"
        publishTopic = "RTSR&D/baanvak/pub/$UDI"
    }

    private fun udiRoutine() {
        sharedPreferences = mqttApplicationContext!!.getSharedPreferences("udiStore", 0)
        if (sharedPreferences.getString("UDI", "null") != "null") {
            setUDI(sharedPreferences.getString("UDI", "null"))
        } else {
            val processorID = Settings.Secure.getString(mqttApplicationContext!!.contentResolver, Settings.Secure.ANDROID_ID) + mqttApplicationContext!!.applicationInfo.packageName
            val queue = Volley.newRequestQueue(mqttApplicationContext)
            val url = "http://developer.wscada.net:88/api/device/register"

            val stringRequest = object : StringRequest(
                Method.POST, url,
                Response.Listener<String> { response ->
                    if (response.toString().length == 16) {
                        val registrationJSON = JSONObject()
                        registrationJSON.put("udi", response.toString())
                        val mqttMessageRegistration = MqttMessage()
                        mqttMessageRegistration.payload = registrationJSON.toString().toByteArray()
                        mqttMessageRegistration.qos = 2
                        mqttClient.publish(registrationTopic, mqttMessageRegistration)
                        with(sharedPreferences.edit()) {
                            putString("UDI", response.toString())
                            commit()
                        }
                        setUDI(response.toString())
                    } else Log.d("VTAG", "error: $response")
                },
                Response.ErrorListener { error ->
                    error.printStackTrace()
                }
            ) {
                override fun getBodyContentType(): String? {
                    return "application/json"
                }

                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray? {
                    val requestJSON = JSONObject()
                    requestJSON.put("messagingPattern", "subscriber+")
                    requestJSON.put("type", "androidMobile")
                    requestJSON.put("processorID", processorID)
                    return requestJSON.toString().toByteArray()
                }
            }
            queue!!.add(stringRequest)
        }
    }

    fun mqttPublish(mqttMessage: MqttMessage) {
        Thread(PublishRunnable(mqttMessage)).start()
    }

    fun mqttPublish(mqttMessage: ByteArray) {
        Thread(PublishRunnable(mqttMessage)).start()
    }

    fun mqttPublish(mqttMessage: ByteArray, qos: Int) {
        Thread(PublishRunnable(mqttMessage, qos)).start()
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
                    Thread.State.RUNNABLE -> {
                        connectRequest = true
                        Log.d("VTAG", "Connect thread is already Running")
                    }
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

    private val mqttCallbackExtended = object: MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            Log.d("VTAG", "Connection Complete")
            if (firstTimeMqttConnect) {
                firstTimeMqttConnect = false
                if (UDI == "null") {
                    udiRoutine()
                }
                mqttSubscribe(subscribeTopic)
            }
            mqttClient.publish("android/pub/ConnectInfo", "Device: $UDI Connected".toByteArray(), 2,false)
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
//                    Log.d("VTAG", message.toString())
//                    mqttPublish("Message Acknowledged from $UDI".toByteArray())
            if (topic!!.contentEquals(subscribeTopic)) {
                MessageEvent.mqttMessage = message
                MessageEvent.mqttTopic = topic
                EventBus.getDefault().post(MessageEvent)
            }
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
    }

    @SuppressLint("HardwareIds")
    fun configureAndConnectMqtt(mqttContext: Context? = mqttApplicationContext) {
        if (mqttContext != null) {
            mqttApplicationContext = mqttContext
        }
        if (!mqttConfigured) {
            val brokerAddress = mqttURI
            val clientId = Settings.Secure.getString(mqttContext!!.contentResolver, Settings.Secure.ANDROID_ID) + mqttApplicationContext!!.applicationInfo.packageName
            val persistence: MqttClientPersistence? = null

            connectOption.userName = mqttUserName
            connectOption.password = mqttPassword.toCharArray()
            connectOption.isCleanSession = false // false important
            connectOption.isAutomaticReconnect = false // false important
            connectOption.keepAliveInterval = 39
            connectOption.connectionTimeout = 5
            connectOption.maxInflight = 50

            mqttClient = MqttAndroidClient(mqttContext, brokerAddress, clientId, persistence)
            mqttClient.setCallback(mqttCallbackExtended)
            mqttApplicationContext!!.registerReceiver(this, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            mqttConfigured = true
            mqttConnect()
        } else {
            mqttConnect()
        }
    }

    fun unconfigureAndDisconnectMqtt() {
        if (mqttConfigured) {
            mqttConfigured = false
            firstTimeMqttConnect = true
            mqttApplicationContext!!.unregisterReceiver(this)
            mqttDestroy()
        }
    }

    fun unconfigureAndDisconnectMqttForcibly() {
        if (mqttConfigured) {
            mqttConfigured = false
            firstTimeMqttConnect = true
            mqttApplicationContext!!.unregisterReceiver(this)
            mqttClient.unregisterResources()
            mqttClient.close()
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (useMqtt) {
            val conn = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = conn.activeNetworkInfo
//            wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
//                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Connectivity::WakeLock")
//            }

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
//                    if (!wakeLock.isHeld) {
//                        wakeLock.acquire(0)
//                    }
                    mqttConnect()
//                    Log.d("VTAG", "Wake Lock: ${wakeLock.isHeld}")
                } else if (networkStatus == 3) {
                    Log.d("VTAG", "Intializing Disconnect Sequence")
                    mqttConnectThread.interrupt()
                    mqttDisconnect()
//                    if (wakeLock.isHeld) {
//                        wakeLock.release()
//                    }
//                    Log.d("VTAG", "Wake Lock: ${wakeLock.isHeld}")
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
            if (connectRequest) {
                connectRequest = false
//                mqttConnect()
                handler.postDelayed({mqttConnect()}, 2000)
                Log.d("VTAG", "Connection request between thread execution")
            }
            Log.d("VTAG", "Con/Dis thread terminated")
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

        constructor(mqttMessage: ByteArray, qos: Int)  {
            mqttPayload.payload = mqttMessage
            mqttPayload.qos = qos
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

object MessageEvent {
    var mqttMessage: MqttMessage? = null
    var mqttTopic: String? = null
}
