package com.horselinc.models.data

data class HLHorseManagerProviderModel (
    var uid: String = "",
    var creatorId: String = "",
    var serviceType: String = ""
): HLServiceProviderModel () {

    fun copy (serviceProvider: HLServiceProviderModel): HLHorseManagerProviderModel = HLHorseManagerProviderModel().apply {
        this.userId = serviceProvider.userId
        this.name = serviceProvider.name
        this.avatarUrl = serviceProvider.avatarUrl
        this.phone = serviceProvider.phone
        this.location = serviceProvider.location
        this.account = serviceProvider.account
    }
}
