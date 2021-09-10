package com.horselinc.views.adapters.recyclerview

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.views.listeners.HLEditInvoiceSelectServiceAdapterListener


/** Created by jcooperation0137 on 2019-08-31.
 */
class HLEditInvoiceSelectServiceAdapter(var context: Context?,
                                        var sectionIndex: Int,
                                        var services: List<HLServiceProviderServiceModel>,
                                        var listener: HLEditInvoiceSelectServiceAdapterListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> HLEditInvoiceServiceViewHolder(LayoutInflater.from(context).inflate(R.layout.item_invoice_select_service, parent, false))
            else -> HLEditInvoiceAddServiceButtonsViewHolder(LayoutInflater.from(context).inflate(R.layout.item_invoice_add_service_buttons, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            1 -> (holder as HLEditInvoiceServiceViewHolder).setData(services[position], position)
            else -> (holder as HLEditInvoiceAddServiceButtonsViewHolder).setData(position)
        }
    }

    override fun getItemCount(): Int {
        return services.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == services.size) return 2
        return 1
    }


    inner class HLEditInvoiceServiceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private var nameTextView: TextView = itemView.findViewById(R.id.tvName)
        private var nameEditText: EditText = itemView.findViewById(R.id.etName)
        private var deleteButton: Button = itemView.findViewById(R.id.btDelete)
        private var priceTextView: TextView = itemView.findViewById(R.id.tvPrice)
        private var priceEditText: EditText = itemView.findViewById(R.id.etPrice)
        private var quantityEditText: EditText = itemView.findViewById(R.id.etQuantity)

        fun setData(service: HLServiceProviderServiceModel, rowIndex: Int) {
            try {
                nameEditText.removeTextChangedListener(nameEditText.tag as TextWatcher?)
                priceEditText.removeTextChangedListener(priceEditText.tag as TextWatcher?)
                quantityEditText.removeTextChangedListener(quantityEditText.tag as TextWatcher?)
            } catch (e: Exception) {

            }

            if (service.uid.isEmpty()) {
                nameTextView.visibility = View.GONE
                nameEditText.visibility = View.VISIBLE
                nameEditText.setText(service.service)
                priceTextView.visibility = View.GONE
                priceEditText.visibility = View.VISIBLE
                priceEditText.hint = "0.00"
                if (service.rate != 0.0f) {
                    priceEditText.setText("%.02f".format(service.rate))
                }

                nameEditText.tag = object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        service.service = nameEditText.text.toString()
                    }
                }
                nameEditText.addTextChangedListener(nameEditText.tag as TextWatcher)

                priceEditText.tag = object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        service.rate = priceEditText.text.toString().toFloatOrNull() ?: 0f
                    }
                }
                priceEditText.addTextChangedListener(priceEditText.tag as TextWatcher)
            } else {
                nameTextView.visibility = View.VISIBLE
                nameTextView.text = service.service
                nameEditText.visibility = View.GONE
                priceTextView.visibility = View.VISIBLE
                priceTextView.text = "%.02f".format(service.rate)
                priceEditText.visibility = View.GONE
            }

            quantityEditText.setText(service.quantity.toString())

            quantityEditText.tag = object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    service.quantity = quantityEditText.text.toString().toIntOrNull() ?: 0
                }
            }
            quantityEditText.addTextChangedListener(quantityEditText.tag as TextWatcher)

            deleteButton.setOnClickListener {
                listener.onClickDeleteServiceButton(sectionIndex, rowIndex)
            }
        }
    }

    inner class HLEditInvoiceAddServiceButtonsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private var selectServiceTextView: TextView = itemView.findViewById(R.id.tvSelectService)
        private var addCustomServiceTextView: TextView = itemView.findViewById(R.id.tvAddCustomService)

        fun setData(rowIndex: Int) {
            selectServiceTextView.setOnClickListener {
                listener.onClickSelectServiceButton(sectionIndex, rowIndex)
            }

            addCustomServiceTextView.setOnClickListener {
                listener.onClickAddCustomServiceButton(sectionIndex, rowIndex)
            }
        }
    }
}