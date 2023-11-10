package com.tricktrack.tricktrack

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*

class FavoriteSpotsAdapter(val context: Context, val showTranslation: Boolean, val nightRideMode: Boolean, private val onItemClick: (FavoriteSpot) -> Unit) : RecyclerView.Adapter<FavoriteSpotsAdapter.ViewHolder>() {

    private val items: MutableList<FavoriteSpot> = mutableListOf()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val spotTypeImage: ImageView = itemView.findViewById(R.id.spot_type_image)
        val spotTitle: TextView = itemView.findViewById(R.id.spot_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.favorite_spots_item, parent, false)
        if (nightRideMode) {
            itemView.findViewById<CardView>(R.id.main_background_fav_item).setBackgroundResource(R.drawable.round_back_dark_lighter_20)
            itemView.findViewById<TextView>(R.id.spot_title).setTextColor(context.getColor(R.color.lighter_grey))
        }
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

        Log.d("itemType123", item.type)

        for (i in typeTexts.indices) {
            if (item.type == typeTexts[i]) {
                correctImageId = typeImages[i]
            }
        }

        holder.spotTypeImage.setImageResource(correctImageId)
        holder.spotTitle.text = item.title


        if (showTranslation) {
            var correctLanguageCode = ""
            for (c in item.title.toCharArray()) {
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN) {
                    correctLanguageCode = "en"
                    break
                }
            }
            if (correctLanguageCode.isEmpty()) {
                correctLanguageCode = ""
                for (c in item.title.toCharArray()) {
                    if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                        correctLanguageCode = "ru"
                        break
                    }
                }
            }

            if (!correctLanguageCode.isEmpty()) {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage("ru")
                    .setTargetLanguage(Locale.getDefault().language)
                    .build()

                val conditions = DownloadConditions.Builder().requireWifi().build()
                val translator = Translation.getClient(options)
                translator.downloadModelIfNeeded(conditions).addOnSuccessListener {
                    translator.translate(item.title).addOnSuccessListener {
                        holder.spotTitle.text = it
                    }
                }
            }
        }


        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setData(data: List<FavoriteSpot>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }
}
