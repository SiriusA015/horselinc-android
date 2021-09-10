package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView


class HLHorseAdapter(context: Context?) : RecyclerArrayAdapter<HLHorseModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLHorseViewHolder (parent, R.layout.item_horse)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {
        super.OnBindViewHolder(holder, position)

        (holder as HLHorseViewHolder).apply {
            itemView.isClickable = false
        }
    }

    class HLHorseViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLHorseModel>(parent, res) {

        private var horseImageView: RoundedImageView = itemView.findViewById(R.id.horseImageView)
        private var barnNameTextView: TextView = itemView.findViewById(R.id.barnNameTextView)
        private var trainerTextView: TextView = itemView.findViewById(R.id.trainerTextView)

        init {
            itemView.findViewById<CardView>(R.id.containerCardView).setOnClickListener { itemView.callOnClick() }
        }

        @SuppressLint("SetTextI18n")
        override fun setData(data: HLHorseModel?) {
            data?.let { horse ->
                // horse image
                horseImageView.loadImage(horse.avatarUrl, R.drawable.ic_default_horse)

                // barn name
                barnNameTextView.text = horse.barnName

                // trainer name
                trainerTextView.text = "${horse.trainer?.name} (${horse.trainer?.barnName})"
            }
        }
    }
}