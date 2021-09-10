package com.horselinc.views.listeners

import com.horselinc.HLInvoiceMethodType
import com.horselinc.models.data.*


/** Created by jcooperation0137 on 2019-08-27.
 */

interface HLInvoiceGroupItemListener {
    fun onClickInvoiceCard(invoice: HLInvoiceModel)
}

interface HLInvoiceGroupTypePopUpViewListener {
    fun onClickHorseButton()
    fun onClickUserButton()
}

interface HLInvoiceUserInfoViewListener {
    fun onClickUserAvatar(user: HLBaseUserModel)
}

interface HLEditInvoiceServiceRequestViewListener {
    fun onClickHorseAvatar(position: Int, horse: HLHorseModel)
    fun onClickCalenderView(position: Int)
}

interface HLEditInvoiceSelectServiceAdapterListener {
    fun onClickDeleteServiceButton(sectionIndex: Int, position: Int)
    fun onClickSelectServiceButton(sectionIndex: Int, position: Int)
    fun onClickAddCustomServiceButton(sectionIndex: Int, position: Int)
}

interface HLSelectInvoiceContactItemListener {
    fun onClickInvoiceContact (invoiceMethod: HLInvoiceMethodType, contact: HLPhoneContactModel)
}