package com.horselinc.models.data

import com.horselinc.HLPlatformType
import com.horselinc.HLUserOnlineStatus
import java.util.*

/**
 *  User Model
 */
data class HLUserModel (
    var uid: String = "",
    var email: String = "",
    var serviceProvider: HLServiceProviderModel? = null,
    var horseManager: HLHorseManagerModel? = null,
    var token: String? = null,
    var status: String = HLUserOnlineStatus.ONLINE,
    var type: String? = null,
    var platform: String = HLPlatformType.ANDROID,
    var createdAt: Long = Calendar.getInstance().timeInMillis,
    var creatorId: String? = null
)

/**
 *  Base User Model
 */
open class HLBaseUserModel (
    var userId: String = "",
    var name: String = "",
    var avatarUrl: String? = null,
    var phone: String? = null,
    var location: String? = null,
    var createdAt: Long = Calendar.getInstance().timeInMillis,
    var invited: HLInvitedModel? = null
)

data class HLInvitedModel (
    var inviterId: String? = null,
    var inviterType: String? = null,
    var invitedFrom: String? = null
)