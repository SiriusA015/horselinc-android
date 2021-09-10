package com.horselinc.models.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import java.util.*

data class HLNotificationModel (
    var uid: String = "",
    var message: String = "",
    var receiverId: String = "",
    @field: JvmField var isRead: Boolean = false,
    var creator: HLBaseUserModel = HLBaseUserModel(),
    var createdAt: Long = Calendar.getInstance().timeInMillis
) {
    @set: Exclude @get: Exclude var documentSnapshot: DocumentSnapshot? = null
}