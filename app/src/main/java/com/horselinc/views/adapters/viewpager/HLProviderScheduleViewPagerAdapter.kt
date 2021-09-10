package com.horselinc.views.adapters.viewpager

import android.content.Context
import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.horselinc.R
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.fragments.provider.HLProviderCurrentScheduleFragment
import com.horselinc.views.fragments.provider.HLProviderPastScheduleFragment


class HLProviderScheduleViewPagerAdapter(fm: FragmentManager, val context: Context?): FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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

        return if (position == 0) {
            ResourceUtil.getString(R.string.current)
        } else {
            ResourceUtil.getString(R.string.past)
        }
    }

    override fun getItem(position: Int): Fragment {
        return if (position == 0) {
            HLProviderCurrentScheduleFragment ()
        } else {
            HLProviderPastScheduleFragment ()
        }
    }

    override fun getCount(): Int {
        return 2
    }
}