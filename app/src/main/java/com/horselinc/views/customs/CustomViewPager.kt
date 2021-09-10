package com.horselinc.views.customs

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager


class CustomViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {

    private var isSwipeEnabled = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return if (isSwipeEnabled) {
            super.onTouchEvent(ev)
        } else false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (isSwipeEnabled) {
            super.onInterceptTouchEvent(ev)
        } else false
    }

    override fun setCurrentItem(item: Int) {
        setCurrentItem(item ,false)
    }


    fun setSwipeEnabled (isEnabled: Boolean) {
        isSwipeEnabled = isEnabled
    }
}
