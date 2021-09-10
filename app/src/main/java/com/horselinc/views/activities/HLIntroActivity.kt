package com.horselinc.views.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.horselinc.*
import com.horselinc.firebase.HLFirebaseService
import com.horselinc.views.adapters.viewpager.HLIntroViewPagerAdapter
import kotlinx.android.synthetic.main.activity_intro.*

class HLIntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        initActivity()
    }

    private fun initActivity () {
        // initialize controllers
        leftArrowImageView.visibility = View.GONE
        continueButton.visibility = View.GONE

        viewPager.adapter = HLIntroViewPagerAdapter(this)
        viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) { }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }

            override fun onPageSelected(position: Int) {
                leftArrowImageView.visibility = if (position == 0) View.GONE else View.VISIBLE
                continueButton.visibility = if (position == 2) View.VISIBLE else View.GONE
                pageIndicator.selection = position
            }
        })

        // event handler
        skipButton.setOnClickListener { finishIntro () }
        continueButton.setOnClickListener { finishIntro() }
        leftArrowImageView.setOnClickListener {
            selectIntroPage (if (viewPager.currentItem == 0) 0 else viewPager.currentItem.dec())
        }
        rightArrowImageView.setOnClickListener {
            selectIntroPage (if (viewPager.currentItem == 2) 2 else viewPager.currentItem.inc())
        }
    }

    private fun selectIntroPage (position: Int) {
        viewPager.setCurrentItem(position, true)
        pageIndicator.selection = position
    }

    private fun finishIntro () {
        // save preference
        App.preference.put(PreferenceKey.FIRST_LAUNCH, false)

        // start activity
        val userId = if (!HLFirebaseService.instance.isAuthorized) "" else HLGlobalData.me?.uid ?: ""
        val cls = when {
            userId.isEmpty() -> HLAuthActivity::class.java
            HLGlobalData.me?.type == null -> HLUserRoleActivity::class.java
            HLGlobalData.me?.type == HLUserType.MANAGER -> HLHorseManagerMainActivity::class.java
            else -> HLServiceProviderMainActivity::class.java
        }

        startActivity(Intent(this, cls))
        finish()
    }
}
