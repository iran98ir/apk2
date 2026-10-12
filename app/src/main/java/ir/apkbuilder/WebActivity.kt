package ir.apkbuilder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.provider.OpenableColumns
import android.webkit.CookieManager
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

    // =========================================================
    // File Chooser
    // فقط تصویر
    // =========================================================

    private var filePathCallback:
        android.webkit.ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val callback = filePathCallback
            filePathCallback = null

            if (callback == null) {
                return@registerForActivityResult
            }

            if (result.resultCode != Activity.RESULT_OK) {
                callback.onReceiveValue(null)
                return@registerForActivityResult
            }

            val data = result.data

            if (data == null) {
                callback.onReceiveValue(null)
                return@registerForActivityResult
            }

            val uris = mutableListOf<Uri>()

            // انتخاب چند تصویر
            val clipData = data.clipData

            if (clipData != null) {

                for (i in 0 until clipData.itemCount) {

                    val uri = clipData
                        .getItemAt(i)
                        .uri

                    if (isAllowedImage(uri)) {
                        uris.add(uri)
                    }
                }

            } else {

                // انتخاب یک تصویر
                val uri = data.data

                if (uri != null && isAllowedImage(uri)) {
                    uris.add(uri)
                }
            }

            if (uris.isEmpty()) {
                callback.onReceiveValue(null)
            } else {
                callback.onReceiveValue(
                    uris.toTypedArray()
                )
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // =========================================================
        // خواندن کانفیگ
        // =========================================================

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

        settings.mixedContentMode =
            WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // =========================================================
        // target="_blank" / window.open
        // =========================================================

        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true

        // =========================================================
        // کوکی
        // =========================================================

        if (web.keepSession) {

            CookieManager
                .getInstance()
                .setAcceptCookie(true)

            CookieManager
                .getInstance()
                .setAcceptThirdPartyCookies(
                    webView,
                    true
                )

        } else {

            CookieManager
                .getInstance()
                .setAcceptCookie(false)

            CookieManager
                .getInstance()
                .setAcceptThirdPartyCookies(
                    webView,
                    false
                )

            CookieManager
                .getInstance()
                .removeAllCookies(null)
        }

        // =========================================================
        // WebViewClient
        // =========================================================

        webView.webViewClient =
            object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {

                    return handleUrl(
                        request.url.toString()
                    )
                }

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    url: String
                ): Boolean {

                    return handleUrl(url)
                }

                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: Bitmap?
                ) {
                    super.onPageStarted(
                        view,
                        url,
                        favicon
                    )
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {

                    super.onReceivedError(
                        view,
                        request,
                        error
                    )

                    if (request.isForMainFrame) {

                        gotoError(
                            mapErrorCode(
                                error.errorCode
                            )
                        )
                    }
                }
            }

        // =========================================================
        // WebChromeClient
        // =========================================================

        webView.webChromeClient =
            object : WebChromeClient() {

                // =====================================================
                // انتخاب فایل HTML
                // فقط IMAGE
                // =====================================================

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback:
                        android.webkit.ValueCallback<Array<Uri>>?,
                    fileChooserParams:
                        FileChooserParams?
                ): Boolean {

                    // اگر انتخاب قبلی باز بوده،
                    // آن را لغو می‌کنیم.
                    this@WebActivity
                        .filePathCallback
                        ?.onReceiveValue(null)

                    this@WebActivity
                        .filePathCallback =
                        filePathCallback

                    if (filePathCallback == null) {
                        return false
                    }

                    try {

                        val intent =
                            Intent(
                                Intent.ACTION_OPEN_DOCUMENT
                            )

                        intent.addCategory(
                            Intent.CATEGORY_OPENABLE
                        )

                        // =================================================
                        // مهم:
                        // فقط تصویر
                        // =================================================

                        intent.type = "image/*"

                        // اجازه انتخاب چند عکس در صورتی که
                        // HTML درخواست چند فایل کرده باشد.
                        val allowMultiple =
                            fileChooserParams
                                ?.mode ==
                                FileChooserParams.MODE_OPEN_MULTIPLE

                        intent.putExtra(
                            Intent.EXTRA_ALLOW_MULTIPLE,
                            allowMultiple
                        )

                        // =================================================
                        // فقط فایل‌های قابل خواندن
                        // =================================================

                        intent.addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        intent.addFlags(
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        )

                        fileChooserLauncher.launch(intent)

                        return true

                    } catch (_: Exception) {

                        this@WebActivity
                            .filePathCallback
                            ?.onReceiveValue(null)

                        this@WebActivity
                            .filePathCallback = null

                        return false
                    }
                }

                // =====================================================
                // target="_blank"
                // =====================================================

                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message
                ): Boolean {

                    val tempWebView =
                        WebView(this@WebActivity)

                    tempWebView.settings
                        .javaScriptEnabled = true

                    tempWebView.settings
                        .domStorageEnabled = true

                    tempWebView.webViewClient =
                        object : WebViewClient() {

                            override fun shouldOverrideUrlLoading(
                                v: WebView,
                                request: WebResourceRequest
                            ): Boolean {

                                val url =
                                    request.url.toString()

                                // browserOnly
                                if (isBrowserOnlyUrl(url)) {

                                    openExternalBrowser(url)

                                    return true
                                }

                                // خارجی
                                if (
                                    web.extLink &&
                                    isHttpUrl(url) &&
                                    !isSameDomain(
                                        url,
                                        web.url
                                    )
                                ) {

                                    openExternalBrowser(url)

                                    return true
                                }

                                // داخلی
                                webView.loadUrl(url)

                                return true
                            }

                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(
                                v: WebView,
                                url: String
                            ): Boolean {

                                if (
                                    isBrowserOnlyUrl(url)
                                ) {

                                    openExternalBrowser(url)

                                    return true
                                }

                                if (
                                    web.extLink &&
                                    isHttpUrl(url) &&
                                    !isSameDomain(
                                        url,
                                        web.url
                                    )
                                ) {

                                    openExternalBrowser(url)

                                    return true
                                }

                                webView.loadUrl(url)

                                return true
                            }
                        }

                    val transport =
                        resultMsg.obj
                            as WebView.WebViewTransport

                    transport.webView =
                        tempWebView

                    resultMsg.sendToTarget()

                    return true
                }
            }

        // =========================================================
        // اضافه کردن WebView
        // =========================================================

        root.addView(webView)

        setContentView(root)

        ThemeHelper.applySystemBars(
            this,
            config
        )

        // =========================================================
        // صفحه اصلی
        // =========================================================

        webView.loadUrl(web.url)
    }

    // =========================================================
    // بررسی اینکه URI واقعاً تصویر است
    // =========================================================

    private fun isAllowedImage(
        uri: Uri
    ): Boolean {

        return try {

            val resolver =
                contentResolver

            val mime =
                resolver.getType(uri)
                    ?.lowercase()
                    ?.trim()

            if (mime == null) {
                return false
            }

            // فقط MIME های image/*
            if (!mime.startsWith("image/")) {
                return false
            }

            // فرمت‌های مجاز
            when (mime) {

                "image/jpeg",
                "image/jpg",
                "image/png",
                "image/webp",
                "image/gif",
                "image/bmp",
                "image/heic",
                "image/heif",
                "image/avif" -> true

                else -> false
            }

        } catch (_: Exception) {

            false
        }
    }

    // =========================================================
    // URL
    // =========================================================

    private fun handleUrl(
        url: String
    ): Boolean {

        if (!isHttpUrl(url)) {

            if (config.web.deepLink) {

                try {

                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )
                    )

                } catch (_: Exception) {
                }
            }

            return true
        }

        if (isBrowserOnlyUrl(url)) {

            openExternalBrowser(url)

            return true
        }

        if (
            config.web.extLink &&
            !isSameDomain(
                url,
                config.web.url
            )
        ) {

            openExternalBrowser(url)

            return true
        }

        return false
    }

    // =========================================================
    // browserOnly
    // =========================================================

    private fun isBrowserOnlyUrl(
        url: String
    ): Boolean {

        val browserOnly =
            config.web.browserOnly

        if (browserOnly.isBlank()) {
            return false
        }

        val current =
            normalizeUrl(url)

        return browserOnly
            .split(",")
            .map {
                it.trim()
            }
            .filter {
                it.isNotEmpty()
            }
            .any { pattern ->

                val target =
                    normalizeUrl(pattern)

                current == target ||
                current.startsWith(
                    "$target/"
                ) ||
                current.startsWith(
                    "$target?"
                ) ||
                current.startsWith(
                    "$target#"
                )
            }
    }

    // =========================================================
    // نرمال‌سازی URL
    // =========================================================

    private fun normalizeUrl(
        url: String
    ): String {

        return url
            .trim()
            .removeSuffix("/")
    }

    // =========================================================
    // مرورگر خارجی
    // =========================================================

    private fun openExternalBrowser(
        url: String
    ) {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            intent.addCategory(
                Intent.CATEGORY_BROWSABLE
            )

            startActivity(intent)

        } catch (_: Exception) {
        }
    }

    // =========================================================
    // HTTP / HTTPS
    // =========================================================

    private fun isHttpUrl(
        url: String
    ): Boolean {

        return url.startsWith(
            "http://",
            ignoreCase = true
        ) ||
        url.startsWith(
            "https://",
            ignoreCase = true
        )
    }

    // =========================================================
    // دامنه
    // =========================================================

    private fun isSameDomain(
        a: String,
        b: String
    ): Boolean {

        return try {

            val hostA =
                Uri.parse(a).host
                    ?: return false

            val hostB =
                Uri.parse(b).host
                    ?: return false

            hostA.equals(
                hostB,
                ignoreCase = true
            ) ||
            hostA.endsWith(
                ".$hostB",
                ignoreCase = true
            ) ||
            hostB.endsWith(
                ".$hostA",
                ignoreCase = true
            )

        } catch (_: Exception) {

            false
        }
    }

    // =========================================================
    // خطاهای WebView
    // =========================================================

    private fun mapErrorCode(
        code: Int
    ): String {

        return when (code) {

            WebViewClient.ERROR_HOST_LOOKUP ->
                "dns"

            WebViewClient.ERROR_CONNECT ->
                "offline"

            WebViewClient.ERROR_TIMEOUT ->
                "timeout"

            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE ->
                "ssl"

            WebViewClient.ERROR_BAD_URL ->
                "unknown"

            WebViewClient.ERROR_UNSUPPORTED_SCHEME ->
                "unknown"

            WebViewClient.ERROR_TOO_MANY_REQUESTS ->
                "e500"

            WebViewClient.ERROR_FILE_NOT_FOUND ->
                "e404"

            else ->
                "unknown"
        }
    }

    // =========================================================
    // صفحه خطا
    // =========================================================

    private fun gotoError(
        type: String
    ) {

        val intent =
            Intent(
                this,
                ErrorActivity::class.java
            )

        intent.putExtra(
            EXTRA_ERROR_TYPE,
            type
        )

        startActivity(intent)

        finish()
    }

    // =========================================================
    // برگشت
    // =========================================================

    override fun onBackPressed() {

        if (config.exit.active) {

            ExitDialog.show(
                this,
                config
            ) {

                if (webView.canGoBack()) {

                    webView.goBack()

                } else {

                    finish()
                }
            }

            return
        }

        if (webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }

    // =========================================================
    // آزادسازی WebView
    // =========================================================

    override fun onDestroy() {

        // اگر انتخاب فایل باز مانده بود
        filePathCallback
            ?.onReceiveValue(null)

        filePathCallback = null

        webView.stopLoading()
        webView.destroy()

        super.onDestroy()
    }

    companion object {

        const val EXTRA_ERROR_TYPE =
            "error_type"
    }
}
