package com.rohsins.icetea

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class BackgroundService: Service() {
    val connectivity = Connectivity();
    val error = connectivity.ConfigureAndConnectMqtt();

    override fun onCreate() {
        Log.d("VTAG", "service has started");

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) : Int {
        return START_STICKY;

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null;

    }

    override fun onDestroy() {

    }
}
