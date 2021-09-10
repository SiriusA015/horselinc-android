package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.models.data.HLServiceProviderServiceModel
import kotlinx.android.synthetic.main.item_rate.view.*


class HLRateAdapter(private var context: Context?
                    , private var services: List<HLServiceProviderServiceModel>) : RecyclerView.Adapter<HLRateAdapter.HLRateViewHolder>() {

    override fun onBindViewHolder(holder: HLRateViewHolder, position: Int) {
        holder.bindData(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HLRateViewHolder {
        var v = LayoutInflater.from(context).inflate(R.layout.item_rate, parent, false)
        return HLRateViewHolder(v)
    }

    override fun getItemCount() = services.size

    inner class HLRateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindData(position: Int) {
            val service = services[position]

            itemView.tvTitle.text = service.service
            itemView.tvPrice.text = "$ %.02f".format(service.rate)
        }

    }
}