package com.horselinc.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build


object NetworkUtil {

    const val NETWORK_UNKNOWN = 0
    const val NETWORK_DISCONNECTED = 1
    const val NETWORK_CONNECTED = 2

    var networkChangeListener: NetworkChangeListener? = null
    var lastNetworkState: Int = NETWORK_UNKNOWN

    fun getNetworkState(context: Context): Int {
        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            cm.getNetworkCapabilities(cm.activeNetwork)?.run {
//                return when {
//                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TYPE_WIFI
//                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TYPE_MOBILE
//                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> TYPE_ETHERNET
//                    hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> TYPE_ETHERNET
//                    else -> TYPE_NOT_CONNECTED
//                }
//            } ?: return TYPE_NOT_CONNECTED
            cm.getNetworkCapabilities(cm.activeNetwork)?.run { return NETWORK_CONNECTED } ?: return NETWORK_DISCONNECTED

        } else {
            cm.activeNetworkInfo?.run {
                return if (isConnectedOrConnecting) NETWORK_CONNECTED else NETWORK_DISCONNECTED
            } ?: return NETWORK_DISCONNECTED
        }
    }

    fun addNetworkChangeListener (listener: NetworkChangeListener) {
        networkChangeListener = listener
    }

    fun removeNetworkChangeListener () {
        networkChangeListener = null
    }
}

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val networkState = NetworkUtil.getNetworkState(context)
        if (NetworkUtil.lastNetworkState != networkState) {
            NetworkUtil.networkChangeListener?.onChangedNetworkStatus(networkState)
            NetworkUtil.lastNetworkState = networkState
        }
    }
}

interface NetworkChangeListener {
    fun onChangedNetworkStatus (networkState: Int)
}