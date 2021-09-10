package com.horselinc.models.data

data class HLPhoneContactModel (
    var id: String = "",
    var name: String = "",
    var photoUri: String? = null,
    var emails: ArrayList<String> = ArrayList(),
    var phoneNumbers: ArrayList<String> = ArrayList()
)