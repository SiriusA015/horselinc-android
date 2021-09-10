package com.horselinc.views.adapters.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLPhoneContactModel
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.makeramen.roundedimageview.RoundedImageView


class HLInvoiceContactAdapter(context: Context?) : RecyclerArrayAdapter<HLPhoneContactModel>(context) {

    override fun OnCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<*> {
        return HLInvoiceContactViewHolder (parent, R.layout.item_invoice_contact)
    }

    class HLInvoiceContactViewHolder(parent: ViewGroup?, res: Int) : BaseViewHolder<HLPhoneContactModel>(parent, res) {

        private val profileImageView: RoundedImageView = itemView.findViewById(R.id.contactImageView)
        private val infoTextView: TextView = itemView.findViewById(R.id.contactInfoTextView)

        @SuppressLint("SetTextI18n")
        override fun setData(data: HLPhoneContactModel?) {
            // profile image
            profileImageView.loadImage(data?.photoUri, R.drawable.ic_profile)

            // contact information
            data?.let { contact ->
                if (contact.name.isEmpty()) {
                    if (contact.emails.isNotEmpty()) {
                        infoTextView.text = contact.emails.first()
                    } else if (contact.phoneNumbers.isNotEmpty()) {
                        infoTextView.text = contact.phoneNumbers.first()
                    }
                } else {
                    when {
                        contact.emails.isNotEmpty() -> {
                            infoTextView.text = "${contact.name}\n${contact.emails.first()}"
                        }
                        contact.phoneNumbers.isNotEmpty() -> {
                            infoTextView.text = "${contact.name}\n${contact.phoneNumbers.first()}"
                        }
                        else -> {
                            infoTextView.text = contact.name
                        }
                    }
                }
            }
        }
    }
}