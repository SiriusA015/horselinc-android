package com.horselinc.views.fragments.manager


import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.firebase.ResponseCallback
import com.horselinc.models.data.HLHorseManagerModel
import com.horselinc.models.data.HLProviderHorseModel
import com.horselinc.views.adapters.recyclerview.HLSpinnerHorseUserAdapter
import com.horselinc.views.listeners.HLSpinnerDialogListener
import com.jude.easyrecyclerview.EasyRecyclerView
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter

class HLManagerHorseUserSpinnerDialogFragment(private val searchType: String,
                                              private val isContainMe: Boolean = false,
                                              private val selectedUser: HLHorseManagerModel? = null,
                                              private val listener: HLSpinnerDialogListener) : DialogFragment() {

    private lateinit var contentView: View
    private lateinit var userRecyclerView: EasyRecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var negativeButton: Button
    private lateinit var positiveButton: Button

    private lateinit var userAdapter: HLSpinnerHorseUserAdapter
    private var selectedIndex: Int = 0

    private var isLoadMore: Boolean = true

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity!!)

        contentView = LayoutInflater.from(context).inflate(R.layout.fragment_manager_horse_user_spinner_dialog, null)
        dialog.setContentView(contentView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // initialize controls
        initControls ()

        // get horse users
        progressBar.visibility = View.VISIBLE
        userRecyclerView.visibility = View.INVISIBLE
        negativeButton.isEnabled = false
        positiveButton.isEnabled = false

        if (searchType == HLHorseUserSearchType.MANAGER) {
            getHorseUsersForHorse ()
        } else {
            getHorseUsers ()
        }


        isCancelable = false

        return dialog
    }

    private fun initControls () {
        // variables
        userRecyclerView = contentView.findViewById(R.id.userRecyclerView)
        progressBar = contentView.findViewById(R.id.progressBar)
        negativeButton = contentView.findViewById(R.id.negativeButton)
        positiveButton = contentView.findViewById(R.id.positiveButton)

        // initialize title
        contentView.findViewById<TextView>(R.id.titleTextView).text = when (searchType) {
            HLHorseUserSearchType.OWNER -> getString(R.string.select_owner)
            HLHorseUserSearchType.TRAINER -> getString(R.string.select_trainer)
            else -> getString(R.string.select_manager)
        }

        // initialize recycler view
        userAdapter = HLSpinnerHorseUserAdapter(context, selectedIndex).apply {
            setMore(R.layout.load_more_user, object: RecyclerArrayAdapter.OnMoreListener {
                override fun onMoreShow() {
                    if (isLoadMore) {
                        if (searchType == HLHorseUserSearchType.MANAGER) {
                            getHorseUsersForHorse ()
                        } else {
                            getHorseUsers ()
                        }
                    } else {
                        stopMore()
                    }
                }

                override fun onMoreClick() {}
            })

            setOnItemClickListener {
                selectedIndex = it
            }
        }
        userRecyclerView.adapter = userAdapter
        userRecyclerView.setLayoutManager(LinearLayoutManager(context))


        // event handler
        negativeButton.setOnClickListener { dismiss() }
        positiveButton.setOnClickListener {
            listener.onClickPositive(selectedIndex, userAdapter.getItem(selectedIndex))
            dismiss()
        }
    }

    private fun getHorseUsers () {
        HLGlobalData.me?.let { me ->
            val lastUserId = if (userAdapter.count == 0) "" else userAdapter.getItem(userAdapter.count - 1).userId

            val excludeIds = if (!isContainMe) {
                arrayListOf(me.uid)
            } else {
                null
            }

            HLFirebaseService.instance.searchHorseUsers(me.uid, searchType, lastUserId, excludeIds,
                callback = object: ResponseCallback<List<HLHorseManagerModel>> {
                    override fun onSuccess(data: List<HLHorseManagerModel>) {
                        progressBar.visibility = View.GONE
                        userRecyclerView.visibility = View.VISIBLE
                        negativeButton.isEnabled = true
                        positiveButton.isEnabled = true

                        if (lastUserId.isEmpty() && data.isNotEmpty()) {
                            userAdapter.add(HLHorseManagerModel())
                        }
                        userAdapter.addAll(data)

                        isLoadMore = data.size.toLong() == HLConstants.LIMIT_HORSE_USERS

                        // set selected index
                        userAdapter.selectedPosition = if (selectedUser == null) 0 else userAdapter.allData.indexOfFirst { user -> user.userId == selectedUser.userId }
                    }

                    override fun onFailure(error: String) {
                        showErrorMessage(error)

                        progressBar.visibility = View.GONE
                        userRecyclerView.visibility = View.VISIBLE
                        negativeButton.isEnabled = true
                        positiveButton.isEnabled = true
                    }
                })
        }
    }

    private fun getHorseUsersForHorse () {
        HLGlobalData.me?.uid?.let { userId ->
            HLFirebaseService.instance.searchHorsesForProvider(userId, callback = object: ResponseCallback<List<HLProviderHorseModel>> {

                override fun onSuccess(data: List<HLProviderHorseModel>) {
                    progressBar.visibility = View.GONE
                    userRecyclerView.visibility = View.VISIBLE
                    negativeButton.isEnabled = true
                    positiveButton.isEnabled = true

                    userAdapter.add(HLHorseManagerModel())
                    userAdapter.addAll(data.map { it.manager })
                    isLoadMore = false

                    // set selected index
                    userAdapter.selectedPosition = if (selectedUser == null) 0 else userAdapter.allData.indexOfFirst { user -> user.userId == selectedUser.userId }
                }

                override fun onFailure(error: String) {
                    showErrorMessage(error)

                    progressBar.visibility = View.GONE
                    userRecyclerView.visibility = View.VISIBLE
                    negativeButton.isEnabled = true
                    positiveButton.isEnabled = true
                }
            })
        }
    }
}
