package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.data.HLHorseOwnerModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView


class HLHorseUserAdapter(context: Context?) : RecyclerArrayAdapter<HLHorseManagerModel>(context) {
    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLHorseUserViewHolder (parent, R.layout.item_horse_user)
    }

    class HLHorseUserViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLHorseManagerModel>(parent, res) {

        private val userTypeTextView: TextView = itemView.findViewById(R.id.userTypeTextView)
        private val userProfileImageView: RoundedImageView = itemView.findViewById(R.id.userProfileImageView)
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val userInfoTextView: TextView = itemView.findViewById(R.id.userInfoTextView)

        override fun setData(data: HLHorseManagerModel?) {
            data?.let { horseUser ->
                // user type
                userTypeTextView.text = when {
                    horseUser is HLHorseOwnerModel -> "Owner"
                    dataPosition == 0 -> "Trainer"
                    else -> "Leased To"
                }

                // user profile
                userProfileImageView.loadImage(horseUser.avatarUrl, R.drawable.ic_profile)

                // user name
                usernameTextView.text = horseUser.name

                // user information
                userInfoTextView.text = if (horseUser is HLHorseOwnerModel) {
                    "(Owns ${String.format("%.02f", horseUser.percentage)}%)"
                } else {
                    if (horseUser.barnName.isEmpty()) {
                        ""
                    } else {
                        "(${horseUser.barnName})"
                    }
                }
            }
        }
    }
}