package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseManagerModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView


class HLProviderHorseDetailAdapter(context: Context?) : RecyclerArrayAdapter<HLHorseManagerModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLProviderHorseDetailViewHolder (parent, R.layout.item_provider_horse_detail)
    }

    class HLProviderHorseDetailViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLHorseManagerModel>(parent, res) {

        private val userTypeTextView: TextView = itemView.findViewById(R.id.userTypeTextView)
        private val userImageView: RoundedImageView = itemView.findViewById(R.id.userImageView)
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val barnNameTextView: TextView = itemView.findViewById(R.id.barnNameTextView)

        @SuppressLint("SetTextI18n")
        override fun setData(data: HLHorseManagerModel?) {

            userTypeTextView.text = data?.userType
            userImageView.loadImage(data?.avatarUrl, R.drawable.ic_profile)
            usernameTextView.text = data?.name

            barnNameTextView.text = ""
            data?.barnName?.let {
                if (it.isNotEmpty()) {
                    barnNameTextView.text = "(${it})"
                }
            }
        }
    }
}