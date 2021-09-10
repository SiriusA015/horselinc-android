package com.horselinc.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.horselinc.App


object ResourceUtil {

    fun getString(@StringRes stringId: Int): String {
        return App.instance.getString(stringId)
    }

    fun getDrawable(@DrawableRes drawableId: Int): Drawable {
        return App.instance.resources.getDrawable(drawableId)
    }

    fun getColor(@ColorRes colorId: Int): Int {
        return ResourcesCompat.getColor(App.instance.resources, colorId, null)
    }

    fun getDimen(@DimenRes dimenId: Int): Float {
        return App.instance.resources.getDimension(dimenId)
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }

    fun isTablet(context: Context): Boolean {
        return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}