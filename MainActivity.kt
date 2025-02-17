package www.weybackmachine.com.myapp

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var linkHandler: LinkHandler
    private var fullScreenView: View? = null
    private var isFullScreen = false
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_SWIPE)
            window.statusBarColor = resources.getColor(android.R.color.black)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            window.statusBarColor = resources.getColor(android.R.color.black)
        }

        linkHandler = LinkHandler(this)
        val webView: WebView = findViewById(R.id.webView)

        // Configurações do WebView
        webView.settings.apply {
            javaScriptEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
        }

        // Definir User-Agent do Opera
        val operaUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36 OPR/80.0.4170.64"
        webView.settings.userAgentString = operaUserAgent

        // Tornar o fundo da WebView preto
        webView.setBackgroundColor(resources.getColor(android.R.color.black))

        // Injetar código CSS para páginas com fundo preto
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                val js = "document.body.style.backgroundColor = 'black'; document.body.style.color = 'white';"
                view.evaluateJavascript(js, null)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (linkHandler.isBlocked(url)) {
                    linkHandler.showConfirmationDialog(url)
                    return true
                }
                view.loadUrl(url)
                return false
            }
        }

        // Definir WebChromeClient
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null) return
                fullScreenView = view
                customViewCallback = callback
                val decorView = window.decorView as FrameLayout
                decorView.addView(view)
                isFullScreen = true
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                supportActionBar?.hide()
            }

            override fun onHideCustomView() {
                exitFullScreen()
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimetype)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Baixando arquivo")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
            request.setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(url, contentDisposition, mimetype)
            )
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(this, "Iniciando download...", Toast.LENGTH_SHORT).show()
        }

        webView.loadUrl("https://www.flaticon.com/br/icone-gratis/escudo_3329139?term=escudo&related_id=3329139")
    }

    private fun exitFullScreen() {
        if (isFullScreen) {
            val decorView = window.decorView as FrameLayout
            decorView.removeView(fullScreenView)
            fullScreenView = null
            isFullScreen = false
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            supportActionBar?.show()
            customViewCallback?.onCustomViewHidden()
        }
    }

    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webView)
        if (isFullScreen) {
            exitFullScreen()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
