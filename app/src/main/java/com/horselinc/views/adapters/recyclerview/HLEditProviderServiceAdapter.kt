package com.horselinc.views.adapters.recyclerview

import android.app.AlertDialog
import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.horselinc.R
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.views.listeners.HLProviderServiceItemListener
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter


class HLEditProviderServiceAdapter(context: Context?,
                                   private val itemListener: HLProviderServiceItemListener) : RecyclerArrayAdapter<HLServiceProviderServiceModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLProviderServiceViewHolder (parent, R.layout.item_edit_provider_service)
    }

    class HLProviderServiceViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLServiceProviderServiceModel>(parent, res) {

        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val rateTextView: TextView = itemView.findViewById(R.id.rateTextView)
        private val editButton: ImageView = itemView.findViewById(R.id.editImageView)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteImageView)

        init {
            editButton.setOnClickListener { onClickEdit () }
            deleteButton.setOnClickListener { onClickDelete () }
        }

        override fun setData(data: HLServiceProviderServiceModel?) {
            data?.let {
                nameTextView.text = it.service
                rateTextView.text = String.format("$%.02f", it.rate)
            }
        }

        private fun onClickEdit () {
            getOwnerAdapter<HLEditProviderServiceAdapter>()?.run {
                itemListener.onClickEdit(dataPosition)
            }
        }

        private fun onClickDelete () {
            getOwnerAdapter<HLEditProviderServiceAdapter>()?.run {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.app_name))
                    .setMessage(R.string.msg_alert_delete_service_rate)
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