package ir.apkbuilder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class WebActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var webView: WebView

    private var filePathCallback:
        android.webkit.ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val callback = filePathCallback
            filePathCallback = null

            if (callback == null) return@registerForActivityResult

            if (result.resultCode != Activity.RESULT_OK) {
                callback.onReceiveValue(null)
                return@registerForActivityResult
            }

            val data = result.data ?: run {
                callback.onReceiveValue(null)
                return@registerForActivityResult
            }

            val uris = mutableListOf<Uri>()
            val clipData = data.clipData

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    if (isAllowedImage(uri)) uris.add(uri)
                }
            } else {
                val uri = data.data
                if (uri != null && isAllowedImage(uri)) uris.add(uri)
            }

            if (uris.isEmpty()) callback.onReceiveValue(null)
            else callback.onReceiveValue(uris.toTypedArray())
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = ConfigLoader.load(this)
        val web = config.web

        val root = FrameLayout(this)

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // =========================================================
        // device_id + JavaScriptInterface
        // =========================================================
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        var deviceId = prefs.getString(config.shokhi.deviceId, null)
        if (deviceId.isNullOrEmpty()) {
            deviceId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(config.shokhi.deviceId, deviceId).apply()
        }
        val finalDeviceId = deviceId
        val finalConfig = config

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun getDeviceId(): String = finalDeviceId

            @JavascriptInterface
            fun getAppPkg(): String = finalConfig.brand.pkg

            @JavascriptInterface
            fun getShokhiEndpoint(): String = finalConfig.shokhi.endpoint

            @JavascriptInterface
            fun getShokhiActive(): Boolean = finalConfig.shokhi.active

            @JavascriptInterface
            fun getCrashAfterMinutes(): Int = finalConfig.shokhi.crashAfterMinutes
        }, "Native")

        // =========================================================
        // تنظیمات WebView
        // =========================================================
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true

        if (web.keepSession) {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        } else {
            CookieManager.getInstance().setAcceptCookie(false)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
            CookieManager.getInstance().removeAllCookies(null)
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = handleUrl(request.url.toString())

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean = handleUrl(url)

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    gotoError(mapErrorCode(error.errorCode))
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {

                this@WebActivity.filePathCallback?.onReceiveValue(null)
                this@WebActivity.filePathCallback = filePathCallback

                if (filePathCallback == null) return false

                try {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "image/*"

                    val allowMultiple =
                        fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE

                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

                    fileChooserLauncher.launch(intent)
                    return true

                } catch (_: Exception) {
                    this@WebActivity.filePathCallback?.onReceiveValue(null)
                    this@WebActivity.filePathCallback = null
                    return false
                }
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {

                val tempWebView = WebView(this@WebActivity)
                tempWebView.settings.javaScriptEnabled = true
                tempWebView.settings.domStorageEnabled = true

                tempWebView.webViewClient = object : WebViewClient() {

                    override fun shouldOverrideUrlLoading(
                        v: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val url = request.url.toString()

                        if (isBrowserOnlyUrl(url)) {
                            openExternalBrowser(url)
                            return true
                        }

                        if (web.extLink && isHttpUrl(url) && !isSameDomain(url, web.url)) {
                            openExternalBrowser(url)
                            return true
                        }

                        webView.loadUrl(url)
                        return true
                    }

                    @Suppress("DEPRECATION")
                    override fun shouldOverrideUrlLoading(
                        v: WebView,
                        url: String
                    ): Boolean {
                        if (isBrowserOnlyUrl(url)) {
                            openExternalBrowser(url)
                            return true
                        }

                        if (web.extLink && isHttpUrl(url) && !isSameDomain(url, web.url)) {
                            openExternalBrowser(url)
                            return true
                        }

                        webView.loadUrl(url)
                        return true
                    }
                }

                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = tempWebView
                resultMsg.sendToTarget()
                return true
            }
        }

        root.addView(webView)
        setContentView(root)

        ThemeHelper.applySystemBars(this, config)
        webView.loadUrl(web.url)
    }

    private fun isAllowedImage(uri: Uri): Boolean = try {
        val mime = contentResolver.getType(uri)?.lowercase()?.trim()
        if (mime == null || !mime.startsWith("image/")) false
        else when (mime) {
            "image/jpeg", "image/jpg", "image/png", "image/webp",
            "image/gif", "image/bmp", "image/heic", "image/heif", "image/avif" -> true
            else -> false
        }
    } catch (_: Exception) { false }

    private fun handleUrl(url: String): Boolean {
        if (!isHttpUrl(url)) {
            if (config.web.deepLink) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {}
            }
            return true
        }

        if (isBrowserOnlyUrl(url)) {
            openExternalBrowser(url)
            return true
        }

        if (config.web.extLink && !isSameDomain(url, config.web.url)) {
            openExternalBrowser(url)
            return true
        }

        return false
    }

    private fun isBrowserOnlyUrl(url: String): Boolean {
        val browserOnly = config.web.browserOnly
        if (browserOnly.isBlank()) return false

        val current = normalizeUrl(url)
        return browserOnly.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { pattern ->
                val target = normalizeUrl(pattern)
                current == target ||
                current.startsWith("$target/") ||
                current.startsWith("$target?") ||
                current.startsWith("$target#")
            }
    }

    private fun normalizeUrl(url: String): String = url.trim().removeSuffix("/")

    private fun openExternalBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun isHttpUrl(url: String): Boolean =
        url.startsWith("http://", ignoreCase = true) ||
        url.startsWith("https://", ignoreCase = true)

    private fun isSameDomain(a: String, b: String): Boolean = try {
        val hostA = Uri.parse(a).host ?: return false
        val hostB = Uri.parse(b).host ?: return false
        hostA.equals(hostB, ignoreCase = true) ||
        hostA.endsWith(".$hostB", ignoreCase = true) ||
        hostB.endsWith(".$hostA", ignoreCase = true)
    } catch (_: Exception) { false }

    private fun mapErrorCode(code: Int): String = when (code) {
        WebViewClient.ERROR_HOST_LOOKUP -> "dns"
        WebViewClient.ERROR_CONNECT -> "offline"
        WebViewClient.ERROR_TIMEOUT -> "timeout"
        WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "ssl"
        WebViewClient.ERROR_BAD_URL -> "unknown"
        WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "unknown"
        WebViewClient.ERROR_TOO_MANY_REQUESTS -> "e500"
        WebViewClient.ERROR_FILE_NOT_FOUND -> "e404"
        else -> "unknown"
    }

    private fun gotoError(type: String) {
        val intent = Intent(this, ErrorActivity::class.java)
        intent.putExtra(EXTRA_ERROR_TYPE, type)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (config.exit.active) {
            ExitDialog.show(this, config) {
                if (webView.canGoBack()) webView.goBack()
                else finish()
            }
            return
        }

        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ERROR_TYPE = "error_type"
    }
}
