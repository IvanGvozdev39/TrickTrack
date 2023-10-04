package com.tricktrack.tricktrack

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ViewPagerAdapter(var images: List<Bitmap>) :
    RecyclerView.Adapter<ViewPagerAdapter.Pager2ViewHolder>() {

    inner class Pager2ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageIV = itemView.findViewById<ImageView>(R.id.image)

        init {
            imageIV.setOnClickListener { v: View ->
                val position: Int = adapterPosition
                //TODO: ...
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Pager2ViewHolder {
        return Pager2ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_page, parent, false))
    }

    override fun onBindViewHolder(holder: Pager2ViewHolder, position: Int) {
        holder.imageIV.setImageBitmap(images[position])
    }

    override fun getItemCount(): Int {
        return images.size
    }

}