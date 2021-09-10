package com.horselinc.views.adapters.recyclerview

import android.app.AlertDialog
import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseManagerPaymentApproverModel
import com.horselinc.views.activities.HLUserRoleActivity
import com.horselinc.views.listeners.HLPaymentApproverItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView
import me.abhinay.input.CurrencySymbols


class HLPaymentApproverAdapter(context: Context?,
                               val itemListener: HLPaymentApproverItemListener) : RecyclerArrayAdapter<HLHorseManagerPaymentApproverModel>(context) {
    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        val resId = if (context is HLUserRoleActivity) {
            R.layout.item_auth_payment_approver
        } else {
            R.layout.item_edit_payment_approver
        }
        return HLPaymentApproverViewHolder (parent, resId)
    }

    class HLPaymentApproverViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLHorseManagerPaymentApproverModel>(parent, res) {

        private val profileImageView: RoundedImageView = itemView.findViewById(R.id.profileImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        private val editButton: ImageView = itemView.findViewById(R.id.editImageView)
        private val deleteButton: ImageView = itemView.findViewById(R.id.clearImageView)

        init {
            editButton.setOnClickListener { onClickEdit () }
            deleteButton.setOnClickListener { onClickDelete () }
        }

        override fun setData(data: HLHorseManagerPaymentApproverModel?) {
            data?.let {
                // profile image
                profileImageView.loadImage(it.avatarUrl, R.drawable.ic_profile)

                // name
                nameTextView.text = it.name

                // amount
                amountTextView.text = if (it.amount == null) "(Unlimited)" else "(Up to ${CurrencySymbols.USA}${it.amount.toString()})"
            }
        }

        private fun onClickEdit () {
            getOwnerAdapter<HLPaymentApproverAdapter>()?.run {
                itemListener.onClickEdit(dataPosition)
            }
        }

        private fun onClickDelete () {
            getOwnerAdapter<HLPaymentApproverAdapter>()?.run {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.app_name))
                    .setMessage(R.string.msg_alert_clear_payment_approver)
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton("Yes") { dialog, _ ->
                        itemListener.onClickDelete(dataPosition)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }
}