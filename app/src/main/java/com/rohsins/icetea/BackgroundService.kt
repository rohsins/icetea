package com.rohsins.icetea

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log

class BackgroundService: Service() {
    val connectivity = Connectivity();
    var wakeLock: PowerManager.WakeLock? = null;

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) : Int {
        Log.d("VTAG", "starting service");
        connectivity.ConfigureAndConnectMqtt();
        this.registerReceiver(connectivity, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                    acquire()
                }
            };
        return START_STICKY;
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    override fun onDestroy() {
        Log.d("VTAG", "killing service");
        connectivity.MqttDisconnect()
        this.unregisterReceiver(connectivity);
        wakeLock!!.release();
    }
}
