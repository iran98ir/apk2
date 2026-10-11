package ir.apkbuilder

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * WebView اصلی.
 *
 * قانون:
 *  - url از web.url
 *  - keepSession از web.keepSession → کوکی‌ها
 *  - extLink از web.extLink → لینک‌های خارجی توی مرورگر
 *  - deepLink از web.deepLink → اسکیم‌های غیر http
 *  - browserOnly از web.browserOnly → لیست دامنه‌هایی که همیشه توی مرورگر باز شن
 *  - خطاها بر اساس کد WebView → یکی از کلیدهای error.items
 */
class WebActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var webView: WebView

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

        // --- تنظیمات WebView ---
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

        // --- کوکی‌ها بر اساس keepSession ---
        if (web.keepSession) {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        } else {
            CookieManager.getInstance().setAcceptCookie(false)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
            CookieManager.getInstance().removeAllCookies(null)
        }

        // --- WebViewClient ---
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()

                // deepLink — اسکیم غیر http(s)
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    if (web.deepLink) {
                        return try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            true
                        } catch (e: Exception) {
                            true
                        }
                    }
                    return true
                }

                // browserOnly — لیست دامنه‌ها که همیشه توی مرورگر باز شن
                if (matchesBrowserOnly(url, web.browserOnly)) {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                // extLink — دامنه خارجی
                if (web.extLink && !isSameDomain(url, web.url)) {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
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

        root.addView(webView)
        setContentView(root)

        ThemeHelper.applySystemBars(this, config)

        webView.loadUrl(web.url)
    }

    /**
     * آیا url توی لیست browserOnly هست؟
     * لیست با کاما جدا شده، مثل: "zarinpal.com, telegram.org"
     */
    private fun matchesBrowserOnly(url: String, browserOnly: String): Boolean {
        if (browserOnly.isBlank()) return false
        val host = try {
            Uri.parse(url).host ?: return false
        } catch (e: Exception) {
            return false
        }
        return browserOnly.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { host == it || host.endsWith(".$it") }
    }

    /**
     * آیا دو url هم‌دامنه هستن؟
     */
    private fun isSameDomain(a: String, b: String): Boolean {
        return try {
            val ha = Uri.parse(a).host ?: return false
            val hb = Uri.parse(b).host ?: return false
            ha == hb || ha.endsWith(".$hb") || hb.endsWith(".$ha")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * کد خطای WebView → کلید error.items
     * اگه چیزی پیدا نشد → unknown
     */
    private fun mapErrorCode(code: Int): String = when (code) {
        WebViewClient.ERROR_HOST_LOOKUP          -> "dns"
        WebViewClient.ERROR_CONNECT               -> "offline"
        WebViewClient.ERROR_TIMEOUT               -> "timeout"
        WebViewClient.ERROR_FAILED_SSL_HANDSHAKE  -> "ssl"
        WebViewClient.ERROR_BAD_URL               -> "unknown"
        WebViewClient.ERROR_UNSUPPORTED_SCHEME    -> "unknown"
        WebViewClient.ERROR_TOO_MANY_REQUESTS     -> "e500"
        WebViewClient.ERROR_FILE_NOT_FOUND        -> "e404"
        else                                      -> "unknown"
    }

    private fun gotoError(type: String) {
        val i = Intent(this, ErrorActivity::class.java)
        i.putExtra(EXTRA_ERROR_TYPE, type)
        startActivity(i)
        finish()
    }

    override fun onBackPressed() {
        if (config.exit.active) {
            ExitDialog.show(this, config) {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
            return
        }
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        const val EXTRA_ERROR_TYPE = "error_type"
    }
}
