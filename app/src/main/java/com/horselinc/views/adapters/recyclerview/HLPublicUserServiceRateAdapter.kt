package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.horselinc.R
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter


class HLPublicUserServiceRateAdapter(context: Context?) : RecyclerArrayAdapter<HLServiceProviderServiceModel>(context) {
    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLPublicUserServiceRateViewHolder (parent, R.layout.item_provider_service)
    }

    class HLPublicUserServiceRateViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLServiceProviderServiceModel>(parent, res) {

        private val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
        private val rateTextView: TextView = itemView.findViewById(R.id.rateTextView)


        @SuppressLint("SetTextI18n")
        override fun setData(data: HLServiceProviderServiceModel?) {
            data?.let { service ->
                serviceTextView.text = service.service
                rateTextView.text = "$ " + String.format("%.02", service.rate)
            }
        }
    }
}