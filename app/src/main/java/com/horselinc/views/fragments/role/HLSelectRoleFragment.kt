package com.horselinc.views.fragments.role


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import com.horselinc.HLUserType
import com.horselinc.R
import com.horselinc.views.fragments.HLBaseFragment

class HLSelectRoleFragment : HLBaseFragment() {

    private var horseManagerCardView: CardView? = null
    private var serviceProviderCardView: CardView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_select_role, container, false)

            // initialize controls
            initControls ()
        }
        return rootView
    }

    private fun initControls () {
        // variables
        horseManagerCardView = rootView?.findViewById(R.id.horseManagerCardView)
        serviceProviderCardView = rootView?.findViewById(R.id.serviceProviderCardView)

        // event handler
        horseManagerCardView?.setOnClickListener { replaceFragment(
            HLCreateProfileFragment(
                HLUserType.MANAGER
            )
        ) }
        serviceProviderCardView?.setOnClickListener { replaceFragment(
            HLCreateProfileFragment(
                HLUserType.PROVIDER
            )
        ) }
    }
}
