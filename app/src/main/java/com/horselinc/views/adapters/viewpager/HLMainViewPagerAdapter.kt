package com.horselinc.views.adapters.viewpager

import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.horselinc.HLUserType
import com.horselinc.views.fragments.common.HLPaymentsFragment
import com.horselinc.views.fragments.manager.*
import com.horselinc.views.fragments.provider.HLProviderHorsesFragment
import com.horselinc.views.fragments.provider.HLProviderProfileFragment
import com.horselinc.views.fragments.provider.HLProviderSchedulesFragment

class HLMainViewPagerAdapter(fm: FragmentManager, private val userType: String) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    var registeredFragments = SparseArray<Fragment>()

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    fun getRegisteredFragment(position: Int): Fragment {
        return registeredFragments.get(position)
    }

    override fun getItem(position: Int): Fragment {
        return if (userType == HLUserType.MANAGER) {
            when (position) {
                0 -> HLManagerHorsesFragment ()
                1 -> HLPaymentsFragment()
                else -> HLManagerProfileFragment ()
            }
        } else {
            when (position) {
                0 -> HLProviderHorsesFragment ()
                1 -> HLProviderSchedulesFragment ()
                2 -> HLPaymentsFragment()
                else -> HLProviderProfileFragment ()
            }
        }
    }

    override fun getCount(): Int {
        return if (userType == HLUserType.MANAGER) 3 else 4
    }
}