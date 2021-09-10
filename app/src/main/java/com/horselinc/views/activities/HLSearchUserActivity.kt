package com.horselinc.views.activities

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLBaseUserModel
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.data.HLHorseManagerProviderModel
import com.horselinc.models.data.HLServiceProviderModel
import com.horselinc.models.event.HLSelectBaseUserEvent
import com.horselinc.utils.DialogUtil
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLBaseUserAdapter
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_search_user.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit


class HLSearchUserActivity : AppCompatActivity() {

    private var selectType = HLBaseUserSelectType.HORSE_TRAINER

    private val excludeUsers = ArrayList<String>()

    private lateinit var adapter: HLBaseUserAdapter
    private var providerAdapter: HLBaseUserAdapter? = null

    private val disposable = CompositeDisposable ()

    private var isLoadMore = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_user)

        // initialize controls
        initControls ()

        // set reactive x
        setReactiveX ()
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            hideKeyboard()
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    /**
     *  Event Handlers
     */
    private fun onSelectUser (selectedUser: HLBaseUserModel) {
        hideKeyboard()

        val baseUser = if (selectType == HLBaseUserSelectType.SERVICE_PROVIDER) {
            selectedUser as HLServiceProviderModel
        } else {
            selectedUser as HLHorseManagerModel
        }
        EventBus.getDefault().post(HLSelectBaseUserEvent(selectType, baseUser))
        finish()
    }

    private fun onClickInvite () {
        HLGlobalData.me?.let { user ->
            user.type?.let { userType ->
                val dialog = DialogUtil.showProgressDialog(this)
                HLBranchService.createInviteLink(this, user.uid, userType, callback = object: ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        dialog?.dismiss()

                        val isInviteProvider = intent.getBooleanExtra(IntentExtraKey.BASE_USER_INVITE_SERVICE_PROVIDER, false)
                        if (isInviteProvider) {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Signup for HorseLinc $data to manager all of your equine needs.")
                                type = "text/plain"
                            }

                            val shareIntent = Intent.createChooser(sendIntent, null)
                            startActivity(shareIntent)
                        } else {
                            val actionIntent = Intent(Intent.ACTION_SENDTO).apply {
                                type = "text/plain"
                                this.data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_SUBJECT, "You're Invited To HorseLinc!")
                                putExtra(Intent.EXTRA_TEXT, "${HLGlobalData.me?.horseManager?.name} wants you to join HorseLinc! You can download the app at $data")
                            }
                            startActivity(actionIntent)
                        }
                    }

                    override fun onFailure(error: String) {
                        dialog?.dismiss()
                        showErrorMessage(error)
                    }
                })
            }
        }
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // title
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear_white)

        val title = SpannableString(getString(R.string.search))
        title.setSpan(ForegroundColorSpan(Color.WHITE), 0, title.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        supportActionBar?.title = title

        // select type
        selectType = intent.getIntExtra(IntentExtraKey.BASE_USER_SELECT_TYPE, HLBaseUserSelectType.HORSE_TRAINER)

        // contain me
        excludeUsers.clear()
        intent.getStringArrayListExtra(IntentExtraKey.BASE_USER_EXCLUDE_IDS)?.let {
            excludeUsers.addAll(it)
        }

        // show my service providers
        val isVisible = intent.getBooleanExtra(IntentExtraKey.BASE_USER_MY_SERVICE_PROVIDER, false)
        if (isVisible) {
            myServiceProviderTextView.visibility = View.VISIBLE
            serviceProviderRecyclerView.visibility = View.VISIBLE

            initMyServiceProviderRecyclerView ()
            getMyServiceProviders()
        } else {
            myServiceProviderTextView.visibility = View.GONE
            serviceProviderRecyclerView.visibility = View.GONE
        }


        // recycler view
        adapter = HLBaseUserAdapter(this).apply {
            setOnItemClickListener { position ->
                onSelectUser (getItem(position))
            }

            setMore(R.layout.load_more_user, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (isLoadMore) {
                        val searchKey = searchEditText.text.trim().toString()
                        if (selectType == HLBaseUserSelectType.SERVICE_PROVIDER) {
                            searchServiceProviders(searchKey, false)
                        } else {
                            searchHorseManagers(searchKey, false)
                        }
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() {}
            })
        }
        userRecyclerView.adapter = adapter
        userRecyclerView.recyclerView.setHasFixedSize(false)
        userRecyclerView.setLayoutManager(LinearLayoutManager(this))

        val displayMetrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels - ResourceUtil.dpToPx(240)

        val params = userRecyclerView.layoutParams as ConstraintLayout.LayoutParams
        params.matchConstraintMaxHeight = height
        userRecyclerView.layoutParams = params


        // event handlers
        inviteButton.setOnClickListener { onClickInvite () }
    }

    private fun setReactiveX () {
        disposable.add(
            RxTextView.textChanges(searchEditText)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { searchName ->
                    val searchKey = searchName.trim().toString()
                    if (searchKey.length >= 3) {
                        if (selectType == HLBaseUserSelectType.SERVICE_PROVIDER) {
                            searchServiceProviders(searchKey)
                        } else {
                            searchHorseManagers(searchKey)
                        }
                    }
                }
        )
    }

    /**
     *  Search User Handlers
     */
    private fun searchHorseManagers (searchKey: String, isRefresh: Boolean = true) {
        val lastUserId = if (isRefresh || adapter.allData.isEmpty()) "" else adapter.allData.last().userId

        HLFirebaseService.instance.searchHorseManagers(searchKey, lastUserId, excludeUsers, object: ResponseCallback<List<HLHorseManagerModel>> {
            override fun onSuccess(data: List<HLHorseManagerModel>) {
                if (isRefresh) {
                    adapter.clear()
                }
                adapter.addAll(data)

                isLoadMore = data.size.toLong() == HLConstants.LIMIT_HORSE_MANAGERS
            }

            override fun onFailure(error: String) {
                showErrorMessage(error)
            }
        })
    }

    private fun searchServiceProviders (searchKey: String, isRefresh: Boolean = true) {
        val lastUserId = if (isRefresh || adapter.allData.isEmpty()) "" else adapter.allData.last().userId

//        userRecyclerView?.setRefreshing(true)
        HLFirebaseService.instance.searchServiceProviders(searchKey, lastUserId, excludeUsers, object: ResponseCallback<List<HLServiceProviderModel>> {
            override fun onSuccess(data: List<HLServiceProviderModel>) {
                if (isRefresh) {
                    adapter.clear()
                }
                adapter.addAll(data)

                isLoadMore = data.size.toLong() == HLConstants.LIMIT_HORSE_MANAGERS
            }

            override fun onFailure(error: String) {
                showErrorMessage(error)
            }
        })
    }

    /**
     *  My Service Providers Handler
     */
    private fun initMyServiceProviderRecyclerView () {
        providerAdapter = HLBaseUserAdapter(this).apply {
            setOnItemClickListener { position ->
                onSelectUser(getItem(position))
            }
        }
        serviceProviderRecyclerView.adapter = providerAdapter
        serviceProviderRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun getMyServiceProviders () {
        HLGlobalData.me?.uid?.let { userId ->
            HLFirebaseService.instance.getHorseManagerProviders(userId, object: ResponseCallback<List<HLHorseManagerProviderModel>> {
                override fun onSuccess(data: List<HLHorseManagerProviderModel>) {
                    providerAdapter?.clear()

                    val userIds = data.map { it.userId }.distinct().sorted()
                    userIds.forEach { userId ->
                        for (user in data) {
                            if (userId == user.userId) {
                                providerAdapter?.add(user)
                                break
                            }
                        }
                    }
                }

                override fun onFailure(error: String) {
                    showErrorMessage(error)
                }
            })
        }
    }
}
