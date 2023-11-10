package com.tricktrack.tricktrack

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView

class CustomArrayAdapterSingleChoice(
    context: Context,
    resource: Int,
    objects: Array<String>,
) : ArrayAdapter<String>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as CheckedTextView
        view.setTextColor(context.getColor(R.color.lighter_grey)) // Set text color based on position
        return view
    }
}
