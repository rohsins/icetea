package com.rohsins.icetea

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.JsonWriter
import android.util.Log
import com.rohsins.icetea.DataModel.DeviceDao
import com.rohsins.icetea.DataModel.DeviceDatabase
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONStringer

class BackgroundService: Service() {
    private var serviceChannelId: String? = null
    private var serviceNotificationBuilder: NotificationCompat.Builder? = null
    private var serviceNotificationManager: NotificationManager? = null
    private var infoChannelId: String? = null
    private var infoNotificationBuilder: NotificationCompat.Builder? = null
    private val kSignalReceiver = KSignalReceiver()

    private lateinit var deviceDao: DeviceDao

    private lateinit var windowManager: WindowManager
    private lateinit var linearLayout: LinearLayout
    private lateinit var frameLayout: FrameLayout
    private var viewOccupied = false

    private lateinit var locationManager: LocationManager
    private var locationTimeCheck: Long = 0
    private var locationLatitude: Float = 0f
    private var locationLongitude: Float = 0f
    private var locationAltitude: Float = 0f

    private inner class PopupRunnable: Runnable {
        private var textArg: String

        constructor(textRunnableArg: String) {
            this.textArg = textRunnableArg
        }

        override fun run() {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val layoutParamsType: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                LayoutParams.TYPE_PHONE
            }
            val layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                layoutParamsType,
                0,
                PixelFormat.TRANSLUCENT
            )

            layoutParams.gravity = Gravity.CENTER or Gravity.START

            val popupText = TextView(applicationContext)
            popupText.textSize = 20f
            popupText.setTextColor(Color.BLACK)
            popupText.text = textArg

            Log.i("popup", "before thing")
            linearLayout?.let {
                it.setOnTouchListener(View.OnTouchListener { v, event ->
                    v?.performClick()
                    frameLayout.removeAllViews()
                    linearLayout.removeAllViews()
                    windowManager.removeView(v)
                    viewOccupied = false
                    true
                })
                if (!viewOccupied) {
                    frameLayout.addView(popupText)
                    linearLayout.addView(frameLayout)
                    windowManager.addView(linearLayout, layoutParams)
                    viewOccupied = true
                } else {
                    frameLayout.removeAllViews()
                    frameLayout.addView(popupText)
                    linearLayout.removeAllViews()
                    linearLayout.addView(frameLayout)
                    windowManager.updateViewLayout(linearLayout, layoutParams)
                }
            }

