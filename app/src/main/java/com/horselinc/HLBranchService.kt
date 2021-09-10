package com.horselinc

import android.content.Context
import com.horselinc.firebase.ResponseCallback
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.LinkProperties

object HLBranchService {

    fun createInviteLink (context: Context,
                          userId: String,
                          userType: String,
                          invoiceId: String? = null,
                          horseId: String? = null,
                          callback: ResponseCallback<String>) {
        val lp = LinkProperties()
            .addControlParameter("link_type", HLDeepLinkType.INVITE)
            .addControlParameter("sender_id", userId)
            .addControlParameter("sender_type", userType)
            .addControlParameter("platform", HLPlatformType.ANDROID)

        invoiceId?.let {
            lp.addControlParameter("invoice_id", it)
        }

        horseId?.let {
            lp.addControlParameter("horse_id", it)
        }

        BranchUniversalObject().generateShortUrl(context, lp) { url, error ->
            if (error == null) {
                callback.onSuccess(url)
            } else {
                callback.onFailure(error.message)
            }
        }
    }
}