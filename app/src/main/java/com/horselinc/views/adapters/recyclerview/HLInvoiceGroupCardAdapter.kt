package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.horselinc.views.listeners.HLInvoiceGroupItemListener
import com.horselinc.*
import com.horselinc.models.data.HLInvoiceModel
import com.horselinc.models.data.HLServiceRequestModel
import com.horselinc.utils.ResourceUtil
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import java.util.*


/** Created by jcooperation0137 on 2019-08-27.
 */
class HLInvoiceGroupCardAdapter(context: Context?, val itemListener: HLInvoiceGroupItemListener): RecyclerArrayAdapter<HLInvoiceModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLInvoiceGroupCardViewHolder(parent, R.layout.item_invoice_group)
    }

    override fun OnBindViewHolder(holder: BaseViewHolder<*>?, position: Int) {
        super.OnBindViewHolder(holder, position)

        (holder as? HLInvoiceGroupCardViewHolder)?.setBottomMargin(position == allData.size - 1)
    }

    class HLInvoiceGroupCardViewHolder(parent: ViewGroup?, res: Int): BaseViewHolder<HLInvoiceModel>(parent, res) {

        private var cardView: CardView = itemView.findViewById(R.id.cardView)
        private var updatedDateTextView: TextView = itemView.findViewById(R.id.updatedDateTextView)
        private var invoiceTotalTextView: TextView = itemView.findViewById(R.id.invoiceTotalTextView)
        private var payerNameTextView: TextView = itemView.findViewById(R.id.payerNameTextView)
        private var requestsLayout: LinearLayout = itemView.findViewById(R.id.servicesLayout)

        private var invoice: HLInvoiceModel? = null

        init {
            updatedDateTextView.text = ""
            invoiceTotalTextView.text = ""
            payerNameTextView.text = ""

            itemView.setOnClickListener { onClickInvoiceCard() }
        }

        @SuppressLint("SetTextI18n")
        override fun setData(data: HLInvoiceModel?) {
            invoice = data

            val updatedAt = invoice?.requests?.let { requests ->
                requests.maxBy { it.updatedAt }?.updatedAt
            }
            if (updatedAt != null) {
                updatedDateTextView.text = updatedAt.simpleDateString
            } else {
                updatedDateTextView.text = Date().time.simpleDateString
            }

            if (HLGlobalData.me?.type == HLUserType.PROVIDER) {
                payerNameTextView.visibility = if (invoice?.requests?.first()?.payer?.name.isNullOrEmpty()) View.GONE else View.VISIBLE
                payerNameTextView.text = invoice?.requests?.first()?.payer?.name
                val amount = (invoice?.amount?: 0.0) + (invoice?.tip?: 0.0)
                invoiceTotalTextView.text = "Invoice Total: $${String.format("%.2f", amount)}"
                invoiceTotalTextView.setTextColor(HLInvoiceStatusType.color(invoice?.status, context))

                val bgColor = if (invoice?.getHorseManager() == null) R.color.colorApproverPaymentCard else R.color.colorWhite
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, bgColor))

            } else {
                payerNameTextView.text = invoice?.requests?.first()?.serviceProvider?.name
                val amount = ((invoice?.amount?: 0.0) + (invoice?.tip?: 0.0)).withStripeFee
                invoiceTotalTextView.text = "Invoice Total: $${String.format("%.2f", amount)}"

                HLGlobalData.me?.horseManager?.userId?.let { userId ->
                    if (invoice?.isPayer(userId) == true) {
                        invoiceTotalTextView.setTextColor(HLInvoiceStatusType.color(invoice?.status, context))
                    } else {
                        invoiceTotalTextView.setTextColor(ContextCompat.getColor(context, R.color.colorMystic))
                    }
                }

                HLGlobalData.me?.horseManager?.let { manager ->
                    if (invoice?.isPaymentApprover(manager) == true) {
                        cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorApproverPaymentCard))
                    } else {
                        cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPayerPaymentCard))
                    }
                }
            }

            // Set service request info.
            requestsLayout.removeAllViews()
            invoice?.requests?.let { requests ->
                for (request in requests) {
                    val briefView = HLInvoiceBriefView(context)
                    briefView.setData(request)
                    requestsLayout.addView(briefView)
                }
            }
        }

        private fun onClickInvoiceCard() {
            getOwnerAdapter<HLInvoiceGroupCardAdapter>()?.run {
                invoice?.let {
                    itemListener.onClickInvoiceCard(it)
                }
            }
        }

        fun setBottomMargin(set: Boolean) {
            val params: LinearLayout.LayoutParams = cardView.layoutParams as LinearLayout.LayoutParams
            if (set) {
                params.bottomMargin = ResourceUtil.dpToPx(76)
            } else {
                params.bottomMargin = ResourceUtil.dpToPx(16)
            }
        }
    }

    class HLInvoiceBriefView(context: Context?): LinearLayout(context) {

        var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_invoice_brief, this)
        var avatarImageView: ImageView = containerView.findViewById(R.id.avatarImageView)
        var nameTextView: TextView = containerView.findViewById(R.id.nameTextView)
        var servicesSumTextView: TextView = containerView.findViewById(R.id.servicesSumTextView)
        var servicesCountTextView: TextView = containerView.findViewById(R.id.servicesCountTextView)
        var durationTextView: TextView = containerView.findViewById(R.id.durationTextView)


        @SuppressLint("SetTextI18n")
        fun setData(request: HLServiceRequestModel) {

            avatarImageView.loadImage(request.horse?.avatarUrl, R.drawable.ic_horse_placeholder)

            nameTextView.text = request.horse?.barnName
            val  count = request.services.count()
            if (count == 1) {
                servicesCountTextView.text = "1 service provided"
            } else {
                servicesCountTextView.text = "$count services provided"
            }

            var amount = request.services.map { it.rate * it.quantity }.sum().toDouble()
            if (HLGlobalData.me?.type == HLUserType.MANAGER) {
                amount = amount.withStripeFee
            }
            servicesSumTextView.text = String.format("$%.2f", amount)
            servicesSumTextView.setTextColor(ContextCompat.getColor(context, R.color.colorPrimaryText))

            if (request.createdAt.calendar.isSameDay(request.updatedAt.calendar)) {
                durationTextView.text = request.createdAt.simpleDateString
            } else {
                durationTextView.text = request.createdAt.simpleDateString + " - " + request.updatedAt.simpleDateString
            }
        }
    }

}