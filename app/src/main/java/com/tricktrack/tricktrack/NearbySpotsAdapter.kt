package com.tricktrack.tricktrack

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class NearbySpotsAdapter(private val onItemClick: (NearbySpot) -> Unit) : RecyclerView.Adapter<NearbySpotsAdapter.ViewHolder>() {

    private val items: MutableList<NearbySpot> = mutableListOf()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val spotTypeImage: ImageView = itemView.findViewById(R.id.spot_type_image)
        val spotTitle: TextView = itemView.findViewById(R.id.spot_title)
        val spotDistance: TextView = itemView.findViewById(R.id.spot_distance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.nearby_spots_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val typeTexts = arrayOf(
            "Открытый скейтпарк",
            "Крытый скейтпарк",
            "Стрит",
            "D.I.Y",
            "Шоп",
            "Dirt"
        )
        val typeImages = arrayOf(
            R.drawable.park_spot_mark,
            R.drawable.covered_park_mark,
            R.drawable.street_spot_mark,
            R.drawable.diy_spot_mark,
            R.drawable.shop_mark,
            R.drawable.dirt_spot_mark
        )

        var correctImageId = R.drawable.not_chosen_mark

        for (i in typeTexts.indices) {
            if (item.type == typeTexts[i]) {
                correctImageId = typeImages[i]
            }
        }

        holder.spotTypeImage.setImageResource(correctImageId)
        holder.spotTitle.text = item.title
        val locale = Locale.US // You can choose a specific locale herez
// Use the specified locale to format the number
        val roundedValue = String.format(locale, "%.2f", item.distance).toFloat()
        holder.spotDistance.text = roundedValue.toString()

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setData(data: List<NearbySpot>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }
}
