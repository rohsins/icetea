package com.rohsins.icetea

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.v4.graphics.ColorUtils
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.*

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

        constructor(lightName: String, lightColor: String = "#59C1D2") {

            layoutParamsElement.setMargins(dp(10), dp(10), dp(10), dp(10))
            linearLayoutElement.layoutParams = layoutParamsElement
            linearLayoutElement.orientation = LinearLayout.VERTICAL
            linearLayoutElement.background = getDrawable(R.drawable.light_view)
            linearLayoutElement.background.setTint(Color.parseColor(lightColor))

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
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.parseColor(lightColor),
                    ColorUtils.blendARGB(
                        Color.parseColor(lightColor),
                        Color.parseColor("#FFFFFF"),
                        0.3.toFloat()
                    )
                )
            )
            gradientDrawable.cornerRadius = dp(6).toFloat()
            seekBar.background = gradientDrawable

            linearLayoutElement.addView(linearLayoutSection1)
            linearLayoutElement.addView(seekBar)
        }

        fun getLayout(): LinearLayout {
            return linearLayoutElement
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_lights)

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
        linearLayout.removeAllViews()
        linearLayout.addView(firstElement.getLayout())
        linearLayout.addView(secondElement.getLayout())

//        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
//            @RequiresApi(Build.VERSION_CODES.O)
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                tProgress = (progress.toFloat()/100)
//                seekBar?.background?.setTint(ColorUtils.blendARGB(Color.parseColor("#54C1E2"), Color.parseColor("#FFFFFF"), tProgress))
//                Toast.makeText(this@Lights, progress.toString(), Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
////                seekBar?.setBackgroundColor(Color.parseColor("#00ff00"))
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
////                seekBar?.setBackgroundColor(Color.parseColor("#0000ff"))
//            }
//
//        })
    }
}
