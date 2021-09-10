package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.models.data.HLServiceProviderServiceModel
import kotlinx.android.synthetic.main.item_invoice_select_service.view.*
import java.lang.Exception


class HLInvoiceSelectServiceAdapter(private var context: Context?,
                                    private var services: List<HLServiceProviderServiceModel>,
                                    private var isEditable: Boolean = true,
                                    private var deleteListener: (position: Int) -> Unit)
    : RecyclerView.Adapter<HLInvoiceSelectServiceAdapter.HLInvoiceSelectServiceViewHolder>() {

    override fun onBindViewHolder(holder: HLInvoiceSelectServiceViewHolder, position: Int) {
        holder.bindData(position, isEditable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HLInvoiceSelectServiceViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.item_invoice_select_service, parent, false)
        return HLInvoiceSelectServiceViewHolder(v)
    }

    override fun getItemCount() = services.size

    inner class HLInvoiceSelectServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SetTextI18n")
        fun bindData(position: Int, isEditable: Boolean) {
            val service = services[position]

            try {
                itemView.etName.removeTextChangedListener(itemView.etName.tag as TextWatcher?)
                itemView.etPrice.removeTextChangedListener(itemView.etPrice.tag as TextWatcher?)
                itemView.etQuantity.removeTextChangedListener(itemView.etQuantity.tag as TextWatcher?)
            } catch (e: Exception) {

            }

            itemView.etName.isEnabled = isEditable
            itemView.etName.isFocusable = isEditable
            itemView.etPrice.isEnabled = isEditable
            itemView.etPrice.isFocusable = isEditable
            itemView.etQuantity.isEnabled = isEditable
            itemView.etQuantity.isFocusable = isEditable

            if (service.uid.isEmpty()) {
                itemView.tvName.visibility = View.GONE
                itemView.etName.visibility = View.VISIBLE
                itemView.etName.setText(service.service)
                itemView.tvPrice.visibility = View.GONE
                itemView.etPrice.visibility = View.VISIBLE
                itemView.etPrice.hint = "0.00"
                if (service.rate != 0.0f) {
                    itemView.etPrice.setText("%.02f".format(service.rate))
                }

                itemView.etName.tag = object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        service.service = itemView.etName.text.toString()
                    }
                }
                itemView.etName.addTextChangedListener(itemView.etName.tag as TextWatcher)

                itemView.etPrice.tag = object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        service.rate = itemView.etPrice.text.toString().toFloatOrNull() ?: 0f
                    }
                }
                itemView.etPrice.addTextChangedListener(itemView.etPrice.tag as TextWatcher)
            } else {
                itemView.tvName.visibility = View.VISIBLE
                itemView.tvName.text = service.service
                itemView.etName.visibility = View.GONE
                itemView.tvPrice.visibility = View.VISIBLE
                itemView.tvPrice.text = "%.02f".format(service.rate)
                itemView.etPrice.visibility = View.GONE
            }

            itemView.etQuantity.setText(service.quantity.toString())

            itemView.etQuantity.tag = object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    service.quantity = itemView.etQuantity.text.toString().toIntOrNull() ?: 0
                }
            }
            itemView.etQuantity.addTextChangedListener(itemView.etQuantity.tag as TextWatcher)

            itemView.btDelete.setOnClickListener { deleteListener(position) }
        }

    }
}