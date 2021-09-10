package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLBaseUserModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView


class HLBaseUserAdapter(context: Context?) : RecyclerArrayAdapter<HLBaseUserModel>(context) {
    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLBaseUserViewHolder (parent, R.layout.item_base_user)
    }

    class HLBaseUserViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLBaseUserModel>(parent, res) {

        private val profileImageView: RoundedImageView = itemView.findViewById(R.id.profileImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)

        override fun setData(data: HLBaseUserModel?) {
            data?.let { baseUser ->
                // profile image
                profileImageView.loadImage(baseUser.avatarUrl, R.drawable.ic_profile)

                // name
                nameTextView.text = baseUser.name
            }
        }
    }
}