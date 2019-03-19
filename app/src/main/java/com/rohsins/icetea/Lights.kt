package com.rohsins.icetea

import android.graphics.Color
import android.graphics.ColorMatrix
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class Lights : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lights)

        val linearLayout: LinearLayout = findViewById(R.id.lightsElement)
        val seekBar: SeekBar = findViewById(R.id.lightsSeekBar)
        val textView: TextView = findViewById(R.id.lightsTextView)

        linearLayout.background.setTint(Color.parseColor("#54C1E2"))
        textView.setText("Street Light")
        seekBar.background.setTint(Color.parseColor("#ff0000"))

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Toast.makeText(this@Lights, progress.toString(), Toast.LENGTH_SHORT).show()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(this@Lights, Color.parseColor("#000000").toString(), Toast.LENGTH_SHORT).show()
//                seekBar?.setBackgroundColor(Color.parseColor("#00ff00"))
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                seekBar?.setBackgroundColor(Color.parseColor("#0000ff"))
            }

        })
    }
}
