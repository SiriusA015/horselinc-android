package com.horselinc.views.adapters.viewpager

import android.content.Context
import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.horselinc.views.fragments.common.HLPaymentFragment
import com.horselinc.HLPaymentScreenType
import com.horselinc.HLUserType
import com.horselinc.R


/** Created by jcooperation0137 on 2019-08-26.
 */
class HLPaymentViewPagerAdapter(fm: FragmentManager, val context: Context?, private val userType: String): FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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

    override fun getPageTitle(position: Int): CharSequence? {
        super.getPageTitle(position)
        return if (userType == HLUserType.MANAGER) {
            when(position) {
                0 -> context?.getString(R.string.payment_manager_outstanding)
                else -> context?.getString(R.string.payment_manager_completed)
            }
        } else {
            when(position) {
                0 -> context?.getString(R.string.payment_provider_drafts)
                1 -> context?.getString(R.string.payment_provider_submitted)
                else -> context?.getString(R.string.payment_provider_paid)
            }
        }
    }

    override fun getItem(position: Int): Fragment {
        return if (userType == HLUserType.MANAGER) {
            when(position) {
                0 -> HLPaymentFragment(HLPaymentScreenType.outstanding)
                else -> HLPaymentFragment(HLPaymentScreenType.completed)
            }
        } else {
            when(position) {
                0 -> HLPaymentFragment(HLPaymentScreenType.drafts)
                1 -> HLPaymentFragment(HLPaymentScreenType.submitted)
                else -> HLPaymentFragment(HLPaymentScreenType.paid)
            }
        }
    }

    override fun getCount(): Int {
        return if (userType == HLUserType.MANAGER) 2 else 3
    }
}