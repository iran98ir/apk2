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

/**
 * WebView اصلی اپ
 *
 * قوانین:
 *
 * 1) web.url
 *    صفحه اصلی اپ است و داخل WebView باز می‌شود.
 *
 * 2) web.browserOnly
 *    لینک‌هایی که باید حتماً خارج از اپ و در مرورگر
 *    پیش‌فرض سیستم باز شوند.
 *
 * 3) web.extLink
 *    در صورت فعال بودن، لینک‌های خارج از دامنه اصلی
 *    در مرورگر باز می‌شوند.
 *
 * 4) هیچ URLای در این فایل هاردکد نشده است.
 *    تمام URLها فقط از app.json خوانده می‌شوند.
 */
class WebActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---------------------------------------------------------
        // بارگذاری تنظیمات از app.json
        // ---------------------------------------------------------
        config = ConfigLoader.load(this)

        val web = config.web

        // ---------------------------------------------------------
        // ساخت WebView
        // ---------------------------------------------------------
        val root = FrameLayout(this)

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // ---------------------------------------------------------
        // تنظیمات WebView
        // ---------------------------------------------------------
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

        // ---------------------------------------------------------
        // Cookie
        // ---------------------------------------------------------
        if (web.keepSession) {

            CookieManager.getInstance().setAcceptCookie(true)

            CookieManager.getInstance()
                .setAcceptThirdPartyCookies(webView, true)

        } else {

            CookieManager.getInstance().setAcceptCookie(false)

            CookieManager.getInstance()
                .setAcceptThirdPartyCookies(webView, false)

            CookieManager.getInstance()
                .removeAllCookies(null)
        }

        // ---------------------------------------------------------
        // WebViewClient
        // ---------------------------------------------------------
        webView.webViewClient = object : WebViewClient() {

            /**
             * تمام navigationهای داخل WebView از اینجا عبور می‌کنند.
             */
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                val url = request.url.toString()

                return handleUrl(url)
            }

            /**
             * برای سازگاری با WebViewهای قدیمی‌تر.
             */
            @Suppress("OVERRIDE_DEPRECATION")
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

                // فقط خطای صفحه اصلی
                if (request.isForMainFrame) {
                    gotoError(
                        mapErrorCode(error.errorCode)
                    )
                }
            }
        }

        // ---------------------------------------------------------
        // WebChromeClient
        // ---------------------------------------------------------
        webView.webChromeClient = object : WebChromeClient() {

            /**
             * مدیریت:
             *
             * target="_blank"
             * window.open()
             *
             * لینک browserOnly در این مسیر هم
             * حتماً به مرورگر سیستم می‌رود.
             */
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {

                val tempWebView = WebView(this@WebActivity)

                // تنظیمات لازم برای WebView موقت
                val tempSettings = tempWebView.settings

                tempSettings.javaScriptEnabled = true
                tempSettings.domStorageEnabled = true
                tempSettings.databaseEnabled = true

                tempSettings.setSupportMultipleWindows(false)
                tempSettings.javaScriptCanOpenWindowsAutomatically = true

                // -------------------------------------------------
                // Client برای WebView موقت
                // -------------------------------------------------
                tempWebView.webViewClient =
                    object : WebViewClient() {

                        override fun shouldOverrideUrlLoading(
                            v: WebView,
                            request: WebResourceRequest
                        ): Boolean {

                            val url = request.url.toString()

                            return handleNewWindowUrl(url)
                        }

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun shouldOverrideUrlLoading(
                            v: WebView,
                            url: String
                        ): Boolean {

                            return handleNewWindowUrl(url)
                        }
                    }

                // -------------------------------------------------
                // تحویل WebView موقت به WebViewTransport
                // -------------------------------------------------
                val transport =
                    resultMsg.obj as WebView.WebViewTransport

                transport.webView = tempWebView

                resultMsg.sendToTarget()

                return true
            }
        }

        // ---------------------------------------------------------
        // قرار دادن WebView روی صفحه
        // ---------------------------------------------------------
        root.addView(webView)

        setContentView(root)

        // ---------------------------------------------------------
        // System Bars
        // ---------------------------------------------------------
        ThemeHelper.applySystemBars(this, config)

        // ---------------------------------------------------------
        // فقط URL اصلی JSON
        // ---------------------------------------------------------
        webView.loadUrl(web.url)
    }

    // =============================================================
    // مدیریت URL اصلی
    // =============================================================

    /**
     * تصمیم می‌گیرد URL کجا باز شود:
     *
     * browserOnly → مرورگر
     * deepLink → Intent
     * extLink → مرورگر
     * بقیه → WebView
     */
    private fun handleUrl(url: String): Boolean {

        val web = config.web

        // ---------------------------------------------------------
        // URLهای غیر HTTP/HTTPS
        // ---------------------------------------------------------
        if (
            !url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {

            if (web.deepLink) {

                return openExternalBrowserOrIntent(url)
            }

            return true
        }

        // ---------------------------------------------------------
        // browserOnly
        //
        // مهم:
        // تطابق دقیق با مقدار داخل JSON
        // ---------------------------------------------------------
        if (
            matchesBrowserOnly(
                url = url,
                browserOnly = web.browserOnly
            )
        ) {

            openInDefaultBrowser(url)

            return true
        }

        // ---------------------------------------------------------
        // extLink
        // ---------------------------------------------------------
        if (
            web.extLink &&
            !isSameDomain(url, web.url)
        ) {

            openInDefaultBrowser(url)

            return true
        }

        // ---------------------------------------------------------
        // سایر لینک‌ها:
        // داخل WebView
        // ---------------------------------------------------------
        return false
    }

    // =============================================================
    // مدیریت URLهای پنجره جدید
    // =============================================================

    private fun handleNewWindowUrl(url: String): Boolean {

        val web = config.web

        // ---------------------------------------------------------
        // browserOnly
        // ---------------------------------------------------------
        if (
            matchesBrowserOnly(
                url = url,
                browserOnly = web.browserOnly
            )
        ) {

            openInDefaultBrowser(url)

            return true
        }

        // ---------------------------------------------------------
        // لینک خارجی
        // ---------------------------------------------------------
        if (
            web.extLink &&
            isHttpUrl(url) &&
            !isSameDomain(url, web.url)
        ) {

            openInDefaultBrowser(url)

            return true
        }

        // ---------------------------------------------------------
        // سایر لینک‌ها:
        // در WebView اصلی
        // ---------------------------------------------------------
        if (isHttpUrl(url)) {

            webView.loadUrl(url)

            return true
        }

        // ---------------------------------------------------------
        // Schemeهای دیگر
        // ---------------------------------------------------------
        if (web.deepLink) {

            openExternalBrowserOrIntent(url)

            return true
        }

        return true
    }

    // =============================================================
    // browserOnly
    // =============================================================

    /**
     * بررسی لینک‌های browserOnly که از JSON آمده‌اند.
     *
     * JSON می‌تواند یک یا چند URL داشته باشد:
     *
     * "https://site.com/a"
     *
     * یا:
     *
     * "https://site.com/a, https://site.com/b"
     *
     * تطابق کاملاً دقیق است.
     */
    private fun matchesBrowserOnly(
        url: String,
        browserOnly: String
    ): Boolean {

        if (browserOnly.isBlank()) {
            return false
        }

        return browserOnly
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { pattern ->

                url == pattern
            }
    }

    // =============================================================
    // باز کردن در مرورگر پیش‌فرض
    // =============================================================

    /**
     * لینک را کاملاً از WebView خارج کرده و
     * با مرورگر پیش‌فرض سیستم باز می‌کند.
     */
    private fun openInDefaultBrowser(url: String) {

        try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )

            intent.addCategory(Intent.CATEGORY_BROWSABLE)

            startActivity(intent)

        } catch (e: Exception) {

            // اگر مرورگر پیش‌فرض قابل اجرا نبود،
            // هیچ URLای داخل WebView باز نمی‌شود.
        }
    }

    // =============================================================
    // Deep Link / Scheme
    // =============================================================

    private fun openExternalBrowserOrIntent(
        url: String
    ): Boolean {

        return try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )

            startActivity(intent)

            true

        } catch (e: Exception) {

            true
        }
    }

    // =============================================================
    // تشخیص HTTP / HTTPS
    // =============================================================

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

    // =============================================================
    // تشخیص دامنه
    // =============================================================

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

        } catch (e: Exception) {

            false
        }
    }

    // =============================================================
    // Error Mapping
    // =============================================================

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

    // =============================================================
    // Error Activity
    // =============================================================

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

    // =============================================================
    // Back Button
    // =============================================================

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

    // =============================================================
    // Constants
    // =============================================================

    companion object {

        const val EXTRA_ERROR_TYPE =
            "error_type"
    }
}
