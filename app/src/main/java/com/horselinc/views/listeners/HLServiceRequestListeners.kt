package com.horselinc.views.listeners

import com.horselinc.models.data.HLServiceProviderModel
import com.horselinc.models.data.HLServiceRequestModel

interface HLManagerServiceRequestItemListener {
    fun onClickOption (request: HLServiceRequestModel?, position: Int) {}
    fun onClickProvider (provider: HLServiceProviderModel?) {}
    fun onClickChooseProvider (request: HLServiceRequestModel?, position: Int) {}
}

interface HLProviderServiceRequestItemListener {
    fun onClickHorse (request: HLServiceRequestModel?, position: Int) {}
    fun onClickOption (request: HLServiceRequestModel?, position: Int) {}
    fun onClickTrainer (request: HLServiceRequestModel?, position: Int) {}
    fun onClickMarkComplete (request: HLServiceRequestModel?, position: Int) {}
    fun onClickAssignJob (request: HLServiceRequestModel?, position: Int) {}
    fun onClickAcceptJob (request: HLServiceRequestModel?, position: Int) {}
    fun onClickDismiss (request: HLServiceRequestModel?, position: Int) {}
}