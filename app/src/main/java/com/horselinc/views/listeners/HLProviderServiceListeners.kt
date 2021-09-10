package com.horselinc.views.listeners

import com.horselinc.models.data.HLServiceProviderServiceModel

interface HLProviderServiceItemListener {
    fun onClickEdit (position: Int)
    fun onClickDelete (position: Int)
}

interface HLProviderServiceListener {
    fun onAdd (service: HLServiceProviderServiceModel)
    fun onUpdate (service: HLServiceProviderServiceModel, position: Int)
}