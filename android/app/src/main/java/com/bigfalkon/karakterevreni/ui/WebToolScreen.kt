package com.bigfalkon.karakterevreni.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream

/**
 * Panel araçlarını (admin paneli, rastgele seçici, yedekleme...) uygulama içinde
 * açan WebView ekranı. blob:/data: indirmeleri İndirilenler klasörüne yazılır,
 * dosya seçici alanları galeriyi açar.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebToolScreen(url: String, title: String, onClose: () -> Unit) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val chooser = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fileCallback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
        fileCallback = null
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Kapat")
                }
            },
            actions = {
                IconButton(onClick = { webViewRef?.reload() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Yenile")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
        )
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(BackgroundDark.toArgb())
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        addJavascriptInterface(WebToolBridge(ctx), "AndroidApp")
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val target = request.url
                                if (target.host == "bigfalkon.github.io") return false
                                runCatching {
                                    view.context.startActivity(
                                        Intent(Intent.ACTION_VIEW, target)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                                return true
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }

                            override fun onShowFileChooser(
                                view: WebView,
                                callback: ValueCallback<Array<Uri>>,
                                params: FileChooserParams
                            ): Boolean {
                                fileCallback?.onReceiveValue(null)
                                fileCallback = callback
                                return try {
                                    chooser.launch(params.createIntent())
                                    true
                                } catch (e: ActivityNotFoundException) {
                                    fileCallback = null
                                    false
                                }
                            }
                        }
                        setDownloadListener { dlUrl, _, contentDisposition, mimeType, _ ->
                            when {
                                dlUrl.startsWith("blob:") ->
                                    evaluateJavascript(blobToBridgeJs(dlUrl), null)
                                dlUrl.startsWith("data:") ->
                                    WebToolBridge(ctx).saveBase64(dlUrl, mimeType.orEmpty(), "")
                                else -> runCatching {
                                    val name = URLUtil.guessFileName(dlUrl, contentDisposition, mimeType)
                                    val request = DownloadManager.Request(Uri.parse(dlUrl)).apply {
                                        setMimeType(mimeType)
                                        setNotificationVisibility(
                                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                        )
                                        setDestinationInExternalPublicDir(
                                            Environment.DIRECTORY_DOWNLOADS, name
                                        )
                                    }
                                    (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
                                        .enqueue(request)
                                    Toast.makeText(ctx, "İndiriliyor…", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        loadUrl(url)
                        webViewRef = this
                    }
                }
            )
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }

    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) wv.goBack() else onClose()
    }

    DisposableEffect(Unit) {
        onDispose { webViewRef?.destroy() }
    }
}

/** blob: URL doğrudan indirilemez; sayfada base64'e çevrilip köprüden geçirilir. */
private fun blobToBridgeJs(blobUrl: String) = """
    (function(){
      fetch('$blobUrl').then(function(r){ return r.blob(); }).then(function(b){
        var fr = new FileReader();
        fr.onloadend = function(){ AndroidApp.saveBase64(fr.result, b.type || '', ''); };
        fr.readAsDataURL(b);
      });
    })();
""".trimIndent()

private class WebToolBridge(private val context: Context) {

    /** dataUrl: "data:<mime>;base64,..." — İndirilenler klasörüne yazar. */
    @JavascriptInterface
    fun saveBase64(dataUrl: String, mimeType: String, suggestedName: String) {
        runCatching {
            val comma = dataUrl.indexOf(',')
            if (comma < 0) return
            val meta = dataUrl.substring(5, comma)
            val mime = mimeType.ifBlank { meta.substringBefore(';') }
                .ifBlank { "application/octet-stream" }
            val bytes = if (meta.contains("base64")) {
                Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
            } else {
                Uri.decode(dataUrl.substring(comma + 1)).toByteArray()
            }
            val ext = when {
                mime.contains("json") -> "json"
                mime.contains("png") -> "png"
                mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
                mime.contains("webp") -> "webp"
                mime.contains("zip") -> "zip"
                mime.contains("csv") -> "csv"
                else -> "bin"
            }
            val name = suggestedName.substringAfterLast('/')
                .ifBlank { "karakter-evreni-${System.currentTimeMillis()}.$ext" }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val target = context.contentResolver
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
                context.contentResolver.openOutputStream(target)?.use { it.write(bytes) }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                FileOutputStream(File(dir, name)).use { it.write(bytes) }
            }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "İndirilenler klasörüne kaydedildi: $name",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
