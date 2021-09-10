package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseOwnerModel
import kotlinx.android.synthetic.main.item_invoice_owner.view.*


class HLInvoiceOwnerAdapter(private var context: Context?
                            , private var owners: List<HLHorseOwnerModel>) : RecyclerView.Adapter<HLInvoiceOwnerAdapter.HLInvoiceOwnerViewHolder>() {

    override fun onBindViewHolder(holder: HLInvoiceOwnerViewHolder, position: Int) {
        holder.bindData(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HLInvoiceOwnerViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.item_invoice_owner, parent, false)
        return HLInvoiceOwnerViewHolder(v)
    }

    override fun getItemCount() = owners.size

    inner class HLInvoiceOwnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindData(position: Int) {
            val owner = owners[position]

            itemView.ivAvatar.loadImage(owner.avatarUrl, R.drawable.ic_profile)
            itemView.tvName.text = owner.name
        }

    }
}