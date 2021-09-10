package com.horselinc.views.adapters.recyclerview

import android.app.AlertDialog
import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLNotificationModel
import com.horselinc.views.listeners.HLNotificationItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.loopeer.itemtouchhelperextension.Extension
import com.makeramen.roundedimageview.RoundedImageView
import java.util.*


class HLNotificationAdapter(context: Context?,
                            private val itemListener: HLNotificationItemListener
) : RecyclerArrayAdapter<HLNotificationModel>(context) {
    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLNotificationViewHolder (parent, R.layout.item_notification)
    }

    class HLNotificationViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLNotificationModel>(parent, res), Extension {

        private val itemCardView: CardView = itemView.findViewById(R.id.itemCardView)
        private val profileImageView: RoundedImageView = itemView.findViewById(R.id.profileImageView)
        private val notificationTextView: TextView = itemView.findViewById(R.id.notificationTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)

        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        init {
            deleteButton.setOnClickListener { onClickDelete() }
        }

        override fun setData(data: HLNotificationModel?) {
            data?.let { notification ->
                // profile image
                profileImageView.loadImage(notification.creator.avatarUrl, R.drawable.ic_profile)

                // notification
                notificationTextView.text = notification.message

                // time
                timeTextView.text = DateUtils.getRelativeTimeSpanString(notification.createdAt,
                    Calendar.getInstance().timeInMillis, DateUtils.MINUTE_IN_MILLIS)
            }
        }

        override fun getActionWidth(): Float = actionContainer.width.toFloat()


        private fun onClickDelete () {
            getOwnerAdapter<HLNotificationAdapter>()?.run {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.app_name))
                    .setMessage(R.string.msg_alert_delete_notification)
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton("Yes") { dialog, _ ->
                        itemListener.onClickDelete(dataPosition, allData[dataPosition])
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        /**
         *  Public
         */
        val itemContainer: View
            get() {
                return itemCardView
            }

        val actionContainer: View
            get() {
                return deleteButton
            }

    }
}