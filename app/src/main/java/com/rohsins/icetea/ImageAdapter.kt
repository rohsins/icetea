package com.rohsins.icetea

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

private val mThumbIds = arrayOf<Int>(
    R.drawable.lights, R.drawable.doors,
    R.drawable.control, R.drawable.terminal,
    R.drawable.cloud2, R.drawable.guitar,
    R.drawable.logicboard, R.drawable.settings,
    R.drawable.cloud, R.drawable.settings2)

class ImageAdapter(private val mContext: Context): BaseAdapter() {

    override fun getCount(): Int = mThumbIds.size;

    override fun getItem(position: Int): Any? = null;

    override fun getItemId(position: Int): Long = 0L;

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView
        
        if (convertView == null) {
            imageView = ImageView(mContext)
            imageView.adjustViewBounds = true
            imageView.setBackgroundResource(R.drawable.button_style)
        } else {
            imageView = convertView as ImageView
        }

        imageView.setImageResource(mThumbIds[position])
        return imageView
    }
}