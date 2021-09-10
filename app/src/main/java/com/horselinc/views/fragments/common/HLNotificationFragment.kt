package com.horselinc.views.fragments.common


import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.HLConstants
import com.horselinc.HLGlobalData
import com.horselinc.R
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLNotificationModel
import com.horselinc.models.event.HLUpdateNotificationCountEvent
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLNotificationAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.listeners.HLNotificationItemListener
import com.jude.easyrecyclerview.EasyRecyclerView
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import com.jude.easyrecyclerview.decoration.SpaceDecoration
import com.loopeer.itemtouchhelperextension.ItemTouchHelperExtension
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HLNotificationFragment : HLBaseFragment() {

    private var headerTextView: TextView? = null
    private var notificationRecyclerView: EasyRecyclerView? = null

    private lateinit var notificationAdapter: HLNotificationAdapter
    private lateinit var itemTouchHelper: ItemTouchHelperExtension

    private var shouldLoadMore = true
    private var isLoading = false
    private var unreadCount: Int = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_notification, container, false)

            initControl ()

            // get notifications
            showProgressDialog()
            getNotifications (isRefresh = true)

            // get unread notifications count
            getUnreadCount ()

            // event bus
            EventBus.getDefault().register(this)
        }
        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun handleInternetAvailable() {
        super.handleInternetAvailable()
//        notificationRecyclerView?.swipeToRefresh?.isEnabled = true
    }

    override fun handleInternetUnavailable() {
        super.handleInternetUnavailable()
        notificationRecyclerView?.setRefreshing(false)
//        notificationRecyclerView?.swipeToRefresh?.isEnabled = false
    }

    /**
     *  Event Bus Handlers
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedUpdateNotificationCountEvent (event: HLUpdateNotificationCountEvent) {
        try {
            getUnreadCount()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     *  Initialize Handlers
     */
    private fun initControl () {
        // controls
        headerTextView = rootView?.findViewById(R.id.headerTextView)
        notificationRecyclerView = rootView?.findViewById(R.id.notificationRecyclerView)

        // initialize recycler view
        notificationAdapter = HLNotificationAdapter(activity, object: HLNotificationItemListener {
            override fun onClickDelete(position: Int, data: HLNotificationModel) {
                deleteNotification (data)
            }
        }).apply {
            setMore(R.layout.load_more, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (shouldLoadMore && isNetworkConnected) {
                        getNotifications(isRefresh = false)
                        getUnreadCount ()
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() { }
            })
        }

        notificationRecyclerView?.run {
            adapter = notificationAdapter
            setLayoutManager(LinearLayoutManager(activity))
            addItemDecoration(SpaceDecoration(ResourceUtil.dpToPx(8)))
            setRefreshListener {
                if (isNetworkConnected) {
                    getNotifications (isRefresh = true)
                    getUnreadCount ()
                } else {
                    setRefreshing(false)
                }
            }
        }

        itemTouchHelper = ItemTouchHelperExtension(object: ItemTouchHelperExtension.Callback() {
            override fun onMove(
                p0: RecyclerView?,
                p1: RecyclerView.ViewHolder?,
                p2: RecyclerView.ViewHolder?
            ): Boolean {
                return true
            }

            override fun onSwiped(p0: RecyclerView.ViewHolder?, p1: Int) { }

            override fun getMovementFlags(p0: RecyclerView?, p1: RecyclerView.ViewHolder?): Int {
                return makeMovementFlags(0, ItemTouchHelper.START)
            }

            override fun isLongPressDragEnabled(): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas?,
                recyclerView: RecyclerView?,
                viewHolder: RecyclerView.ViewHolder?,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val holder = viewHolder as HLNotificationAdapter.HLNotificationViewHolder

                var diffX = dX
                if (diffX < -holder.actionContainer.width) {
                    diffX = -holder.actionContainer.width.toFloat()
                }

                holder.itemContainer.translationX = diffX
                return
            }
        })
        itemTouchHelper.attachToRecyclerView(notificationRecyclerView?.recyclerView)

        // event handlers
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
    }

    /**
     *  Notification Handlers
     */
    private fun getNotifications (isRefresh: Boolean) {

        if (isRefresh) {
            isLoading = false
            shouldLoadMore = false
        }

        if (isLoading) return
        isLoading = true
        val lastDocument = if (isRefresh) null else notificationAdapter.allData.last().documentSnapshot

        HLGlobalData.me?.uid?.let { userId ->
            HLFirebaseService.instance.getNotifications(userId, lastDocument, object: ResponseCallback<List<HLNotificationModel>> {
                override fun onSuccess(data: List<HLNotificationModel>) {

                    hideProgressDialog()

                    isLoading = false
                    shouldLoadMore = (data.size).toLong() == HLConstants.LIMIT_NOTIFICATIONS

                    if (isRefresh) {
                        notificationAdapter.clear()
                    }
                    notificationAdapter.addAll(data)
                }

                override fun onFailure(error: String) {
                    isLoading = false
                    shouldLoadMore = false
                    showError(error)
                }
            })
        }
    }

    private fun getUnreadCount () {
        HLGlobalData.me?.uid?.let {  userId ->
            HLFirebaseService.instance.getUnreadNotificationCount(userId, object: ResponseCallback<Int> {
                override fun onSuccess(data: Int) {
                    hideProgressDialog()
                    updateUnreadCount (data)
                }

                override fun onFailure(error: String) {
                    showError(error)
                }
            })
        }
    }

    private fun deleteNotification (notification: HLNotificationModel) {
        showProgressDialog()
        HLFirebaseService.instance.deleteNotification(notification.uid, object: ResponseCallback<String> {
            override fun onSuccess(data: String) {

                hideProgressDialog()

                val index = notificationAdapter.allData.indexOfFirst { it.uid == notification.uid }
                if (index >= 0) {
                    notificationAdapter.remove(index)
                }
            }

            override fun onFailure(error: String) {
                showError(error)
            }
        })
    }

    private fun updateUnreadCount (unreadCount: Int) {
        this.unreadCount = unreadCount

        headerTextView?.text = when (unreadCount) {
            0 -> "You read all messages"
            1 -> "1 unread message"
            else -> "$unreadCount unread messages"
        }
    }
}
