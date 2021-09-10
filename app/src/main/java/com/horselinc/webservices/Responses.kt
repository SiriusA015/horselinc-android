package com.horselinc.webservices

data class SignInResponse (
    var token: String?
)

data class MessageResponse (
    var message: String?
)

data class CreateDeepLinkResponse (
    var url: String?
)