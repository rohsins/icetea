package com.rohsins.icetea

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class BackgroundService: Service() {
    private var serviceChannelId: String? = null
    private var serviceNotificationBuilder: NotificationCompat.Builder? = null
    private var serviceNotificationManager: NotificationManager? = null
    private var infoChannelId: String? = null
    private var infoNotificationBuilder: NotificationCompat.Builder? = null
    private val kSignalReceiver = KSignalReceiver()

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannelId = createServiceNotificationChannel("183290", "Foreground Service Channel")
            infoChannelId = createInfoNotificationChannel("183292", "Info Channel")
        } else {
            serviceChannelId = "Foreground Service Channel"
            infoChannelId = "Info Channel"
        }

        serviceNotificationBuilder = NotificationCompat.Builder(this, serviceChannelId as String)
            .setSmallIcon(R.mipmap.icetea)
            .setContentTitle("The Donkey Application")
            .setContentText("Foreground Service")

        infoNotificationBuilder = NotificationCompat.Builder(this, infoChannelId as String)
            .setSmallIcon(R.mipmap.icetea)
            .setAutoCancel(true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createServiceNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        serviceNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        serviceNotificationManager!!.createNotificationChannel(channel)
        return channelId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createInfoNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        val infoNotificationManager= getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        infoNotificationManager.createNotificationChannel(channel)
        return channelId
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) : Int {
        Log.d("VTAG", "starting service")
        val filter = IntentFilter("KSignalReceiverFlag")
//        registerReceiver(kSignalReceiver, filter)
        startForeground(291, serviceNotificationBuilder!!.build())
        connectivity.configureAndConnectMqtt(applicationContext)
        EventBus.getDefault().register(this)
//        KSignalTrigger(30000)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("VTAG", "killing service")
        connectivity.unconfigureAndDisconnectMqttForcibly()
//        unregisterReceiver(kSignalReceiver)
        EventBus.getDefault().unregister(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("VTAG", "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
    }

    private fun infoNotificationNotify(id: Int, title: String, msg: String) {
        infoNotificationBuilder!!.setContentTitle(title)
        infoNotificationBuilder!!.setContentText(msg)
        infoNotificationBuilder!!.priority = Notification.PRIORITY_HIGH
        infoNotificationBuilder!!.setAutoCancel(true)
        infoNotificationBuilder!!.setShowWhen(true)
        infoNotificationBuilder!!.setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_LIGHTS)
        with(NotificationManagerCompat.from(this)) {
            notify(id, infoNotificationBuilder!!.build())
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessage(event: MessageEvent) {
        Thread(LightRoutine(event, this)).start()

        try {
            val jObject = JSONObject(event.mqttMessage.toString())
            val payloadType = jObject.getString("payloadType")
            if (payloadType.contains("alert")) {
                var title = "Alert"
                val payload = jObject.getJSONObject("payload")
                when (payload.getInt("thingCode")) {
                    308 -> {
                        title = "Water Spill"
                    }
                    309 -> {
                        title = "Door Sensor"
                    }
                    310 -> {
                        title = "Smoke Sensor"
                    }
                    389 -> {
                        title = "Motion Sensor"
                    }
                }
                infoNotificationNotify(
                    payload.getInt("thingCode"),
                    title,
                    payload.getString("message")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun KSignalTrigger(triggerTime: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, Intent("KSignalReceiverFlag"),
            PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + triggerTime, pendingIntent)
        }
    }

    inner class KSignalReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            KSignalTrigger(30000)
            Log.d("VTAG", "KSignal Triggered")
        }
    }
}
