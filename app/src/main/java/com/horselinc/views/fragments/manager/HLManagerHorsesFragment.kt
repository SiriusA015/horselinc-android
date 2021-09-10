package com.horselinc.views.fragments.manager


import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.HLConstants
import com.horselinc.HLGlobalData
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.hideKeyboard
import com.horselinc.models.data.HLHorseFilterModel
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.event.HLRefreshHorsesEvent
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLHorseAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.jude.easyrecyclerview.EasyRecyclerView
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.jude.easyrecyclerview.decoration.SpaceDecoration
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class HLManagerHorsesFragment : HLBaseFragment() {

    private var newHorseProfileButton: Button? = null
    private var horseRecyclerView: EasyRecyclerView? = null
    private var filterButton: ImageView? = null
    private var badgeImageView: ImageView? = null
    private var searchView: SearchView? = null

    private lateinit var horseAdapter: HLHorseAdapter
    private var horses = ArrayList<HLHorseModel>()

    private var filter: HLHorseFilterModel? = null
        set(value) {
            field = value

            badgeImageView?.visibility = if (value == null) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

    private var isHorseLoadMore = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_horses, container, false)

            EventBus.getDefault().register(this)

            // initialize controls
            initControls ()

            // get horses
            showProgressDialog()
            getHorses (isRefresh = true)
        }

        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun handleInternetAvailable() {
        super.handleInternetAvailable()
//        horseRecyclerView?.swipeToRefresh?.isEnabled = true
    }

    override fun handleInternetUnavailable() {
        super.handleInternetUnavailable()
        horseRecyclerView?.setRefreshing(false)
//        horseRecyclerView?.swipeToRefresh?.isEnabled = false
    }

    /**
     *  Event Bus Handlers
     */
    @Subscribe (threadMode = ThreadMode.MAIN)
    fun onReceivedRefreshHorsesEvent (event: HLRefreshHorsesEvent) {
        try {
            Handler().postDelayed({
                filter = event.filter
                getHorses(isRefresh = true)
            }, 500)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // variables
        filterButton = rootView?.findViewById(R.id.filterImageView)
        newHorseProfileButton = rootView?.findViewById(R.id.newHorseProfileButton)
        horseRecyclerView = rootView?.findViewById(R.id.horseRecyclerView)
        badgeImageView = rootView?.findViewById(R.id.badgeImageView)
        searchView = rootView?.findViewById(R.id.searchView)

        badgeImageView?.visibility = View.GONE

        // recycler view
        horseAdapter = HLHorseAdapter(activity).apply {
            setMore(R.layout.load_more_horse, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (isHorseLoadMore && isNetworkConnected) {
                        getHorses()
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() {}
            })

            setOnItemClickListener { position ->
                searchView?.clearFocus()
                replaceFragment(HLManagerHorseDetailFragment(horseAdapter.getItem(position)), R.id.mainContainer)
            }
        }

        horseRecyclerView?.run {
            recyclerView?.setPadding(0, 0, 0, ResourceUtil.dpToPx(92))
            recyclerView?.clipToPadding = false
            addItemDecoration(SpaceDecoration(ResourceUtil.dpToPx(4)))
            setLayoutManager(LinearLayoutManager(activity))
            setRefreshListener {
                if (isNetworkConnected) {
                    getHorses(isRefresh = true)
                } else {
                    setRefreshing(false)
                }
            }
        }


        // event handler
        filterButton?.setOnClickListener {
            replaceFragment(HLManagerHorsesFilterFragment (filter?.copy()), R.id.mainContainer)
        }
        newHorseProfileButton?.setOnClickListener { replaceFragment(HLManagerHorseProfileFragment (), R.id.mainContainer) }

        searchView?.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchHorse (query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchHorse (newText)
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
    }

    /**
     *  Others
     */
    private fun getHorses (isRefresh: Boolean = false) {

        hideKeyboard()
        searchView?.clearFocus()

        HLGlobalData.me?.let { me ->
            val lastHorseId = if (horses.size == 0 || isRefresh) "" else horses.last().uid

            HLFirebaseService.instance.searchHorsesForManager(me.uid, lastHorseId, filter, object: ResponseCallback<List<HLHorseModel>> {
                override fun onSuccess(data: List<HLHorseModel>) {
                    hideProgressDialog()

                    if (isRefresh) {
                        horses.clear()
                        horses.addAll(data)
                    }

                    searchView?.visibility = if (horses.isEmpty()) View.GONE else View.VISIBLE

                    isHorseLoadMore = data.size.toLong() == HLConstants.LIMIT_HORSES

                    searchHorse(searchView?.query?.toString())

                    // filter button
                    filterButton?.visibility = if (horseAdapter.allData.isEmpty()) View.INVISIBLE else View.VISIBLE
                }

                override fun onFailure(error: String) {
                    showError(message = error)
                }
            })
        }
    }

    @SuppressLint("DefaultLocale")
    private fun searchHorse (query: String?) {
        query?.let {
            horseAdapter.clear()

            if (it.isEmpty()) {
                horses.forEach { horse ->
                    horseAdapter.add(horse.copy())
                }
            } else {
                val filteredHorses = horses.filter { horse ->
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

                filteredHorses.forEach { filteredHorse ->
                    horseAdapter.add(filteredHorse.copy())
                }
            }

            if (horseAdapter.count == 0) {
                horseRecyclerView?.showEmpty()
            } else {
                horseRecyclerView?.adapter = horseAdapter
            }
        }
    }
}
