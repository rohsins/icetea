package com.rohsins.icetea

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.v4.graphics.ColorUtils
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import com.rarepebble.colorpicker.ColorPickerView
import com.rohsins.icetea.DataModel.Light
import com.rohsins.icetea.DataModel.LightDao
import com.rohsins.icetea.DataModel.LightDatabase

class Lights : AppCompatActivity() {

    private fun dp(dp: Int): Int {
        val density = resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    inner class LightElement {
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

        constructor(lightName: String, lightColor: String = "#59C1D2") {
            previousColor = lightColor

            layoutParamsElement.setMargins(dp(10), dp(10), dp(10), dp(10))
            linearLayoutElement.layoutParams = layoutParamsElement
            linearLayoutElement.orientation = LinearLayout.VERTICAL
            linearLayoutElement.background = getDrawable(R.drawable.light_view)
//            linearLayoutElement.background.setTint(Color.parseColor(lightColor))

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
            seekBar.max = 100
            seekBar.splitTrack = false
            seekBar.thumb = getDrawable(R.drawable.light_thumb)
            seekBar.progressDrawable = getDrawable(R.drawable.none)
            changeColor(lightColor)

            linearLayoutElement.addView(linearLayoutSection1)
            linearLayoutElement.addView(seekBar)

            switch.isChecked = true

            setSeekOnChangeListener()
            setSwitchOnChangeListener()
            setOnLongPress()
        }

        fun getLayout(): LinearLayout {
            return linearLayoutElement
        }

        fun changeColor(color: String) {
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
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    Toast.makeText(this@Lights, seekBar?.progress.toString(), Toast.LENGTH_SHORT).show()
                }
            })
        }

        private fun setSwitchOnChangeListener() {
            switch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (!isChecked) {
                    disableLight()
                } else {
                    enableLight()
                }
            }
        }

        private fun setOnLongPress() {
            linearLayoutSection1.setOnLongClickListener {
                if (switch.isChecked) {
                    val colorPickerView = ColorPickerView(this@Lights)
                    colorPickerView.color = Color.parseColor(previousColor)
                    colorPickerView.showHex(false)
                    colorPickerView.showAlpha(false)
                    colorPickerView.showPreview(true)

                    val alertDialogBuilder = AlertDialog.Builder(this@Lights)
                    alertDialogBuilder.setPositiveButton("OK") { dialog, which ->
                        previousColor = '#' + colorPickerView.color.toUInt().toString(16)
                        changeColor(previousColor)
                    }
                    alertDialogBuilder.setNegativeButton("Cancel") { dialog, which ->
                        Toast.makeText(this@Lights, "Cancel", Toast.LENGTH_SHORT).show()
                    }
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.setView(colorPickerView)
                    alertDialog.show()
                }
                true
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_lights)

        val lightDao = LightDatabase.getInstance(this@Lights).lightDao()

//        var light = Light(123, "hello", 3, 93)
//        light.lightAlias = "something"
//        lightDao.insertLight(light)

        var rlight = lightDao.getLightById()
        Log.i("VTAG", rlight.get(0).lightAlias);

        val linearLayout = LinearLayout(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
//        layoutParams.setMargins(dp(10), dp(10), dp(10), dp(10))
        linearLayout.layoutParams = layoutParams
        linearLayout.orientation = LinearLayout.VERTICAL

        setContentView(linearLayout)

        val firstElement = LightElement("Living Room", "#329582")
        val secondElement = LightElement("Bed Room", "#427431")
        val thirdElement = LightElement("Bath Room", "#824491")

        linearLayout.removeAllViews()
        linearLayout.addView(firstElement.getLayout())
        linearLayout.addView(secondElement.getLayout())
        linearLayout.addView(thirdElement.getLayout())


//        rlight.forEach {
//            Light
//        }
    }
}
