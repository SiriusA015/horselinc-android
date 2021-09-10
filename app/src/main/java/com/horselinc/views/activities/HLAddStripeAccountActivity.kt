package com.horselinc.views.activities

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.horselinc.HLConstants
import com.horselinc.HLGlobalData
import com.horselinc.R
import kotlinx.android.synthetic.main.activity_add_stripe_account.*


class HLAddStripeAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_stripe_account)

        // initialize controls
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear_white)

        loadProgressBar.progress = 0
        initWebView ()

        HLGlobalData.me?.let {
            webView.loadUrl("${HLConstants.ADD_STRIPE_ACCOUNT_URL}${it.uid}")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView () {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadProgressBar.progress = 0
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url?.contains("horse-linc://") == true) {
                    setResult(RESULT_OK)
                    finish()
                    return true
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                loadProgressBar.progress = newProgress
                if (newProgress == 100) {
                    loadProgressBar.progress = 0
                }
            }
        }

        webView.settings.javaScriptEnabled = true
    }
}
