package com.horselinc.views.listeners

import com.horselinc.models.data.HLHorseManagerPaymentApproverModel

interface HLPaymentApproverItemListener {
    fun onClickEdit (position: Int)
    fun onClickDelete (position: Int)
}

interface HLPaymentApproverListener {
    fun onAdd (newApprover: HLHorseManagerPaymentApproverModel) {}
    fun onUpdate (updateApprover: HLHorseManagerPaymentApproverModel, updatePosition: Int) {}
}