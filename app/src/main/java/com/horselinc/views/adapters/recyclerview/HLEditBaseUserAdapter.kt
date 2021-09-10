package com.horselinc.views.adapters.recyclerview

import android.app.AlertDialog
import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLBaseUserModel
import com.horselinc.views.listeners.HLEditBaseUserItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView


class HLEditBaseUserAdapter(context: Context?,
                            private val itemListener: HLEditBaseUserItemListener) : RecyclerArrayAdapter<HLBaseUserModel>(context) {
    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLEditBaseUserViewHolder (parent, R.layout.item_edit_base_user)
    }

    class HLEditBaseUserViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLBaseUserModel>(parent, res) {

        private val profileImageView: RoundedImageView = itemView.findViewById(R.id.profileImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val deleteImageView: ImageView = itemView.findViewById(R.id.deleteImageView)

        init {
            deleteImageView.setOnClickListener { onClickDelete () }
        }

        override fun setData(data: HLBaseUserModel?) {
            data?.let { baseUser ->
                // profile image
                profileImageView.loadImage(baseUser.avatarUrl, R.drawable.ic_profile)

                // name
                nameTextView.text = baseUser.name
            }
        }

        private fun onClickDelete () {
            getOwnerAdapter<HLEditBaseUserAdapter>()?.run {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.app_name))
                    .setMessage(R.string.msg_alert_delete_base_user)
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