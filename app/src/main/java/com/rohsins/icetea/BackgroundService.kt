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
import android.util.Log

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
//        this.registerReceiver(Connectivity, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        Connectivity.configureAndConnectMqtt(applicationContext)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("VTAG", "killing service")
        this.unregisterReceiver(Connectivity)
        Connectivity.unconfigureAndDisconnectMqtt()
        unregisterReceiver(kSignalReceiver)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("VTAG", "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
//
//        val filter = IntentFilter("KSignalReceiverFlag")
//        registerReceiver(kSignalReceiver, filter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pendingIntent = PendingIntent.getForegroundService(applicationContext, 0,
                Intent(this, BackgroundService::class.java), PendingIntent.FLAG_ONE_SHOT)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 0, pendingIntent)
        }

//        val intent = Intent("KSignalReceiverFlag")
//        sendBroadcast(intent)
    }

    inner class KSignalReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            startForeground(291, serviceNotificationBuilder!!.build())
            Log.d("VTAG", "KSig Triggered")

        }
    }
}
