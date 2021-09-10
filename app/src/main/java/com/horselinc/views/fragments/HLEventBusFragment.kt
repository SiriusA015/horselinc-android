package com.horselinc.views.fragments

import android.os.Bundle
import org.greenrobot.eventbus.EventBus

/**
 *  Created by TengFei Li on 26, August, 2019
 */

open class HLEventBusFragment : HLBaseFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

}