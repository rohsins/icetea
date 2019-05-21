package com.rohsins.icetea

import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.graphics.ColorUtils
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.*
import com.rarepebble.colorpicker.ColorPickerView
import com.rohsins.icetea.DataModel.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONObject

val lightsViewUpdateLock = Object()

class Lights : AppCompatActivity() {

    private lateinit var lightDao: LightDao

    private fun dp(dp: Int): Int {
        val density = resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    private lateinit var linearLayout: LinearLayout
    private lateinit var scrollView: ScrollView

    private val renderHandler = Handler()

    private val renderRunnable = Runnable {
        linearLayout.removeAllViews()
        scrollView.removeAllViews()

        lightDao.getAllLight().forEach {
            linearLayout.addView(LightElement(it.id, it.alias, it.isChecked, it.intensity, it.color).getLayout())
        }

        scrollView.addView(linearLayout)
    }

    inner class LightElement {
        private var id: String
        private var lightName: String
        private var isChecked: Boolean
        private var intensity: Int
        private var lightColor: String

        private val linearLayoutElement = LinearLayout(this@Lights)
        private val layoutParamsElement = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(100))
        private val linearLayoutSection1 = LinearLayout(this@Lights)
        private val layoutParamsSection1 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        private val imageView = ImageView(this@Lights)
        private val imageViewLayoutParams = LinearLayout.LayoutParams(dp(40), dp(40), 1f)
        private val textView = TextView(this@Lights)
        private val textViewLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            979f
        )
        private val switch = Switch(this@Lights)
        private val switchLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40), 20f)
        private val seekBar = SeekBar(this@Lights)
        private val seekBarLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(20)
        )
        private var previousColor: String

        constructor(id: String, lightName: String, isChecked: Boolean, intensity: Int, lightColor: String = "#59C1D2") {
            this.id = id
            this.lightName = lightName
            this.isChecked = isChecked
            this.intensity = intensity
            this.lightColor = lightColor

            previousColor = lightColor

            layoutParamsElement.setMargins(dp(10), dp(10), dp(10), dp(10))
            linearLayoutElement.layoutParams = layoutParamsElement
            linearLayoutElement.orientation = LinearLayout.VERTICAL
            linearLayoutElement.background = getDrawable(R.drawable.light_view)

            layoutParamsSection1.setMargins(dp(10), dp(10), dp(10), dp(10))
            linearLayoutSection1.layoutParams = layoutParamsSection1
            linearLayoutSection1.orientation = LinearLayout.HORIZONTAL

            imageView.layoutParams = imageViewLayoutParams
            imageView.background = getDrawable(R.drawable.lights)

            textViewLayoutParams.marginStart = dp(10)
            textView.gravity = Gravity.CENTER_VERTICAL
            textView.textSize = 20f
            textView.text = lightName
            textView.layoutParams = textViewLayoutParams
            textView.setTextColor(Color.parseColor("#000000"))

            switch.layoutParams = switchLayoutParams

            linearLayoutSection1.addView(imageView)
            linearLayoutSection1.addView(textView)
            linearLayoutSection1.addView(switch)

            seekBarLayoutParams.topMargin = dp(20)
            seekBar.layoutParams = seekBarLayoutParams
            seekBar.max = 255
            seekBar.progress = intensity
            seekBar.splitTrack = false
            seekBar.thumb = getDrawable(R.drawable.light_thumb)
            seekBar.progressDrawable = getDrawable(R.drawable.none)
            changeColor(lightColor)

            linearLayoutElement.addView(linearLayoutSection1)
            linearLayoutElement.addView(seekBar)

            switch.isChecked = isChecked

            if (!isChecked) disableLight()

            setSeekOnChangeListener()
            setSwitchOnChangeListener()
            setOnLongPress()
        }

        fun getLayout(): LinearLayout {
            return linearLayoutElement
        }

        private fun changeColor(color: String) {
            if (color.length > 6) {
                linearLayoutElement.background.setTint(Color.parseColor(color))
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        Color.parseColor(color),
                        ColorUtils.blendARGB(
                            Color.parseColor(color),
                            Color.parseColor("#FFFFFF"),
                            0.4.toFloat()
                        )
                    )
                )
                gradientDrawable.cornerRadius = dp(6).toFloat()
                seekBar.background = gradientDrawable
            }
        }

        private fun disableLight() {
            textView.setTextColor(Color.parseColor("#FFFFFF"))
            changeColor("#505050")
            linearLayoutElement.removeView(seekBar)
            linearLayoutElement.layoutParams.height = dp(80)
        }

        private fun enableLight() {
            textView.setTextColor(Color.parseColor("#000000"))
            changeColor(previousColor)
            linearLayoutElement.addView(seekBar)
            linearLayoutElement.layoutParams.height = dp(100)
        }

        private fun setSeekOnChangeListener() {
            seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    this@LightElement.intensity = seekBar!!.progress
                    lightSend(1)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                    this@LightElement.intensity = seekBar!!.progress
//                    Toast.makeText(this@Lights, seekBar?.progress.toString(), Toast.LENGTH_SHORT).show()
//                    lightDao.updateLight(Light(this@LightElement.id, this@LightElement.lightName, this@LightElement.isChecked, this@LightElement.intensity, this@LightElement.lightColor))
                    lightSend(2)
                }
            })
        }

        private fun setSwitchOnChangeListener() {
            switch.setOnCheckedChangeListener { buttonView, isChecked ->
                this.isChecked = isChecked
                if (!isChecked) {
                    disableLight()
                } else {
                    enableLight()
                }
//                lightDao.updateLight(Light(this.id, this.lightName, this.isChecked, this.intensity, this.lightColor))
                lightSend(2)
            }
        }

        private fun setOnLongPress() {
            linearLayoutSection1.setOnLongClickListener {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                if (switch.isChecked) {
                    val colorPickerView = ColorPickerView(this@Lights)
                    colorPickerView.color = Color.parseColor(previousColor)
                    colorPickerView.showHex(false)
                    colorPickerView.showAlpha(false)
                    colorPickerView.showPreview(true)
                    colorPickerView.addColorObserver {
                        val previewColor = '#' + it.color.toUInt().toString(16)
                        if (this.lightColor != previewColor) {
                            this.lightColor = previewColor
                            lightSend(1)
                        }
                    }

                    val alertDialogBuilder = AlertDialog.Builder(this@Lights)
                    alertDialogBuilder.setPositiveButton("Apply") { dialog, which ->
                        previousColor = '#' + colorPickerView.color.toUInt().toString(16)
                        this.lightColor = previousColor
                        changeColor(previousColor)
//                        lightDao.updateLight(Light(this.id, this.lightName, this.isChecked, this.intensity, this.lightColor))
                        lightSend(2)
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                    }
                    alertDialogBuilder.setNegativeButton("Cancel") { dialog, which ->
                        this.lightColor = previousColor
                        lightSend(1)
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                    }
                    alertDialogBuilder.setOnDismissListener {
                        this.lightColor = previousColor
                        lightSend(1)
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                    }
                    alertDialogBuilder.setOnCancelListener {
                        this.lightColor = previousColor
                        lightSend(1)
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                    }
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.setView(colorPickerView)
                    alertDialog.show()
                }
                true
            }
        }

        private fun lightSend(qos: Int) {
            val payload = JSONObject()
            payload.put("thingCode", thingCode)
            // payload.put("alias", this.lightName)
            payload.put("isChecked", this.isChecked)
            payload.put("intensity", this.intensity)
            payload.put("color", this.lightColor)
            val essential = JSONObject()
            essential.put("publisherudi", udi)
            val targetSubscriber = JSONArray()
            targetSubscriber.put(this.id)
            val mqttPacket = JSONObject()
            mqttPacket.put("qos", qos)
            essential.put("targetSubscriber", targetSubscriber)
            if (qos == 2) {
                essential.put("payloadType", "command")
            } else {
                essential.put("payloadType", "preview")
            }
            essential.put("mqttPacket", mqttPacket)
            essential.put("payload", payload)
            val packedJson = JSONObject()
            packedJson.put("essential", essential)
            connectivity.mqttPublish(packedJson.toString().toByteArray(), qos)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lightDao = LightDatabase.getInstance(this@Lights).lightDao()

        linearLayout = LinearLayout(this)
        scrollView = ScrollView(this)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        linearLayout.layoutParams = layoutParams
        linearLayout.orientation = LinearLayout.VERTICAL

        scrollView.addView(linearLayout)
        setContentView(scrollView)

        renderHandler.post(renderRunnable)

        if (!MainActivity.serviceRunning) {
            connectivity.configureAndConnectMqtt(applicationContext)
        }

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!MainActivity.serviceRunning) {
            connectivity.unconfigureAndDisconnectMqttForcibly()
        }
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

