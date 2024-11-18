package com.sliver.sample.route.navigation.features.args.webpage

import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.toRoute
import com.sliver.sample.route.navigation.R
import com.sliver.sample.route.navigation.Root
import com.sliver.sample.route.navigation.printNavArgs

class WebPageActivity : AppCompatActivity() {
    private val viewModel by viewModels<WebPageViewModel> { WebPageViewModel }

    private val url by lazy { intent?.getStringExtra("url")!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_web_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        printNavArgs()

        val webPageRoute1 = viewModel.savedStateHandle.toRoute<Root.WebPage>()

        val webPageRoute2 = Root.WebPage(intent?.getStringExtra("url")!!)

        Log.e("TAG", "onCreate: $webPageRoute1")

        Log.e("TAG", "onCreate: $webPageRoute2")

        val webView = findViewById<WebView>(R.id.web_view)
        webView.loadUrl(url)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }
        val webSettings = webView.settings
        webSettings.setSupportZoom(true)
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.javaScriptEnabled = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.allowFileAccess = true
        webSettings.loadsImagesAutomatically = true
        webSettings.defaultTextEncodingName = "UTF-8"
        webSettings.domStorageEnabled = true
    }
}