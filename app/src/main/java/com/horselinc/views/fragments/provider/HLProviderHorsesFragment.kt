package com.horselinc.views.fragments.provider


import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseFilterModel
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.data.HLProviderHorseModel
import com.horselinc.models.event.HLRefreshHorsesEvent
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLProviderHorseUserAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLProviderHorseItemListener
import com.jude.easyrecyclerview.EasyRecyclerView
import com.jude.easyrecyclerview.decoration.SpaceDecoration
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class HLProviderHorsesFragment : HLBaseFragment() {

    /**
     *  Controls
     */
    private var badgeImageView: ImageView? = null
    private var searchView: SearchView? = null
    private var userRecyclerView: EasyRecyclerView? = null


    /**
     *  Variables
     */
    private lateinit var horseUserAdapter: HLProviderHorseUserAdapter
    private val providerHorses = ArrayList<HLProviderHorseModel>()

    private var filter: HLHorseFilterModel? = null
        set(value) {
            field = value
            badgeImageView?.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_horses, container, false)

            EventBus.getDefault().register(this)

            // initialize controls
            initControls ()

            // get horses
            getHorses(true)
        }

        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun handleInternetAvailable() {
        super.handleInternetAvailable()
//        userRecyclerView?.swipeToRefresh?.isEnabled = true
    }

    override fun handleInternetUnavailable() {
        super.handleInternetUnavailable()
        userRecyclerView?.setRefreshing(false)
//        userRecyclerView?.swipeToRefresh?.isEnabled = false
    }

    /**
     *  Event Bus Handlers
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedRefreshHorsesEvent (event: HLRefreshHorsesEvent) {
        try {
            filter = event.filter
            getHorses()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     *  Initialize Controls
     */
    private fun initControls () {
        // controls
        badgeImageView = rootView?.findViewById(R.id.badgeImageView)
        userRecyclerView = rootView?.findViewById(R.id.userRecyclerView)
        searchView = rootView?.findViewById(R.id.searchView)

        badgeImageView?.visibility = View.GONE


        // recycler view
        horseUserAdapter = HLProviderHorseUserAdapter(activity, object: HLProviderHorseItemListener {
            override fun onClickHorse(position: Int, horse: HLHorseModel) {

                searchView?.clearFocus()

                replaceFragment(HLProviderHorseDetailFragment(horse.copy()), R.id.mainContainer)
            }
        })
        horseUserAdapter.clear()

        userRecyclerView?.run {
            setLayoutManager(LinearLayoutManager(activity))
            addItemDecoration(SpaceDecoration(ResourceUtil.dpToPx(4)))
            recyclerView?.setPadding(0, 0, 0, ResourceUtil.dpToPx(92))
            recyclerView?.clipToPadding = false

            setRefreshListener {
                if (isNetworkConnected) {
                    getHorses ()
                } else {
                    setRefreshing(false)
                }
            }
        }

        // search view
        searchView?.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchHorses(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchHorses(newText)
                return true
            }
        })

        searchView?.let { sv ->
            val searchTextView = sv.findViewById<View>(sv.context.resources.getIdentifier(
                "android:id/search_src_text",
                null,
                null
            )) as AutoCompleteTextView
            searchTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f)
        }


        // event handlers
        rootView?.findViewById<ImageView>(R.id.filterImageView)?.setOnClickListener {
            onClickFilter ()
        }
        rootView?.findViewById<Button>(R.id.inviteButton)?.setOnClickListener { onClickInvite () }
    }

    /**
     *  Event Handlers
     */
    private fun onClickFilter () {
        hideKeyboard()
        searchView?.setQuery("", true)
        searchView?.clearFocus()

        replaceFragment(HLProviderHorseFilterFragment(filter?.copy()), R.id.mainContainer)
    }

    private fun onClickInvite () {

        hideKeyboard()
        searchView?.clearFocus()

        HLGlobalData.me?.let { user ->
            activity?.let { act ->
                showProgressDialog()
                HLBranchService.createInviteLink(act, user.uid, HLUserType.PROVIDER, callback = object: ResponseCallback<String> {
                    override fun onSuccess(data: String) {
                        hideProgressDialog()

                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.type = "text/plain"
                        intent.data = Uri.parse("mailto:")
                        intent.putExtra(Intent.EXTRA_SUBJECT, "You're Invited To HorseLinc!")
                        intent.putExtra(Intent.EXTRA_TEXT, "${HLGlobalData.me?.serviceProvider?.name} wants you to join HorseLinc! You can download the app at $data")
                        startActivity(intent)
                    }

                    override fun onFailure(error: String) {
                        showError(error)
                    }
                })
            }
        }
    }


    /**
     *  Horses Handlers
     */
    private fun getHorses (showProgress: Boolean = false) {

        hideKeyboard()
        searchView?.clearFocus()

        HLGlobalData.me?.uid?.let { userId ->
            if (showProgress) {
                showProgressDialog()
            }

            HLFirebaseService.instance.searchHorsesForProvider(userId, filter, object: ResponseCallback<List<HLProviderHorseModel>> {
                override fun onSuccess(data: List<HLProviderHorseModel>) {
                    if (showProgress) {
                        hideProgressDialog()
                    }

                    providerHorses.clear()
                    providerHorses.addAll(data)

                    searchView?.visibility = if (providerHorses.isEmpty()) View.GONE else View.VISIBLE

                    searchHorses(searchView?.query?.toString())
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
        }
    }


    /**
     *  Search Handlers
     */
    @SuppressLint("DefaultLocale")
    private fun searchHorses (query: String?) {

        query?.let {

            horseUserAdapter.clear()
            horseUserAdapter.removeSelectedUsers()

            if (it.isEmpty()) {
                providerHorses.forEach { providerHorse ->
                    horseUserAdapter.add(providerHorse.copy())
                }
            } else {
                providerHorses.forEach { group ->
                    val horses = group.horses.filter { horse ->

                        (horse.barnName.toLowerCase().contains(it.toLowerCase())
                                || horse.displayName.toLowerCase().contains(it.toLowerCase())
                                || horse.trainer?.name?.toLowerCase()?.contains(it.toLowerCase()) == true
                                || horse.trainer?.barnName?.toLowerCase()?.contains(it.toLowerCase()) == true
                                || horse.owners?.map { owner -> owner.name }?.any { name -> name.toLowerCase().contains(it.toLowerCase()) } == true
                                || horse.owners?.map { owner -> owner.barnName }?.any { barnName -> barnName.toLowerCase().contains(it.toLowerCase()) } == true
                                || horse.leaser?.name?.toLowerCase()?.contains(it.toLowerCase()) == true
                                || horse.leaser?.barnName?.toLowerCase()?.contains(it.toLowerCase()) == true
                                || horse.privateNote?.contains(it.toLowerCase()) == true)

                    }

                    if (horses.isNotEmpty()) {
                        val horseGroup = HLProviderHorseModel()
                        horseGroup.manager = group.manager
                        horseGroup.horses = ArrayList()
                        horseGroup.horses.addAll(horses)
                        horseUserAdapter.add(horseGroup)
                        horseUserAdapter.addSelectedUser(horseGroup.manager)
                    }
                }
            }

            if (horseUserAdapter.count == 0) {
                userRecyclerView?.showEmpty()
            } else {
                userRecyclerView?.adapter = horseUserAdapter
            }
        }
    }
}