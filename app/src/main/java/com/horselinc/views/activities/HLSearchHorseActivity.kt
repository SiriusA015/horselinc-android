package com.horselinc.views.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.horselinc.IntentExtraKey
import com.horselinc.R
import com.horselinc.models.event.HLNetworkChangeEvent
import com.horselinc.showErrorMessage
import com.horselinc.showInfoMessage
import com.horselinc.utils.NetworkChangeListener
import com.horselinc.utils.NetworkUtil
import com.horselinc.views.fragments.provider.HLProviderSearchHorseFragment
import org.greenrobot.eventbus.EventBus

class HLSearchHorseActivity : AppCompatActivity(), NetworkChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_horse)

        supportFragmentManager.beginTransaction()
            .replace(R.id.searchContainer, HLProviderSearchHorseFragment(intent.getBooleanExtra(IntentExtraKey.CREATE_INVOICE, false)))
            .commit()
    }

    override fun onChangedNetworkStatus(networkState: Int) {
        if (networkState == NetworkUtil.NETWORK_DISCONNECTED) {
            showErrorMessage(getString(R.string.msg_err_network))
        } else {
            showInfoMessage(getString(R.string.msg_info_network))
        }
        EventBus.getDefault().post(HLNetworkChangeEvent(networkState))
    }
}
