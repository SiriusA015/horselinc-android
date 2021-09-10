package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.models.data.HLServiceProviderServiceModel
import kotlinx.android.synthetic.main.item_invoice_service_view.view.*


class HLInvoiceServiceViewAdapter(private var context: Context?
                                  , private var services: List<HLServiceProviderServiceModel>) : RecyclerView.Adapter<HLInvoiceServiceViewAdapter.HLInvoiceServiceViewViewHolder>() {

    override fun onBindViewHolder(holder: HLInvoiceServiceViewViewHolder, position: Int) {
        holder.bindData(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HLInvoiceServiceViewViewHolder {
        var v = LayoutInflater.from(context).inflate(R.layout.item_invoice_service_view, parent, false)
        return HLInvoiceServiceViewViewHolder(v)
    }

    override fun getItemCount() = services.size

    inner class HLInvoiceServiceViewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SetTextI18n")
        fun bindData(position: Int) {
            val service = services[position]

            itemView.tvTitle.text = "${service.service} (x${service.quantity})"
            itemView.tvPrice.text = "$ %.2f".format(service.quantity.toFloat() * service.rate)
        }

    }
}