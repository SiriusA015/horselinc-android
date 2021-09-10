package com.horselinc.views.listeners

import com.horselinc.models.data.HLBaseUserModel
import com.horselinc.models.data.HLHorseManagerProviderModel
import com.horselinc.models.data.HLServiceProviderServiceModel

interface HLEditBaseUserItemListener {
    fun onClickDelete (position: Int, user: HLBaseUserModel) {}
}

interface HLManagerServiceProviderItemListener {
    fun onClickDelete (position: Int, provider: HLHorseManagerProviderModel)
}
interface HLProviderServiceEditDeleteListener {
    fun onClickDelete(rowIndex: Int, service: HLServiceProviderServiceModel)
    fun onClickEdit(rowIndex: Int, service: HLServiceProviderServiceModel)
    fun onClickAddAnother()
}