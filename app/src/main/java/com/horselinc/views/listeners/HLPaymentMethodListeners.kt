package com.horselinc.views.listeners

import com.horselinc.models.data.HLStripeCustomerModel

interface HLPaymentMethodItemListener {
    fun onClickRadio (position: Int)
    fun onClickDelete (position: Int)
}

interface HLAddPaymentCardListener {
    fun onAddToCustomer (newCustomer: HLStripeCustomerModel) {}
}