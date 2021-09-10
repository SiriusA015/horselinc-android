package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseModel
import com.horselinc.views.listeners.HLEditHorseItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView


class HLEditHorseAdapter(context: Context?,
                         private val itemListener: HLEditHorseItemListener) : RecyclerArrayAdapter<HLHorseModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLEditHorseViewHolder (parent, R.layout.item_edit_horse)
    }

    class HLEditHorseViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLHorseModel>(parent, res) {

        private var horseImageView: RoundedImageView = itemView.findViewById(R.id.horseImageView)
        private var barnNameTextView: TextView = itemView.findViewById(R.id.barnNameTextView)
        private var displayNameTextView: TextView = itemView.findViewById(R.id.displayNameTextView)
        private var deleteImageView: ImageView = itemView.findViewById(R.id.deleteImageView)

        init {
            deleteImageView.setOnClickListener { onClickDelete () }
        }

        @SuppressLint("SetTextI18n")
        override fun setData(data: HLHorseModel?) {
            data?.let { horse ->
                // horse image
                horseImageView.loadImage(horse.avatarUrl, R.drawable.ic_default_horse)

                // barn name
                barnNameTextView.text = horse.barnName

                // display name
                displayNameTextView.text = "\"${horse.displayName}\""
            }
        }

        private fun onClickDelete () {
            getOwnerAdapter<HLEditHorseAdapter>()?.run {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.app_name))
                    .setMessage(R.string.msg_alert_delete_horse)
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton("Yes") { dialog, _ ->
                        itemListener.onClickDelete(dataPosition, allData[dataPosition])
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }
}