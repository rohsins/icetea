package com.rohsins.icetea

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

val connectivity: Connectivity = Connectivity()

val handler: Handler = Handler()

class MainActivity : AppCompatActivity() {
    companion object {
        var serviceRunning = false
        private const val REQUEST_CODE = 10101
        private const val LOCATION_PERMISSION_ID = 42
        var locationPermission = false
    }

    fun checkLocationPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermission = true
            return true
        }
        locationPermission = false
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridView.adapter = ImageAdapter(this)

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
//            Toast.makeText(this, "$position", Toast.LENGTH_SHORT).show()
            if (position == 0) {
                val intent = Intent(this, Lights::class.java)
                startActivity(intent)
            } else if (position == 9 && !serviceRunning) {
                serviceRunning = true
                Intent(this, BackgroundService::class.java).also { intent ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                }
                Toast.makeText(this, "service started", Toast.LENGTH_SHORT).show()
            } else if (position == 9 && serviceRunning) {
                serviceRunning = false
                Intent(this, BackgroundService::class.java).also {intent ->
                    stopService(intent)
                }
                Toast.makeText(this, "killing service", Toast.LENGTH_SHORT).show()
            } else if (position == 8) {
                Toast.makeText(this, UDI, Toast.LENGTH_LONG).show()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                //finish()
            } else {
                checkDrawOverlayPermission()
            }
        } else {
            TODO("VERSION.SDK_INT < M")
        }

        if (!checkLocationPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_ID
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkDrawOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_CODE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                //finish()
            } else {
                Toast.makeText(this, "Sorry. Can't draw overlays without permission...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
