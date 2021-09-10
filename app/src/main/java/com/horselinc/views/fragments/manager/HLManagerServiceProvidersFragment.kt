package com.horselinc.views.fragments.manager


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.HLGlobalData
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseManagerProviderModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLManagerServiceProviderAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLManagerServiceProviderItemListener
import com.jude.easyrecyclerview.decoration.SpaceDecoration

class HLManagerServiceProvidersFragment : HLBaseFragment() {

    private var recyclerView: RecyclerView? = null

    private lateinit var adapter: HLManagerServiceProviderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_manager_service_providers, container, false)

            initControls ()
        }

        getServiceProviders ()

        return rootView
    }

    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // initialize recycler view
        recyclerView = rootView?.findViewById(R.id.recyclerView)

        adapter = HLManagerServiceProviderAdapter(activity, object: HLManagerServiceProviderItemListener {
            override fun onClickDelete(position: Int, provider: HLHorseManagerProviderModel) {
                removeProvider(provider)
            }
        })
        recyclerView?.run {
            this.adapter = this@HLManagerServiceProvidersFragment.adapter
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(SpaceDecoration(ResourceUtil.dpToPx(8)))
        }

        // event handlers
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        rootView?.findViewById<Button>(R.id.addButton)?.setOnClickListener { replaceFragment(HLManagerAddServiceProviderFragment()) }
    }

    /**
     *  Service Provider Handlers
     */
    private fun getServiceProviders () {
        HLGlobalData.me?.uid?.let { userId ->
            HLFirebaseService.instance.getHorseManagerProviders(userId, object: ResponseCallback<List<HLHorseManagerProviderModel>> {

                override fun onSuccess(data: List<HLHorseManagerProviderModel>) {
                    adapter.clear()

                    val serviceTypes = data.map { it.serviceType }.distinct().sorted()
                    serviceTypes.forEach { serviceType ->
                        adapter.add(data.filter { it.serviceType == serviceType })
                    }
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
        }
    }

    private fun removeProvider (provider: HLHorseManagerProviderModel) {
        HLFirebaseService.instance.deleteHorseManagerProvider(provider.uid, object: ResponseCallback<String> {
            override fun onSuccess(data: String) {
                val index = adapter.allData.indexOfFirst {  providers ->
                    providers.any { it.uid == provider.uid }
                }

                if (index >= 0) {
                    val newData = adapter.getItem(index).filter { it.uid != provider.uid }
                    if (newData.isEmpty()) {
                        adapter.remove(index)
                    } else {
                        adapter.update(newData, index)
                    }
                }
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        })
    }
}
