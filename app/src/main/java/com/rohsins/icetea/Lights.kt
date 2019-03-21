package com.rohsins.icetea

import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.graphics.ColorUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_lights.*
import java.util.zip.Inflater

class Lights : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_lights)

        val linearLayout = LinearLayout(this)

        setContentView(linearLayout)

        val linearLayoutElement = LinearLayout(this)
        val layoutParamsElement = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 10)
        layoutParamsElement.setMargins(10, 10, 10, 10)
        linearLayoutElement.layoutParams = layoutParamsElement
        linearLayoutElement.orientation = LinearLayout.VERTICAL
        linearLayoutElement.background = getDrawable(R.drawable.light_view)

        val linearLayoutSection1 = LinearLayout(this)
        val layoutParamsSection1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 10)
        layoutParamsSection1.setMargins(10, 10, 10, 10)
        linearLayoutSection1.layoutParams = layoutParamsSection1
        linearLayoutSection1.orientation = LinearLayout.HORIZONTAL

        val imageView = ImageView(this)
        val imageViewLayoutParams = LinearLayout.LayoutParams(40, 40)
//        imageViewLayoutParams.weight = 1F
        imageView.layoutParams = imageViewLayoutParams
        imageView.background = getDrawable(R.drawable.lights)

        val textView = TextView(this)
        val textViewLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        textViewLayoutParams.weight = 979F
        textViewLayoutParams.marginStart = 10
        textView.gravity = Gravity.CENTER
        textView.textSize = 20F
        textView.setText("Street Light")
        textView.layoutParams = textViewLayoutParams

        val switch = Switch(this)
        val switchLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 40)
//        switchLayoutParams.weight = 20F
        switch.layoutParams = switchLayoutParams

        linearLayoutSection1.addView(imageView)
        linearLayoutSection1.addView(textView)
        linearLayoutSection1.addView(switch)

        val seekBar = SeekBar(this)
        val seekBarLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            80
        )
        seekBarLayoutParams.topMargin = 400
        seekBar.layoutParams = seekBarLayoutParams
        seekBar.max = 100
        seekBar.splitTrack = false
        seekBar.background = getDrawable(R.drawable.light_seekbar_background)
        seekBar.thumb = getDrawable(R.drawable.light_thumb)
        seekBar.progressDrawable = getDrawable(R.drawable.none)

        linearLayoutElement.addView(linearLayoutSection1)
        linearLayoutElement.addView(seekBar)

        linearLayout.removeAllViews()
        linearLayout.addView(linearLayoutElement)

//        val linearLayout: LinearLayout = findViewById(R.id.lightsElement)
//        val seekBar: SeekBar = findViewById(R.id.lightsSeekBar)
//        val textView: TextView = findViewById(R.id.lightsTextView)
//        var tProgress: Float
//
//        linearLayout.background.setTint(Color.parseColor("#54C1E2"))
//        textView.setText("Street Light")
        seekBar.background.setTint(ColorUtils.blendARGB(Color.parseColor("#54C1E2"), Color.parseColor("#FFFFFF"), 0.toFloat()))
//
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
