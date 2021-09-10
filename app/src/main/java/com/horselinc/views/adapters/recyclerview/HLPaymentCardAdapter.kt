package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import com.horselinc.R
import com.horselinc.models.data.HLStripeCardModel
import com.horselinc.views.activities.HLUserRoleActivity
import com.horselinc.views.listeners.HLPaymentMethodItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.stripe.android.model.Card


class HLPaymentCardAdapter(context: Context?,
                           var selectedIndex: Int = 0,
                           val paymentItemListener: HLPaymentMethodItemListener) : RecyclerArrayAdapter<HLStripeCardModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        val resId = if (context is HLUserRoleActivity) {
            R.layout.item_auth_payment_card
        } else {
            R.layout.item_edit_payment_card
        }
        return HLPaymentCardViewHolder (parent, resId)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {
        super.OnBindViewHolder(holder, position)

        (holder as HLPaymentCardViewHolder).apply {
            if (position == selectedIndex) {
                radioButton.isChecked = true
                deleteButton.visibility = View.INVISIBLE
            } else {
                radioButton.isChecked = false
                deleteButton.visibility = View.VISIBLE
            }
        }
    }

    class HLPaymentCardViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLStripeCardModel>(parent, res) {

        private var paymentImageView: ImageView = itemView.findViewById(R.id.paymentImageView)
        private var paymentTextView: TextView = itemView.findViewById(R.id.paymentTextView)
        var deleteButton: ImageView = itemView.findViewById(R.id.deleteImageView)
        var radioButton: RadioButton = itemView.findViewById(R.id.radioButton)

        init {
            radioButton.setOnClickListener { onClickRadio() }
            deleteButton.setOnClickListener { onClickDelete () }
        }

        @SuppressLint("SetTextI18n")
        override fun setData(data: HLStripeCardModel?) {
            data?.let {
                paymentImageView.setImageResource(Card.getBrandIcon(it.brand))
                paymentTextView.text = "${it.brand} ${it.last4}"
            }
        }

        private fun onClickRadio () {
            getOwnerAdapter<HLPaymentCardAdapter>()?.run {
                if (selectedIndex != dataPosition) {
                    radioButton.isChecked = false

                    AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.app_name))
                        .setMessage(R.string.msg_alert_update_default_payment)
                        .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                        .setPositiveButton("Yes") { dialog, _ ->
                            selectedIndex = dataPosition
                            notifyItemRangeChanged(0, allData.size)
                            paymentItemListener.onClickRadio(selectedIndex)
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

        private fun onClickDelete () {
            getOwnerAdapter<HLPaymentCardAdapter>()?.run {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.app_name))
                    .setMessage(R.string.msg_alert_delete_payment)
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton("Yes") { dialog, _ ->
                        paymentItemListener.onClickDelete(dataPosition)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }
}