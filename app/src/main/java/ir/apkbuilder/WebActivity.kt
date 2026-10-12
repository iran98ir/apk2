package ir.apkbuilder

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class WebActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // خواندن کانفیگ app.json
        config = ConfigLoader.load(this)
        val web = config.web

        val root = FrameLayout(this)

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // =========================
        // تنظیمات WebView
        // =========================
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

        // پشتیبانی از target="_blank" و window.open
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true

        // =========================
        // کوکی‌ها
        // =========================
        if (web.keepSession) {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance()
                .setAcceptThirdPartyCookies(webView, true)
        } else {
            CookieManager.getInstance().setAcceptCookie(false)
            CookieManager.getInstance()
                .setAcceptThirdPartyCookies(webView, false)

            CookieManager.getInstance().removeAllCookies(null)
        }

        // =========================
        // WebViewClient
        // =========================
        webView.webViewClient = object : WebViewClient() {

            // نسخه جدید اندروید
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                return handleUrl(request.url.toString())
            }

            // پشتیبانی از نسخه‌های قدیمی اندروید
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
                super.onPageStarted(view, url, favicon)
            }

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

        // =========================
        // WebChromeClient
        // =========================
        webView.webChromeClient = object : WebChromeClient() {

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {

                val tempWebView = WebView(this@WebActivity)

                tempWebView.settings.javaScriptEnabled = true
                tempWebView.settings.domStorageEnabled = true

                tempWebView.webViewClient =
                    object : WebViewClient() {

                        override fun shouldOverrideUrlLoading(
                            v: WebView,
                            request: WebResourceRequest
                        ): Boolean {

                            val url = request.url.toString()

                            // لینک تبلیغ → مرورگر
                            if (isBrowserOnlyUrl(url)) {
                                openExternalBrowser(url)
                                return true
                            }

                            // لینک خارجی → مرورگر
                            if (
                                web.extLink &&
                                isHttpUrl(url) &&
                                !isSameDomain(url, web.url)
                            ) {
                                openExternalBrowser(url)
                                return true
                            }

                            // بقیه لینک‌ها → WebView اصلی
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

                            if (
                                web.extLink &&
                                isHttpUrl(url) &&
                                !isSameDomain(url, web.url)
                            ) {
                                openExternalBrowser(url)
                                return true
                            }

                            webView.loadUrl(url)
                            return true
                        }
                    }

                val transport =
                    resultMsg.obj as WebView.WebViewTransport

                transport.webView = tempWebView

                resultMsg.sendToTarget()

                return true
            }
        }

        root.addView(webView)
        setContentView(root)

        ThemeHelper.applySystemBars(this, config)

        // =========================
        // صفحه اصلی app.json
        // =========================
        webView.loadUrl(web.url)
    }

    // =========================================================
    // تشخیص URL
    // =========================================================

    private fun handleUrl(url: String): Boolean {

        // -------------------------
        // لینک‌های غیر HTTP
        // -------------------------
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

        // -------------------------
        // browserOnly
        // -------------------------
        if (isBrowserOnlyUrl(url)) {
            openExternalBrowser(url)
            return true
        }

        // -------------------------
        // لینک خارجی
        // -------------------------
        if (
            config.web.extLink &&
            !isSameDomain(url, config.web.url)
        ) {
            openExternalBrowser(url)
            return true
        }

        // -------------------------
        // بقیه → داخل WebView
        // -------------------------
        return false
    }

    // =========================================================
    // تشخیص browserOnly از کانفیگ app.json
    // =========================================================

    private fun isBrowserOnlyUrl(url: String): Boolean {

        val browserOnly =
            config.web.browserOnly

        if (browserOnly.isBlank()) {
            return false
        }

        val current = normalizeUrl(url)

        return browserOnly
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { pattern ->

                val target = normalizeUrl(pattern)

                // تطبیق بر اساس شروع مسیر
                current == target ||
                current.startsWith("$target/") ||
                current.startsWith("$target?") ||
                current.startsWith("$target#")
            }
    }

    // =========================================================
    // نرمال‌سازی URL
    // =========================================================

    private fun normalizeUrl(url: String): String {

        return url
            .trim()
            .removeSuffix("/")
    }

    // =========================================================
    // باز کردن مرورگر پیش‌فرض گوشی
    // =========================================================

    private fun openExternalBrowser(url: String) {

        try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )

            intent.addCategory(Intent.CATEGORY_BROWSABLE)

            startActivity(intent)

        } catch (_: Exception) {
            // مرورگر در دسترس نبود
        }
    }

    // =========================================================
    // HTTP / HTTPS
    // =========================================================

    private fun isHttpUrl(url: String): Boolean {

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
    // تشخیص دامنه
    // =========================================================

    private fun isSameDomain(
        a: String,
        b: String
    ): Boolean {

        return try {

            val hostA =
                Uri.parse(a).host ?: return false

            val hostB =
                Uri.parse(b).host ?: return false

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

    private fun mapErrorCode(code: Int): String {

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

    private fun gotoError(type: String) {

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
    // دکمه برگشت
    // =========================================================

    override fun onBackPressed() {

        if (config.exit.active) {

            ExitDialog.show(this, config) {

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

    companion object {

        const val EXTRA_ERROR_TYPE =
            "error_type"
    }
}
