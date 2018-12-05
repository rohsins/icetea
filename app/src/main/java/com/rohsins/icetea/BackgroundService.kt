package com.rohsins.icetea

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.IBinder
import android.util.Log

class BackgroundService: Service() {
    val connectivity = Connectivity();
    val error = connectivity.ConfigureAndConnectMqtt();

    override fun onCreate() {
        Log.d("VTAG", "starting service");
        this.registerReceiver(connectivity, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) : Int {
        return START_STICKY;
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    override fun onDestroy() {
        Log.d("VTAG", "killing service");
        connectivity.MqttDisconnect()
        this.unregisterReceiver(connectivity);
    }
}
