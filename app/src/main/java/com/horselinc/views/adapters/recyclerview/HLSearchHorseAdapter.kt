package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView


class HLSearchHorseAdapter(context: Context?) : RecyclerArrayAdapter<HLHorseModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLSearchHorseViewHolder (parent, R.layout.item_search_horse)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {
        super.OnBindViewHolder(holder, position)

        (holder as HLSearchHorseViewHolder).apply {
            itemView.isClickable = true
        }
    }

    class HLSearchHorseViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLHorseModel>(parent, res) {

        private var ivHorseAvatar: RoundedImageView = itemView.findViewById(R.id.ivHorseAvatar)
        private var tvHorseName: TextView = itemView.findViewById(R.id.tvHorseName)
        private var tvHorseInfo: TextView = itemView.findViewById(R.id.tvHorseInfo)

        @SuppressLint("SetTextI18n")
        override fun setData(data: HLHorseModel?) {
            data?.let { horse ->
                ivHorseAvatar.loadImage(horse.avatarUrl, R.drawable.ic_default_horse)
                tvHorseName.text = horse.displayName
                tvHorseInfo.text = horse.barnName
            }
        }
    }
}