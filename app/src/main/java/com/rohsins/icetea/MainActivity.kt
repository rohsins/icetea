package com.rohsins.icetea

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        val channelId = "ForegroundService";
        var mBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.abc_btn_check_material)
            .setContentTitle("My notification")
            .setContentText("Much longer text that cannot fit one line...")
            .setStyle(NotificationCompat.BigTextStyle()
            .bigText("Much longer text that cannot fit one line..."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        private fun createNotificationChannel() {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
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

        Intent(this, BackgroundService::class.java).also {intent ->
            startService(intent);
        }

        val gridView: GridView = findViewById(R.id.gridView);
        gridView.adapter = ImageAdapter(this);

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            Toast.makeText(this, "$position", Toast.LENGTH_SHORT).show();
            if (position == 1) {
                Connectivity.MqttPublish("what is up".toByteArray());
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy();
        Intent(this, BackgroundService::class.java).also {intent ->
            stopService(intent);
        }
    }
}
