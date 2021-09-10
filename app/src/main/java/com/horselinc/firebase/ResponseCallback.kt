package com.horselinc.firebase

interface ResponseCallback<T> {
    fun onSuccess (data: T) {}
    fun onFailure (error: String) {}
}