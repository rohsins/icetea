package com.rohsins.icetea

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.v4.app.NotificationCompat
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    val channelId = "ForegroundService";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Thread({
//            Log.d("VTAG", "address: ${InetAddress.getByName("hardware.wscada.net").hostAddress}");
//        }).start();

//        createNotificationChannel();
//        var mBuilder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.abc_btn_check_material)
//            .setContentTitle("My notification")
//            .setContentText("Much longer text that cannot fit one line...")
//            .setStyle(NotificationCompat.BigTextStyle()
//            .bigText("Much longer text that cannot fit one line..."))
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Intent(this, BackgroundService::class.java).also {intent ->
            startService(intent);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(intent);
//            } else {
//                startService(intent);
//            }
        }

//        Thread(LooperThread()).start();

        val gridView: GridView = findViewById(R.id.gridView);
        gridView.adapter = ImageAdapter(this);

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            Toast.makeText(this, "$position", Toast.LENGTH_SHORT).show();
            if (position == 1) {
                Connectivity.MqttPublish("what is up".toByteArray());
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "YesName";
            val descriptionText = "cool thing";
            val importance = NotificationManager.IMPORTANCE_DEFAULT;
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText;
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            notificationManager.createNotificationChannel(channel);
        }
    }

    override fun onDestroy() {
        super.onDestroy();
        Intent(this, BackgroundService::class.java).also {intent ->
            stopService(intent);
        }
    }

//    var handler = Handler();
//
//    internal inner class LooperThread : Runnable {
//        override fun run() {
//            try {
//                var socket = Socket("hardware.wscada.net", 1883);
//                socket.close();
//                Log.d("VTAG", "Internet is Alive");
//                Log.d("VTAG", "so: ${socket.inetAddress}");
//            } catch (ex: Exception) {
//                Log.d("VTAG", "Internet is Dead");
//                Log.d("VTAG", "Esdfsad: $ex");
//            }
//
//
//            handler.postDelayed({Thread(LooperThread()).start()}, 1000)
//        }
//    }
}
