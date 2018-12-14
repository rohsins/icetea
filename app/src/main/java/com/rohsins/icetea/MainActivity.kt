package com.rohsins.icetea

import android.content.Intent
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    companion object {
        var serviceRunning = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gridView: GridView = findViewById(R.id.gridView)
        gridView.adapter = ImageAdapter(this)

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            Toast.makeText(this, "$position", Toast.LENGTH_SHORT).show()
            if (position == 0 && !serviceRunning) {
                serviceRunning = true
                Intent(this, BackgroundService::class.java).also { intent ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                }
                Toast.makeText(this, "service started", Toast.LENGTH_SHORT).show()
            } else if (position == 1) {
                Connectivity.mqttPublish("reply: what is up".toByteArray())
            } else if (position == 20 && serviceRunning) {
                serviceRunning = false
                Intent(this, BackgroundService::class.java).also {intent ->
                    stopService(intent)
                }
                Toast.makeText(this, "killing service", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
