package com.rohsins.icetea

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log

class BackgroundService: Service() {
    private val connectivity = Connectivity()

    private var serviceChannelId: String? = null
    private var serviceNotificationBuilder: NotificationCompat.Builder? = null
    private var serviceNotificationManager: NotificationManager? = null
    private var serviceAlive = false

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannelId = createNotificationChannel("183290", "Foreground Service")
        } else {
            serviceChannelId = "Foreground Service"
        }

        serviceNotificationBuilder = NotificationCompat.Builder(this, serviceChannelId as String)
            .setSmallIcon(R.drawable.notify_panel_notification_icon_bg)
            .setContentTitle("icetea service")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
//        channel.lightColor = Color.BLUE;
//        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE;
        serviceNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        serviceNotificationManager!!.createNotificationChannel(channel)
        return channelId
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) : Int {
        Log.d("VTAG", "starting service")
        serviceAlive = true
        startForeground(291, serviceNotificationBuilder!!.build())
        this.registerReceiver(connectivity, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        connectivity.configureAndConnectMqtt(applicationContext)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("VTAG", "killing service")
        this.unregisterReceiver(connectivity)
        connectivity.destroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("VTAG", "onTaskRemoved")
        super.onTaskRemoved(rootIntent)

        val intent = Intent(applicationContext, BackgroundService::class.java)
        val pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_ONE_SHOT)
        val alarmManager = (getSystemService(Context.ALARM_SERVICE)) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, pendingIntent)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(rootIntent);
//        } else {
//            startService(rootIntent);
//        }
    }
}
