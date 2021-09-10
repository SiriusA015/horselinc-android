package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.views.listeners.HLProviderServiceEditDeleteListener


/** Created by jcooperation0137 on 2019-09-01.
 */
class HLProviderServiceEditDeleteAdapter(var context: Context?,
                                         var services: ArrayList<HLServiceProviderServiceModel>,
                                         var listener: HLProviderServiceEditDeleteListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            1 -> HLProviderServiceEditDeleteViewHolder(LayoutInflater.from(context).inflate(R.layout.item_provider_service_edit_delete, parent, false))
            else -> HLProviderServiceAddViewHolder(LayoutInflater.from(context).inflate(R.layout.item_provider_service_add, parent, false))
        }
    }

    override fun getItemCount(): Int {
        return services.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder as HLProviderServiceEditDeleteViewHolder).setData(position, services[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (services.size == position) {
            true -> 2
            else -> 1
        }
    }

    inner class HLProviderServiceEditDeleteViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var serviceNameTextView: TextView = itemView.findViewById(R.id.serviceNameTextView)
        var serviceRateTextView: TextView = itemView.findViewById(R.id.serviceRateTextView)
        var editImageView: ImageView = itemView.findViewById(R.id.editImageView)
        var deleteImageView: ImageView = itemView.findViewById(R.id.deleteImageView)

        fun setData(rowIndex: Int, service: HLServiceProviderServiceModel) {
            serviceNameTextView.text = service.service
            serviceRateTextView.text = String.format("$ %.2f", service.rate)

            editImageView.setOnClickListener {
                listener.onClickEdit(rowIndex, service)
            }

            deleteImageView.setOnClickListener {
                listener.onClickDelete(rowIndex, service)
            }
        }
    }

    inner class HLProviderServiceAddViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var addServiceTextView: TextView = itemView.findViewById(R.id.addServiceTextView)

        init {
            addServiceTextView.setOnClickListener {
                listener.onClickAddAnother()
            }
        }
    }
}