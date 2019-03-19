package com.rohsins.icetea

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class Lights : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lights)

        val linearLayout: LinearLayout = findViewById(R.id.lightsElement)
        val seekBar: SeekBar = findViewById(R.id.lightsSeekBar)
        val textView: TextView = findViewById(R.id.lightsTextView)
    }
}
