package com.horselinc.views.fragments


import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ListView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.horselinc.*
import com.horselinc.models.data.HLServiceProviderServiceModel
import com.horselinc.models.event.HLNetworkChangeEvent
import com.horselinc.utils.DialogUtil
import com.horselinc.utils.NetworkUtil
import com.horselinc.views.activities.HLCalendarActivity
import com.horselinc.views.listeners.HLSelectServicesListener
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

open class HLBaseFragment : Fragment() {

    var rootView: View? = null
    var actionBar: ActionBar? = null
    var isNetworkConnected: Boolean = true

    private var dlgProgress: AlertDialog? = null

    /**
     *  Event Bus Handlers
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedNetworkChangeEvent (event: HLNetworkChangeEvent) {
        try {
            if (event.networkState == NetworkUtil.NETWORK_CONNECTED) {
                handleInternetAvailable()
            } else {
                handleInternetUnavailable()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Internet Check Handlers
     */
    open fun handleInternetAvailable () {
        isNetworkConnected = true
    }

    open fun handleInternetUnavailable () {
        isNetworkConnected = false
    }


    /**
     * Fragment Transition Handlers
     */
    private fun getContainerId(): Int {
        if (null != rootView) return (rootView?.parent as ViewGroup).id
        return 0
    }

    fun replaceFragment(fragment: Fragment, containerId: Int? = null, addToBackStack: Boolean = true) {
        hideKeyboard()

        val id = containerId ?: getContainerId()

        if (addToBackStack) {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(id, fragment)
                ?.addToBackStack(fragment.javaClass.name)
                ?.commit()
        } else {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(id, fragment)
                ?.commit()
        }
    }

    protected fun popFragment(popFragment: Class<*>? = null) {
        hideKeyboard()

        popFragment?.let {
            activity?.supportFragmentManager?.popBackStack(popFragment.name, 0)
        } ?: activity?.supportFragmentManager?.popBackStack()
    }

    protected fun popToMain () {
        activity?.supportFragmentManager?.popBackStack(null, POP_BACK_STACK_INCLUSIVE)
    }

    protected fun showProgressDialog() {
        dlgProgress?.dismiss()
        dlgProgress = activity?.let { DialogUtil.showProgressDialog(it) }
    }

    fun hideProgressDialog() {
        dlgProgress?.dismiss()
    }

    protected fun showError (message: String?) {
        hideProgressDialog()
        message?.let {
            showErrorMessage(it)
        }
    }

    protected fun openBrowser (url: String, activityResultCode: Int = 0) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (activityResultCode > 0) {
            startActivityForResult(browserIntent, activityResultCode)
        } else {
            startActivity(browserIntent)
        }
    }

    protected fun showCalendar (startDate: Long? = null, endDate: Long? = null, selectedDate: Long? = null) {
        val calendarIntent = Intent (activity, HLCalendarActivity::class.java)

        startDate?.let {
            calendarIntent.putExtra(IntentExtraKey.CALENDAR_START_DATE, it)
        }

        endDate?.let {
            calendarIntent.putExtra(IntentExtraKey.CALENDAR_END_DATE, it)
        }

        selectedDate?.let {
            calendarIntent.putExtra(IntentExtraKey.CALENDAR_SELECTED_DATE, it)
        }

        startActivityForResult(calendarIntent, ActivityRequestCode.SELECT_DATE)
    }

    protected fun showSelectService (services: List<HLServiceProviderServiceModel>, listener: HLSelectServicesListener) {

        if (services.isEmpty()) {
            showInfoMessage("You've selected all services")
            return
        }

        val items = services.map {
            it.selected = false
            it.quantity = 1
            "${it.service} - ${"$%.02f".format(it.rate)}"
        }

        val adapter = ArrayAdapter<String>(activity!!, R.layout.item_select_service_dlg, items)
        val dlg = AlertDialog.Builder(activity!!)
            .setCancelable(true)
            .setAdapter(adapter, null)
            .setPositiveButton("Done") { _, _ ->
                listener.onClickDone(services.filter { it.selected })
            }
            .setNegativeButton("Close") { _, _ ->
            }
            .create().apply {
                listView.itemsCanFocus = false
                listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
                listView.setOnItemClickListener { _, view, position, _ ->
                    services[position].selected = (view as CheckedTextView).isChecked
                }
            }

        dlg.show()
    }
}