//    override fun onPause() {
//        super.onPause()
//        if (EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().unregister(this)
//        }
//    }

    override fun onResume() {
        super.onResume()
        renderHandler.post(renderRunnable)
//        if (!EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().register(this)
//        }
    }

//    override fun onStop() {
//        super.onStop()
//        if (EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().unregister(this)
//        }
//    }

//    override fun onStart() {
//        super.onStart()
//        if (!EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().register(this)
//        }
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: MessageEvent) {
        if (!MainActivity.serviceRunning) Thread(LightRoutine(event, this)).start()
        synchronized(lightsViewUpdateLock) {
            lightsViewUpdateLock.wait()
            renderHandler.post(renderRunnable)
        }
    }
}

class LightRoutine: Runnable {
    var event: MessageEvent
    var context: Context

    constructor(eventArg: MessageEvent, contextArg: Context) {
        event = eventArg
        context = contextArg
    }
    override fun run() {
        try {
            val jsonFile = JSONObject(event.mqttMessage.toString())
            val subscriberudi = jsonFile.getString("subscriberudi")
            val payloadType = jsonFile.getString("payloadType")
            val payload = jsonFile.getJSONObject("payload")
            if (payloadType!!.contentEquals("commandReply")
                && payload.getInt("thingCode") == 13001
                && !payload.getString("color").isEmpty()) {
                val lightDao = LightDatabase.getInstance(context).lightDao()
                lightDao.updateLight(
                    Light(
                        subscriberudi,
                        payload.getString("alias"),
                        payload.getBoolean("isChecked"),
                        payload.getInt("intensity"),
                        payload.getString("color")
                    )
                )
            } else if (payloadType!!.contentEquals("response")
                && payload.getInt("thingCode") == 13001
                && !payload.getString("color").isEmpty()) {
                val lightDao = LightDatabase.getInstance(context).lightDao()
                lightDao.updateLight(
                    Light(
                        subscriberudi,
                        payload.getString("alias"),
                        payload.getBoolean("isChecked"),
                        payload.getInt("intensity"),
                        payload.getString("color")
                    )
                )
            }
            val typeCheckPub = payload.getString("pubType")!!.contentEquals("lightSwitch")
            val typeCheckSub = payload.getString("subType")!!.contentEquals("lightSwitch")
            if (payloadType.contentEquals("appSync")
                && (typeCheckPub || typeCheckSub)
                && (payload.getString("pubUDI")!!.contentEquals(udi)
                        || payload.getString("subUDI")!!.contentEquals(udi))) {
                if (payload.getString("activity")!!.contentEquals("link")) {
                    val lightDao = LightDatabase.getInstance(context).lightDao()
                    lightDao.insertLight(
                        Light(
                            if (typeCheckPub) payload.getString("pubUDI") else payload.getString("subUDI"),
                            if (typeCheckPub) payload.getString("pubAlias") else payload.getString("subAlias"),
                            false,
                            10,
                            "#ff4aa352"
                        )
                    )

                    val packedJson = JSONObject()
                    val essential = JSONObject()
                    val targetSubscriber = JSONArray()
                    val requestPayload = JSONObject()
                    requestPayload.put("thingCode", thingCode)
                    requestPayload.put("state", true)
                    targetSubscriber.put(payload.getString("subUDI"))
                    essential.put("publisherudi", udi)
                    essential.put("targetSubscriber", targetSubscriber)
                    essential.put("payloadType", "request")
                    essential.put("payload", requestPayload)
                    packedJson.put("essential", essential)
                    connectivity.mqttPublish(packedJson.toString().toByteArray(), 2)
                } else if (payload.getString("activity")!!.contentEquals("unlink")) {
                    val lightDao = LightDatabase.getInstance(context).lightDao()
                    if (typeCheckPub) lightDao.deleteLight(payload.getString("pubUDI")) else lightDao.deleteLight(payload.getString("subUDI"))
                }
            } else if (payloadType.contentEquals("appSync")
                && (payload.getString("pubUDI")!!.contentEquals(udi)
                        || payload.getString("subUDI")!!.contentEquals(udi))) {
                val deviceDao = DeviceDatabase.getInstance(context).deviceDao()
                val deviceCheck = payload.getString("subUDI").contentEquals(udi)
                if (payload.getString("activity")!!.contentEquals("link")) {
                    deviceDao.insertDevice(
                        Device(
                            0,
                            if (deviceCheck) payload.getString("pubUDI") else payload.getString("subUDI"),
                            if (deviceCheck) payload.getString("pubAlias") else payload.getString("subAlias"),
                            if (deviceCheck) payload.getString("pubType") else payload.getString("subType")
                        )
                    )
                } else if (payload.getString("activity")!!.contentEquals("unlink")) {
                    deviceDao.deleteDevice(if (deviceCheck) payload.getString("pubUDI") else payload.getString("subUDI"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        synchronized(lightsViewUpdateLock) {
            lightsViewUpdateLock.notify()
        }
    }
}
