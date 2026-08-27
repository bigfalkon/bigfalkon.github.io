package com.bigfalkon.karakterevreni

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bigfalkon.karakterevreni.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    companion object {
        /** Uygulamanın açtığı site. */
        const val START_URL = "https://bigfalkon.github.io/index.html"
        private val APP_HOSTS = setOf("bigfalkon.github.io")
        private const val UA_SUFFIX = " KarakterEvreniApp/1.0"
        private const val BACK_EXIT_WINDOW_MS = 2000L
    }

    private lateinit var binding: ActivityMainBinding
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var lastBackPress = 0L
    private var pageReady = false

    private val fileChooser = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val cb = filePathCallback ?: return@registerForActivityResult
        filePathCallback = null
        cb.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !pageReady }
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupSwipeRefresh()
        binding.retryBtn.setOnClickListener { hideOffline(); binding.webView.reload() }
        setupBackHandling()

        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        } else {
            binding.webView.loadUrl(intent?.data?.takeIf { isAppUrl(it) }?.toString() ?: START_URL)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { if (isAppUrl(it)) binding.webView.loadUrl(it.toString()) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() = with(binding.webView) {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            // Sayfanın kendi responsive düzeni var; kullanıcı yine de sıkışırsa yakınlaştırabilsin.
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = userAgentString + UA_SUFFIX
        }
        isVerticalScrollBarEnabled = true
        overScrollMode = View.OVER_SCROLL_NEVER
        setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg))
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        addJavascriptInterface(NativeBridge(), "AndroidApp")
        webViewClient = AppWebViewClient()
        webChromeClient = AppChromeClient()
        setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            handleDownload(url, contentDisposition, mimeType)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }
    }

    private fun setupSwipeRefresh() = with(binding.swipeRefresh) {
        setColorSchemeColors(ContextCompat.getColor(context, R.color.primary))
        setProgressBackgroundColorSchemeColor(ContextCompat.getColor(context, R.color.surface))
        setOnRefreshListener { binding.webView.reload() }
        // Sayfanın en üstünde değilken çekerek yenilemeyi kapat; modal/scroll ile çakışmasın.
        binding.webView.viewTreeObserver.addOnScrollChangedListener {
            isEnabled = binding.webView.scrollY == 0
        }
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val web = binding.webView
                // Önce sayfadaki açık katmanı (modal/menü) kapatmayı dene.
                web.evaluateJavascript(
                    "(window.__androidBack && window.__androidBack()) ? 'handled' : 'no'"
                ) { res ->
                    if (res != null && res.contains("handled")) return@evaluateJavascript
                    if (web.canGoBack()) {
                        web.goBack()
                    } else {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPress < BACK_EXIT_WINDOW_MS) {
                            finish()
                        } else {
                            lastBackPress = now
                            toast(getString(R.string.exit_confirm))
                        }
                    }
                }
            }
        })
    }

    // ─── WebViewClient ────────────────────────────────────────────────────────
    private inner class AppWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            return when {
                isAppUrl(uri) -> false
                uri.scheme == "http" || uri.scheme == "https" -> { openExternal(uri); true }
                else -> { openWithSystem(uri); true }
            }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            binding.progress.visibility = View.VISIBLE
            hideOffline()
        }

        override fun onPageFinished(view: WebView, url: String?) {
            pageReady = true
            binding.progress.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            injectNativeUi(view)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (request.isForMainFrame) {
                pageReady = true
                showOffline()
            }
        }
    }

    // ─── WebChromeClient ──────────────────────────────────────────────────────
    private inner class AppChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            binding.progress.progress = newProgress
            if (newProgress >= 100) binding.progress.visibility = View.GONE
        }

        override fun onShowFileChooser(
            webView: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams
        ): Boolean {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = callback
            return try {
                fileChooser.launch(params.createIntent())
                true
            } catch (e: ActivityNotFoundException) {
                filePathCallback = null
                false
            }
        }
    }

    // ─── Native UI enjeksiyonu ────────────────────────────────────────────────
    /** Siteyi bozmadan, uygulama içinde mobil/native hissi veren küçük UI düzeltmeleri. */
    private fun injectNativeUi(view: WebView) {
        val css = readAsset("native-ui.css").replace("\n", " ").replace("'", "\\'")
        val js = readAsset("native-ui.js")
        view.evaluateJavascript(
            """
            (function(){
              if (!document.getElementById('android-native-ui')) {
                var s = document.createElement('style');
                s.id = 'android-native-ui';
                s.textContent = '$css';
                document.head.appendChild(s);
              }
              document.documentElement.classList.add('android-app');
            })();
            """.trimIndent(),
            null
        )
        view.evaluateJavascript(js, null)
    }

    private fun readAsset(name: String): String =
        assets.open(name).bufferedReader().use { it.readText() }

    // ─── İndirmeler ───────────────────────────────────────────────────────────
    private fun handleDownload(url: String, contentDisposition: String?, mimeType: String?) {
        when {
            url.startsWith("blob:") -> binding.webView.evaluateJavascript(blobFetchJs(url), null)
            url.startsWith("data:") -> saveDataUrl(url, mimeType)
            else -> {
                val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
                try {
                    val req = DownloadManager.Request(url.toUri()).apply {
                        setMimeType(mimeType)
                        addRequestHeader("User-Agent", binding.webView.settings.userAgentString)
                        setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        )
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                    }
                    (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                    toast(getString(R.string.download_started))
                } catch (e: Exception) {
                    toast(getString(R.string.download_failed))
                }
            }
        }
    }

    private fun saveDataUrl(dataUrl: String, mimeType: String?) {
        val comma = dataUrl.indexOf(',')
        if (comma < 0) return toast(getString(R.string.download_failed))
        val meta = dataUrl.substring(5, comma)
        val mime = mimeType?.takeIf { it.isNotBlank() } ?: meta.substringBefore(';').ifBlank { "application/octet-stream" }
        val bytes = if (meta.contains("base64")) {
            Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
        } else {
            Uri.decode(dataUrl.substring(comma + 1)).toByteArray()
        }
        saveToDownloads(bytes, defaultName(mime), mime)
    }

    private fun defaultName(mime: String): String {
        val ext = when {
            mime.contains("json") -> "json"
            mime.contains("png") -> "png"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("webp") -> "webp"
            mime.contains("csv") -> "csv"
            mime.contains("zip") -> "zip"
            else -> "bin"
        }
        return "karakter-evreni-${System.currentTimeMillis()}.$ext"
    }

    /** blob: URL'leri WebView'a indirtemeyiz; sayfada base64'e çevirip köprüden geçiriyoruz. */
    private fun blobFetchJs(blobUrl: String) = """
        (function(){
          fetch('$blobUrl').then(function(r){ return r.blob(); }).then(function(b){
            var fr = new FileReader();
            fr.onloadend = function(){
              AndroidApp.saveBase64(fr.result, b.type || '', '');
            };
            fr.readAsDataURL(b);
          }).catch(function(){ AndroidApp.downloadFailed(); });
        })();
    """.trimIndent()

    private fun saveToDownloads(bytes: ByteArray, fileName: String, mime: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return toast(getString(R.string.download_failed))
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                FileOutputStream(File(dir, fileName)).use { it.write(bytes) }
            }
            toast(getString(R.string.download_saved, fileName))
        } catch (e: Exception) {
            toast(getString(R.string.download_failed))
        }
    }

    // ─── JS köprüsü ───────────────────────────────────────────────────────────
    inner class NativeBridge {
        /** dataUrl: "data:<mime>;base64,..." — sayfadaki blob/data indirmeleri buradan geçer. */
        @JavascriptInterface
        fun saveBase64(dataUrl: String, mimeType: String, suggestedName: String) {
            runOnUiThread {
                val comma = dataUrl.indexOf(',')
                if (comma < 0) return@runOnUiThread toast(getString(R.string.download_failed))
                val mime = mimeType.ifBlank {
                    dataUrl.substring(5, comma).substringBefore(';')
                }.ifBlank { "application/octet-stream" }
                val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
                val name = suggestedName.substringAfterLast('/').ifBlank { defaultName(mime) }
                saveToDownloads(bytes, name, mime)
            }
        }

        @JavascriptInterface
        fun downloadFailed() {
            runOnUiThread { toast(getString(R.string.download_failed)) }
        }

        @JavascriptInterface
        fun openExternalUrl(url: String) {
            runOnUiThread { openExternal(url.toUri()) }
        }
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────────
    private fun isAppUrl(uri: Uri): Boolean =
        (uri.scheme == "https" || uri.scheme == "http") && uri.host in APP_HOSTS

    private fun openExternal(uri: Uri) {
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, uri)
        } catch (e: ActivityNotFoundException) {
            openWithSystem(uri)
        }
    }

    private fun openWithSystem(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            toast(getString(R.string.no_app_for_link))
        }
    }

    private fun showOffline() {
        binding.offlineView.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = false
        binding.progress.visibility = View.GONE
    }

    private fun hideOffline() {
        binding.offlineView.visibility = View.GONE
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
