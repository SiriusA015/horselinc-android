package com.horselinc.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.horselinc.HLGlobalData

class HLFirebaseMessagingService : FirebaseMessagingService() {

    private val tag: String = "HLMessagingService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(tag, "From:" + remoteMessage.from)

        if (remoteMessage.data.size.compareTo(0) == 0) {
            Log.d(tag, "Message data payload: " + remoteMessage.data)

            // Schedule job or handle now
        }

        if (remoteMessage.notification != null) {
            Log.d(tag, "Message Notification Body: " + remoteMessage.notification?.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        HLGlobalData.token = token

        HLFirebaseService.instance.registerToken(token, object: ResponseCallback<String> {})
    }
}
