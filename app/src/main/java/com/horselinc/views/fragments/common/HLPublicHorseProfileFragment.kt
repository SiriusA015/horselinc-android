package com.horselinc.views.fragments.common


import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horselinc.R
import com.horselinc.loadImage
import com.horselinc.models.data.HLHorseModel
import com.horselinc.utils.ResourceUtil
import com.horselinc.views.adapters.recyclerview.HLProviderHorseDetailAdapter
import com.horselinc.views.fragments.HLBaseFragment
import com.jude.easyrecyclerview.decoration.DividerDecoration
import com.makeramen.roundedimageview.RoundedImageView

class HLPublicHorseProfileFragment(private val horse: HLHorseModel) : HLBaseFragment() {

    private var horseImageView: RoundedImageView? = null
    private var barnNameTextView: TextView? = null
    private var showNameTextView: TextView? = null
    private var userRecyclerView: RecyclerView? = null
    private var detailTextView: TextView? = null

    private lateinit var userAdapter: HLProviderHorseDetailAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        rootView?:let {
            rootView = inflater.inflate(R.layout.fragment_public_horse_profile, container, false)

            initControls ()

            setHorseData ()
        }

        return rootView
    }


    /**
     *  Initialize Handles
     */
    private fun initControls () {
        // controls
        horseImageView = rootView?.findViewById(R.id.horseImageView)
        barnNameTextView = rootView?.findViewById(R.id.barnNameTextView)
        showNameTextView = rootView?.findViewById(R.id.showNameTextView)
        userRecyclerView = rootView?.findViewById(R.id.userRecyclerView)
        detailTextView = rootView?.findViewById(R.id.detailTextView)

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
    }

    /**
     *  Horse Data Handlers
     */
    @SuppressLint("SetTextI18n")
    private fun setHorseData () {
        // horse profile
        horseImageView?.loadImage(horse.avatarUrl, R.drawable.ic_horse_placeholder)
        barnNameTextView?.text = horse.barnName
        showNameTextView?.text = horse.displayName

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
        // gender
        var info = "${horse.gender}\n"

        // registration
        horse.registrations?.let {
            val registrationString = ArrayList<String>()
            it.forEach { registration ->
                if (registration.name.isNotEmpty()) {
                    registrationString.add("${registration.name} #${registration.number}")
                }
            }

            info += "\n${registrationString.joinToString("\n")}\n"
        }

        // description
        info += "\n${horse.description}"
    }
}