            Log.i("popup", "ran")
        }
    }

    private fun popup(text: String) {
        handler.post(PopupRunnable(text))
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        linearLayout = LinearLayout(applicationContext)
        frameLayout = FrameLayout(applicationContext)
        val frameLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.START)
        frameLayoutParams.setMargins(dp(20), dp(20), dp(20), dp(20))
        frameLayout.layoutParams = frameLayoutParams
        frameLayout.background = getDrawable(R.drawable.popup_view)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if ((locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            && MainActivity.locationPermission) {
            Log.i("location", "requesting location")
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                locationListener
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannelId = createServiceNotificationChannel("183290", "Foreground Service Channel")
            infoChannelId = createInfoNotificationChannel("183292", "Info Channel")
        } else {
            serviceChannelId = "Foreground Service Channel"
            infoChannelId = "Info Channel"
        }

        serviceNotificationBuilder = NotificationCompat.Builder(this, serviceChannelId as String)
            .setSmallIcon(R.mipmap.icetea)
            .setContentTitle("The Donkey Application")
            .setContentText("Foreground Service")

        infoNotificationBuilder = NotificationCompat.Builder(this, infoChannelId as String)
            .setSmallIcon(R.mipmap.icetea)
            .setAutoCancel(true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createServiceNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        serviceNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        serviceNotificationManager!!.createNotificationChannel(channel)
        return channelId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createInfoNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        val infoNotificationManager= getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        infoNotificationManager.createNotificationChannel(channel)
        return channelId
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) : Int {
        Log.d("VTAG", "starting service")
//        val filter = IntentFilter("KSignalReceiverFlag")
//        registerReceiver(kSignalReceiver, filter)
        deviceDao = DeviceDatabase.getInstance(this).deviceDao()

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
        locationManager.removeUpdates(locationListener)
        connectivity.unconfigureAndDisconnectMqttForcibly()
//        unregisterReceiver(kSignalReceiver)
        EventBus.getDefault().unregister(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("VTAG", "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
    }

    private fun infoNotificationNotify(id: Int, title: String, msg: String) {
        infoNotificationBuilder!!.setContentTitle(title)
        infoNotificationBuilder!!.setContentText(msg)
        infoNotificationBuilder!!.priority = Notification.PRIORITY_HIGH
        infoNotificationBuilder!!.setAutoCancel(true)
        infoNotificationBuilder!!.setWhen(System.currentTimeMillis())
        infoNotificationBuilder!!.setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_LIGHTS)
        with(NotificationManagerCompat.from(this)) {
            notify(id, infoNotificationBuilder!!.build())
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessage(event: MessageEvent) {
        Thread(LightRoutine(event, this)).start()

        try {
            val jObject = JSONObject(event.mqttMessage.toString())
            val payloadType = jObject.getString("payloadType")
            val payload = jObject.getJSONObject("payload")
            if (payloadType.contains("alert")) {
                lateinit var title: String // = "Unknown"
                try {
                    title = deviceDao.getDevice(jObject.getString("publisherudi")).type.capitalize().replace("Sensor", " Sensor")
                } catch (e: Exception) {
                    title = deviceDao.getDevice(jObject.getString("subscriberudi")).type.capitalize().replace("Sensor", " Sensor")
                }
//                when (payload.getInt("thingCode")) {
//                    308 -> {
//                        title = "Water Spill"
//                    }
//                    309 -> {
//                        title = "Door Sensor"
//                    }
//                    310 -> {
//                        title = "Smoke Sensor"
//                    }
//                    389 -> {
//                        title = "Motion Sensor"
//                    }
//                }
                infoNotificationNotify(
                    deviceDao.getDevice(jObject.getString("publisherudi")).id,
                    title,
                    payload.getString("message") + " @ " +
                        try {
                            deviceDao.getDeviceAlias(jObject.getString("publisherudi"))
                        } catch (e: Exception) {
                            deviceDao.getDeviceAlias(jObject.getString("subscriberudi"))
                        }
                )
            } else if (payloadType.contains("cap")) {
                popup(payload.getString("message"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    private fun dp(dp: Int): Int {
        val density = resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    private val locationListener = object: LocationListener {
        override fun onLocationChanged(location: Location?) {
            if (((location!!.time - locationTimeCheck) > 60000)
                && ((location!!.latitude.toFloat() != locationLatitude)
                    || location!!.longitude.toFloat() != locationLongitude)) {
                locationTimeCheck = location!!.time
                locationLatitude = location!!.latitude.toFloat()
                locationLongitude = location!!.longitude.toFloat()
                locationAltitude = location!!.altitude.toFloat()

                val locationJSON = JSONObject()
                locationJSON.put("latitude", locationLatitude)
                locationJSON.put("longitude", locationLongitude)
                locationJSON.put("altitude", locationAltitude)
                val extraJSON = JSONObject()
                extraJSON.put("location", locationJSON)
                val essentialJSON = JSONObject()
                essentialJSON.put("publisherudi", UDI)
                essentialJSON.put("payloadType", "info")
                val updateJSON = JSONObject()
                updateJSON.put("essential", essentialJSON)
                updateJSON.put("extra", extraJSON)
                connectivity.mqttPublish(updateJSON.toString().toByteArray())
                Log.i("location", "location changed and location published")
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.i("location", "status changed")
        }

        override fun onProviderEnabled(provider: String?) {
            Log.i("location", "provider enabled")
        }

        override fun onProviderDisabled(provider: String?) {
            Log.i("location", "provider disabled")
        }
    }
}
