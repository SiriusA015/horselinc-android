package com.horselinc.views.adapters.viewpager

import android.content.Context
import androidx.viewpager.widget.PagerAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.horselinc.R


class HLIntroViewPagerAdapter(private val context: Context) : PagerAdapter() {

    private val introPageResIds = listOf(R.layout.view_welcome, R.layout.view_horse_managers, R.layout.view_service_providers)

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(introPageResIds[position], collection, false) as ViewGroup
        collection.addView(layout)
        return layout
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    override fun getCount(): Int {
        return introPageResIds.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}