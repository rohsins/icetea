package com.rohsins.icetea

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@SuppressLint("StaticFieldLeak")
val connectivity: Connectivity = Connectivity()

class BackgroundService: Service() {
    private var serviceChannelId: String? = null
    private var serviceNotificationBuilder: NotificationCompat.Builder? = null
    private var serviceNotificationManager: NotificationManager? = null
    private var serviceAlive = false
    private val kSignalReceiver = KSignalReceiver()
//    val connectivity = Connectivity()

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannelId = createNotificationChannel("183290", "Foreground Service")
        } else {
            serviceChannelId = "Foreground Service"
        }

        serviceNotificationBuilder = NotificationCompat.Builder(this, serviceChannelId as String)
            .setSmallIcon(R.mipmap.icetea)
            .setContentTitle("rohsins application")
            .setContentText("oreo")
            .setBadgeIconType(R.mipmap.icetea)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        serviceNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        serviceNotificationManager!!.createNotificationChannel(channel)
        return channelId
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) : Int {
        Log.d("VTAG", "starting service")
        serviceAlive = true
        val filter = IntentFilter("KSignalReceiverFlag")
        registerReceiver(kSignalReceiver, filter)
        startForeground(291, serviceNotificationBuilder!!.build())
        connectivity.configureAndConnectMqtt(applicationContext)
        EventBus.getDefault().register(this)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("VTAG", "killing service")
        connectivity.mqttPublish("Service Killed".toByteArray())
        Handler().postDelayed({ connectivity.unconfigureAndDisconnectMqtt() }, 2000)
        unregisterReceiver(kSignalReceiver)
        EventBus.getDefault().unregister(this)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pendingIntent = PendingIntent.getForegroundService(
                applicationContext, 0,
                Intent(this, BackgroundService::class.java), PendingIntent.FLAG_ONE_SHOT
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 3000, pendingIntent)
        } else {
            val pendingIntent = PendingIntent.getService(
                applicationContext, 0,
                Intent(this, BackgroundService::class.java), PendingIntent.FLAG_ONE_SHOT)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, 3000, pendingIntent)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("VTAG", "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
//
//        val filter = IntentFilter("KSignalReceiverFlag")
//        registerReceiver(kSignalReceiver, filter)

//        val intent = Intent("KSignalReceiverFlag")
//        sendBroadcast(intent)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessage(event: MessageEvent) {
        Log.d("VTAG", "event message: ${event.mqttMessage}")
    }

    inner class KSignalReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            startForeground(291, serviceNotificationBuilder!!.build())
            Log.d("VTAG", "KSignal Triggered")
        }
    }
}
