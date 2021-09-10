package com.horselinc.views.listeners

import com.horselinc.models.data.HLServiceProviderServiceModel

interface HLSelectServicesListener {
    fun onClickDone (selectedServices: List<HLServiceProviderServiceModel>) {}
}