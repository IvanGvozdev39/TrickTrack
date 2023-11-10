package com.tricktrack.tricktrack

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class SpotConditionAdapter(internal var context: Context, val nightRideMode: Boolean, internal var texts: Array<String>) :
    BaseAdapter() {
    internal var inflater: LayoutInflater

    init {
        inflater = LayoutInflater.from(context)
    }

    override fun getCount(): Int {
        return texts.size
    }

    override fun getItem(i: Int): Any? {
        return null
    }

    override fun getItemId(i: Int): Long {
        return 0
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {

        val view = inflater.inflate(R.layout.spinner_condition_item,null)
        val names = view.findViewById<View>(R.id.textView) as TextView?
        names!!.text = texts[i]
        if (nightRideMode) {
            names.setTextColor(context.getColor(R.color.lighter_grey))
            view.setBackgroundColor(context.getColor(R.color.dark_theme))
        }
        return view
    }
}