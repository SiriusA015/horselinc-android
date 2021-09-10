package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.horselinc.HLGlobalData
import com.horselinc.HLUserType
import com.horselinc.R
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.withStripeFee
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter


class HLProviderServiceAdapter(context: Context?) : RecyclerArrayAdapter<HLServiceProviderServiceModel>(context) {
    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLProviderServiceViewHolder (parent, R.layout.item_provider_service)
    }

    class HLProviderServiceViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLServiceProviderServiceModel>(parent, res) {

        private val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
        private val rateTextView: TextView = itemView.findViewById(R.id.rateTextView)


        @SuppressLint("SetTextI18n")
        override fun setData(data: HLServiceProviderServiceModel?) {
            data?.let { service ->
                serviceTextView.text = "${service.service} (x${service.quantity})"

                val amount = service.rate * (service.quantity).toDouble()
                if (HLGlobalData.me?.type == HLUserType.PROVIDER) {
                    rateTextView.text = "$ ${String.format("%.02f", amount)}"
                } else {
                    rateTextView.text = "$ ${String.format("%.02f", amount.withStripeFee)}"
                }
            }
        }
    }
}