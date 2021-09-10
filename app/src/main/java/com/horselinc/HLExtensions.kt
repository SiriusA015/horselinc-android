package com.horselinc

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.github.razir.progressbutton.*
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import com.tapadoo.alerter.Alerter
import org.threeten.bp.DayOfWeek
import org.threeten.bp.temporal.WeekFields
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


/**
 *  Fragment Extensions
 */
fun Fragment.hideKeyboard() {
    activity?.let {
        val inputMethodManager = it.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        val view = it.currentFocus ?: View(it)
        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun Fragment.showMessage(message: String?) {
    activity?.let {
        AlertDialog.Builder(it)
            .setTitle(getString(R.string.app_name))
            .setMessage(message)
            .setNegativeButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

fun Fragment.showSuccessMessage (message: String) {
    if (Alerter.isShowing) {
        Alerter.hide()
    }

    activity?.let {
        Alerter.create(activity)
            .setTitle(R.string.app_name)
            .setText(message)
            .setIcon(R.drawable.ic_message_success)
            .setBackgroundColorRes(R.color.colorTeal)
            .show()
    }
}

fun Fragment.showErrorMessage (error: String) {
    if (Alerter.isShowing) {
        Alerter.hide()
    }

    activity?.let {
        Alerter.create(activity)
            .setTitle(R.string.app_name)
            .setText(error)
            .setIcon(R.drawable.ic_message_error)
            .setBackgroundColorRes(R.color.colorPink)
            .show()
    }
}

fun Fragment.showInfoMessage (info: String) {
    if (Alerter.isShowing) {
        Alerter.hide()
    }

    activity?.let {
        Alerter.create(activity)
            .setTitle(R.string.app_name)
            .setText(info)
            .setIcon(R.drawable.ic_message_info)
            .setBackgroundColorRes(R.color.colorDarkBlue)
            .show()
    }
}


fun Fragment.setProgressButton (progressButton: Button?) {
    progressButton?.let {
        bindProgressButton(it)
        it.attachTextChangeAnimator {
            fadeInMills = 300
            fadeOutMills = 300
        }
    }
}

fun Fragment.showProgressButton (progressButton: Button?, progressColor: Int = Color.WHITE, gravity: Int = DrawableButton.GRAVITY_CENTER) {
    progressButton?.let {
        it.showProgress {
            this.progressColor = progressColor
            this.gravity = gravity
        }

        it.isEnabled = false
    }
}

fun Fragment.hideProgressButton (progressButton: Button?, isEnabled: Boolean = true, stringResId: Int) {
    progressButton?.hideProgress(stringResId)
    progressButton?.isEnabled = isEnabled
}

fun Fragment.hideProgressButton (progressButton: Button?, isEnabled: Boolean = true, string: String? = progressButton?.text.toString()) {
    progressButton?.hideProgress(string)
    progressButton?.isEnabled = isEnabled
}

/**
 *  Activity Extensions
 */

fun AppCompatActivity.hideKeyboard () {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = currentFocus ?: View(this)
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun AppCompatActivity.showSuccessMessage (message: String) {
    if (Alerter.isShowing) {
        Alerter.hide()
    }

    Alerter.create(this)
        .setTitle(R.string.app_name)
        .setText(message)
        .setIcon(R.drawable.ic_message_success)
        .setBackgroundColorRes(R.color.colorTeal)
        .show()
}

fun AppCompatActivity.showErrorMessage (error: String) {
    if (Alerter.isShowing) {
        Alerter.hide()
    }

    Alerter.create(this)
        .setTitle(R.string.app_name)
        .setText(error)
        .setIcon(R.drawable.ic_message_error)
        .setBackgroundColorRes(R.color.colorPink)
        .show()
}

fun AppCompatActivity.showInfoMessage (info: String) {
    if (Alerter.isShowing) {
        Alerter.hide()
    }

    Alerter.create(this)
        .setTitle(R.string.app_name)
        .setText(info)
        .setIcon(R.drawable.ic_message_info)
        .setBackgroundColorRes(R.color.colorDarkBlue)
        .show()
}


/**
 *  Image View Extension
 */
fun ImageView.loadImage (url: String?, placeHolderRes: Int) {
    if ((url ?: "").isEmpty()) {
        setImageResource(placeHolderRes)
    } else {
        Picasso.get()
            .load(url)
            .error(placeHolderRes)
            .placeholder(placeHolderRes)
            .into(this)
    }
}


/**
 *  Any Object Extension
 */
fun <T> Any.toCustomObject (valueType: Class<T>): T? {
    val gson = Gson ()
    val jsonString = gson.toJson(this) ?: return null
    return try {
        gson.fromJson(jsonString, valueType)
    } catch (ex: Exception) {
        null
    }
}


/**
 *  Double Extension
 */
val Double.withStripeFee: Double
    get() {
        return this  * ((HLGlobalData.settings.applicationFee) + 100) / 100
    }

val Double.priceString: String
    get() {
        return ""
    }

/**
 *  Float Extension
 */
fun Float.toCurrencyString (currencySymbol: String): String {
    val numberFormat = NumberFormat.getCurrencyInstance()
    numberFormat.currency = Currency.getInstance(currencySymbol)
    return numberFormat.format(this)
}


/**
 *  Long Extension
 */
val Long.simpleDateString: String
    get() {
        return try {
            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            formatter.timeZone = TimeZone.getDefault()
            formatter.format(Date(this))
        } catch (ex: Exception) {
            ex.printStackTrace()
            ""
        }
    }

val Long.calendar: Calendar
    get() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = this
        return calendar
    }

val Long.date: Date
    get() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = this
        return calendar.time
    }


/**
 *  Calendar Extension
 */
fun Calendar.isSameDay(other: Calendar?): Boolean {

    return (other != null &&
            get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            get(Calendar.MONTH) == other.get(Calendar.MONTH) &&
            get(Calendar.DAY_OF_MONTH) == other.get(Calendar.DAY_OF_MONTH))
}

/**
 *  Date Extension
 */
fun Date?.formattedString (format: String, timeZone: String = ""): String {
    return try {
        val formatter = SimpleDateFormat(format, Locale.getDefault())
        formatter.timeZone = if (timeZone.isEmpty()) TimeZone.getDefault() else TimeZone.getTimeZone(timeZone)
        val formatDate = this ?: Calendar.getInstance().time
        formatter.format(formatDate)
    } catch (ex: Exception) {
        ex.printStackTrace()
        ""
    }
}


/**
 *  Root
 */
fun daysOfWeekFromLocale(): Array<DayOfWeek> {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    var daysOfWeek = DayOfWeek.values()
    if (firstDayOfWeek != DayOfWeek.MONDAY) {
        val rhs = daysOfWeek.sliceArray(firstDayOfWeek.ordinal..daysOfWeek.indices.last)
        val lhs = daysOfWeek.sliceArray(0 until firstDayOfWeek.ordinal)
        daysOfWeek = rhs + lhs
    }
    return daysOfWeek
}


