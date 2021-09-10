package com.horselinc.views.activities

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLServiceShowModel
import com.horselinc.models.event.HLSearchEvent
import com.horselinc.showErrorMessage
import com.horselinc.showInfoMessage
import com.horselinc.utils.NetworkChangeListener
import com.horselinc.utils.NetworkUtil
import com.horselinc.views.adapters.recyclerview.HLSearchServiceShowAdapter
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_search_service_show.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class HLSearchServiceShowActivity : AppCompatActivity(), NetworkChangeListener {

    private lateinit var showAdapter: HLSearchServiceShowAdapter
    private val disposable = CompositeDisposable ()

    private var isNetworkConnected = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_service_show)

        NetworkUtil.addNetworkChangeListener(this)

        // title
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear_white)

        val title = SpannableString(getString(R.string.search_for_show_by_title))
        title.setSpan(ForegroundColorSpan(Color.WHITE), 0, title.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        supportActionBar?.title = title


        showAdapter = HLSearchServiceShowAdapter(this).apply {
            setOnItemClickListener { position ->
                EventBus.getDefault().post(HLSearchEvent(showAdapter.getItem(position)))
                finish()
            }
        }

        lstServices.apply {
            adapter = showAdapter
            setLayoutManager(LinearLayoutManager(this@HLSearchServiceShowActivity))
            showEmpty()
        }

        setupSearchEdit()
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkUtil.removeNetworkChangeListener()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onChangedNetworkStatus(networkState: Int) {
        if (networkState == NetworkUtil.NETWORK_CONNECTED) {
            isNetworkConnected = true
            showInfoMessage(getString(R.string.msg_info_network))
            lstServices.swipeToRefresh?.isEnabled = true
        } else {
            isNetworkConnected = false
            showErrorMessage(getString(R.string.msg_err_network))
            lstServices.setRefreshing(false)
            lstServices.swipeToRefresh?.isEnabled = false
        }
    }

    private fun setupSearchEdit() {
        disposable.add(
            RxTextView.textChanges(etSearch)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { searchName ->
                    val searchKey = searchName.trim().toString()

                    showAdapter.clear()

                    if (searchKey.length < 3) {
                        ivSearch.visibility = View.VISIBLE
                    } else {
                        ivSearch.visibility = View.GONE
                        searchShows(searchKey)
                    }
                }
        )
    }

    private fun searchShows(searchKey: String) {
        lstServices.setRefreshing(true)

        HLFirebaseService.instance.searchServiceShows(searchKey, null, null, object :
            ResponseCallback<List<HLServiceShowModel>> {
            override fun onSuccess(data: List<HLServiceShowModel>) {
                lstServices.setRefreshing(false)
                showAdapter.clear()
                showAdapter.addAll(data)
            }
        })
    }

}
