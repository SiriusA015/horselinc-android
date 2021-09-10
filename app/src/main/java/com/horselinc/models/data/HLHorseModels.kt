package com.horselinc.models.data

import com.google.firebase.firestore.Exclude
import java.util.*
import kotlin.collections.ArrayList

data class HLHorseModel (
    var uid: String = "",
    var avatarUrl: String? = null,
    var barnName: String = "",
    var displayName: String = "",
    var gender: String = "",
    var birthYear: Int? = null,
    var trainerId: String = "",
    var creatorId: String = "",
    var leaserId: String? = null,
    var description: String? = null,
    var color: String? = null,
    var sire: String? = null,
    var dam: String? = null,
    var height: Double? = null,
    var registrations: ArrayList<HLHorseRegistrationModel>? = null,
    var privateNote: String? = null,
    var createdAt: Long = Calendar.getInstance().timeInMillis,
    @field: JvmField var isDeleted: Boolean = false
) {
    @get: Exclude var creator: HLHorseManagerModel? = null
    @get: Exclude var trainer: HLHorseManagerModel? = null
    @get: Exclude var leaser: HLHorseManagerModel? = null
    @get: Exclude var ownerIds: ArrayList<String>? = null
    @get: Exclude var owners: ArrayList<HLHorseOwnerModel>? = null


    fun copy (): HLHorseModel = HLHorseModel().apply {
        this.uid = this@HLHorseModel.uid
        this.avatarUrl = this@HLHorseModel.avatarUrl
        this.barnName = this@HLHorseModel.barnName
        this.displayName = this@HLHorseModel.displayName
        this.gender = this@HLHorseModel.gender
        this.birthYear = this@HLHorseModel.birthYear
        this.trainerId = this@HLHorseModel.trainerId
        this.creatorId = this@HLHorseModel.creatorId
        this.leaserId = this@HLHorseModel.leaserId
        this.description = this@HLHorseModel.description
        this.color = this@HLHorseModel.color
        this.sire = this@HLHorseModel.sire
        this.dam = this@HLHorseModel.dam
        this.height = this@HLHorseModel.height
        this.registrations = this@HLHorseModel.registrations
        this.privateNote = this@HLHorseModel.privateNote
        this.createdAt = this@HLHorseModel.createdAt
        this.isDeleted = this@HLHorseModel.isDeleted
        this.trainer = this@HLHorseModel.trainer
        this.creator = this@HLHorseModel.creator
        this.leaser = this@HLHorseModel.leaser
        this.ownerIds = this@HLHorseModel.ownerIds
        this.owners = this@HLHorseModel.owners
    }
}


/**
 *  Horse Registration Model
 */
data class HLHorseRegistrationModel (
    var name: String = "",
    var number: String = ""
)


/**
 * Horse Model for service provider search
 */
data class HLProviderHorseModel (
    var manager: HLHorseManagerModel = HLHorseManagerModel(),
    var horses: ArrayList<HLHorseModel> = ArrayList()
)