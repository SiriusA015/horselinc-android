package com.horselinc.views.customs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.horselinc.R
import com.horselinc.views.listeners.HLInvoiceGroupTypePopUpViewListener


/** Created by jcooperation0137 on 2019-08-28.
 */

class HLInvoiceGroupTypePopUpView(context: Context?): LinearLayout(context) {

    var containerView: View = LayoutInflater.from(context).inflate(R.layout.view_invoice_group_type_pop_up, this)
    var horseImageView: ImageView = containerView.findViewById(R.id.horseImageView)
    var userImageView: ImageView = containerView.findViewById(R.id.userImageView)

    init {
        updateButtonState(false)
    }

    fun setOnClickListener(listener: HLInvoiceGroupTypePopUpViewListener) {
        horseImageView.setOnClickListener {
            listener.onClickHorseButton()
        }

        userImageView.setOnClickListener {
            listener.onClickUserButton()
        }
    }

    fun updateButtonState(isHorseGroup: Boolean) {
        if (isHorseGroup) {
            horseImageView.isEnabled = false
            horseImageView.alpha = 1.0f

            userImageView.isEnabled = true
            userImageView.alpha = 0.2f
        } else {
            horseImageView.isEnabled = true
            horseImageView.alpha = 0.2f

            userImageView.isEnabled = false
            userImageView.alpha = 1.0f
        }
    }
}