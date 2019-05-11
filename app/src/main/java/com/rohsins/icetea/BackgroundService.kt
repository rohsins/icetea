package com.rohsins.icetea

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

class BackgroundService: Service() {
    private var serviceChannelId: String? = null
    private var serviceNotificationBuilder: NotificationCompat.Builder? = null
    private var serviceNotificationManager: NotificationManager? = null
    private var serviceAlive = false
    private val kSignalReceiver = KSignalReceiver()

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannelId = createNotificationChannel("183290", "Foreground Service")
        } else {
            serviceChannelId = "Foreground Service"
        }

        serviceNotificationBuilder = NotificationCompat.Builder(this, serviceChannelId as String)
            .setSmallIcon(R.mipmap.icetea)
//            .setContentTitle("rohsins application")
//            .setContentText("oreo")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
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
//        KSignalTrigger(30000)
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
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("VTAG", "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessage(event: MessageEvent) {
        Thread(LightRoutine(event, this)).start()
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
