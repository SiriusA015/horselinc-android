package com.horselinc.views.fragments.common

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import com.horselinc.R
import com.horselinc.views.fragments.HLBaseFragment

/**
 *  Created by TengFei Li on 26, August, 2019
 */

class HLWebViewFragment(private val url: String) : HLBaseFragment() {

    private var webView: WebView? = null
    private var loadProgressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView ?: let {
            rootView = inflater.inflate(R.layout.fragment_web_view, container, false)
            initControls()
        }

        return rootView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initControls() {
        // controls
        loadProgressBar = rootView?.findViewById(R.id.loadProgressBar)
        webView = rootView?.findViewById(R.id.webView)

        loadProgressBar?.progress = 0

        webView?.run {
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    loadProgressBar?.progress = 0
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    loadProgressBar?.progress = newProgress
                    if (newProgress == 100) {
                        loadProgressBar?.progress = 0
                    }
                }
            }

            settings?.apply {
                javaScriptEnabled = true
//                loadWithOverviewMode = true
//                useWideViewPort = true
            }
        }

        webView?.loadUrl(url)

        // event handlers
        rootView?.findViewById<ImageButton>(R.id.btClose)?.setOnClickListener { popFragment() }
    }
}