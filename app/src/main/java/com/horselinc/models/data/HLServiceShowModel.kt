package com.horselinc.models.data

import java.util.*

data class HLServiceShowModel (
    var uid: String = "",
    var name: String = "",
    var createdAt: Long = Calendar.getInstance().timeInMillis
)