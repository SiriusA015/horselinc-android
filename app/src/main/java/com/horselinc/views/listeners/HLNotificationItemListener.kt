package com.horselinc.views.listeners

import com.horselinc.models.data.HLNotificationModel

interface HLNotificationItemListener {
    fun onClickDelete (position: Int, data: HLNotificationModel) {}
}