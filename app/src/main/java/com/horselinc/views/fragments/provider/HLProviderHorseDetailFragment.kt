package com.horselinc.views.fragments.provider


import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseModel
import com.horselinc.models.event.HLUpdateHorsePrivateNoteEvent
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLProviderHorseDetailAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.horselinc.views.fragments.common.HLPublicUserProfileFragment
import com.jude.easyrecyclerview.decoration.DividerDecoration
import com.makeramen.roundedimageview.RoundedImageView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class HLProviderHorseDetailFragment(private val horse: HLHorseModel) : HLBaseFragment() {

    private var horseImageView: RoundedImageView? = null
    private var barnNameTextView: TextView? = null
    private var displayNameTextView: TextView? = null

    private var userRecyclerView: RecyclerView? = null

    private var detailTextView: TextView? = null
    private var noteTitleTextView: TextView? = null
    private var noteTextView: TextView? = null
    private var additionalTextView: TextView? = null
    private var privateNoteTextView: TextView? = null

    private lateinit var userAdapter: HLProviderHorseDetailAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_provider_horse_detail, container, false)

            EventBus.getDefault().register(this)

            initControls()

            setHorseData ()
        }

        return rootView
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    /**
     *  Event Bus Handlers
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedUpdatePrivateNoteEvent (event: HLUpdateHorsePrivateNoteEvent) {
        try {
            horse.privateNote = event.data
            privateNoteTextView?.text = horse.privateNote
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     *  Initialize Handlers
     */
    private fun initControls () {
        // controls
        horseImageView = rootView?.findViewById(R.id.horseProfileImageView)
        barnNameTextView = rootView?.findViewById(R.id.barnNameTextView)
        displayNameTextView = rootView?.findViewById(R.id.displayNameTextView)
        userRecyclerView = rootView?.findViewById(R.id.userRecyclerView)
        detailTextView = rootView?.findViewById(R.id.detailTextView)
        noteTitleTextView = rootView?.findViewById(R.id.noteTitleTextView)
        noteTextView = rootView?.findViewById(R.id.noteTextView)
        additionalTextView = rootView?.findViewById(R.id.additionalTextView)
        privateNoteTextView = rootView?.findViewById(R.id.privateNoteTextView)

        // recycler view
        userAdapter = HLProviderHorseDetailAdapter(activity).apply {
            setOnItemClickListener { position ->
                replaceFragment(HLPublicUserProfileFragment(getItem(position)))
            }
        }
        userRecyclerView?.run {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)

            val itemDecoration = DividerDecoration(
                Color.parseColor("#1e000000"),
                ResourceUtil.dpToPx(1),
                0,
                0
            )
            itemDecoration.setDrawLastItem(false)
            itemDecoration.setDrawHeaderFooter(false)
            addItemDecoration(itemDecoration)
        }


        // event handlers
        rootView?.findViewById<ImageView>(R.id.backImageView)?.setOnClickListener { popFragment() }
        rootView?.findViewById<ImageView>(R.id.editPrivateNotesImageView)?.setOnClickListener { onClickPrivateNotes () }
        rootView?.findViewById<Button>(R.id.createInvoiceButton)?.setOnClickListener { onClickCreateInvoice () }
    }


    /**
     *  Event Handlers
     */
    private fun onClickPrivateNotes () {
        replaceFragment(HLProviderPrivateNoteFragment(horse))
    }

    private fun onClickCreateInvoice () {
        replaceFragment(HLProviderCreateInvoiceFragment(horse))
    }

    /**
     *  Horse Data Handlers
     */
    @SuppressLint("SetTextI18n")
    private fun setHorseData () {
        // horse profile
        horseImageView?.loadImage(horse.avatarUrl, R.drawable.ic_horse_placeholder)
        barnNameTextView?.text = horse.barnName
        displayNameTextView?.text = "\"${horse.displayName}\""

        userAdapter.clear()

        // horse user
        horse.trainer?.let {
            it.userType = "TRAINER"
            userAdapter.add(it)
        }

        horse.leaser?.let {
            it.userType = "LEASED TO"
            userAdapter.add(it)
        }

        horse.owners?.let {
            it.forEach { owner ->
                owner.userType = "OWNER"
                userAdapter.add(owner)
            }
        }

        // horse detail
        var detailInfo = ""
        if (horse.gender.isNotEmpty()) {
            horse.birthYear?.let { birthYear ->
                detailInfo = if (birthYear > 0) {
                    "${horse.gender}, Born $birthYear"
                } else {
                    horse.gender
                }
            }
        } else {
            horse.birthYear?.let { birthYear ->
                detailInfo = "Born $birthYear"
            }
        }
        detailTextView?.text = detailInfo

        // horse manager note
        if ((horse.description ?: "").isEmpty()) {
            noteTitleTextView?.visibility = View.GONE
            noteTextView?.visibility = View.GONE
        } else {
            noteTitleTextView?.visibility = View.VISIBLE
            noteTextView?.visibility = View.VISIBLE
            noteTextView?.text = horse.description
        }

        // horse additional
        val infos = ArrayList<String>()
        val tmpInfos = ArrayList<String>()

        // registrations
        horse.registrations?.let { registrations ->
            registrations.forEach { registration ->
                if (registration.name.isNotEmpty() && registration.number.isNotEmpty()) {
                    tmpInfos.add("${registration.name} #${registration.number}")
                }
            }

            if (tmpInfos.isNotEmpty()) {
                infos.add(tmpInfos.joinToString("\n"))
            }
        }

        // sire / dam
        tmpInfos.clear()
        if ((horse.sire ?: "").isNotEmpty()) {
            tmpInfos.add("Sire: ${horse.sire}")
        }
        if ((horse.dam ?: "").isNotEmpty()) {
            tmpInfos.add ("Dam: ${horse.dam}")
        }
        if (tmpInfos.isNotEmpty()) {
            infos.add(tmpInfos.joinToString("\n"))
        }

        // height / color
        tmpInfos.clear()
        horse.height?.let {
            if (it > 0) {
                tmpInfos.add("Height: ${String.format("%.2f", it)} Hands")
            }
        }
        if ((horse.color ?: "").isNotEmpty()) {
            tmpInfos.add ("Color: ${horse.color}")
        }
        if (tmpInfos.isNotEmpty()) {
            infos.add(tmpInfos.joinToString("\n"))
        }

        if (infos.isEmpty()) {
            additionalTextView?.text = "N/A"
        } else {
            additionalTextView?.text = infos.joinToString("\n\n")
        }

        // horse private note
        privateNoteTextView?.text = horse.privateNote
    }
}
