package com.horselinc.models.data

import java.util.*

/**
 *  Horse Owner Model
 */
data class HLHorseOwnerModel (
    var uid: String = "",
    var horseId: String = ""
): HLHorseManagerModel() {

    fun update (horseManager: HLHorseManagerModel) {
        this.userId = horseManager.userId
        this.name = horseManager.name
        this.avatarUrl = horseManager.avatarUrl
        this.phone = horseManager.phone
        this.location = horseManager.location
        this.createdAt = Calendar.getInstance().timeInMillis
        this.barnName = horseManager.barnName
        this.customer = horseManager.customer
    }
}