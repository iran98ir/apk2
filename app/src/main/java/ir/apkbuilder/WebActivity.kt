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
 * - url از json
 * - keepSession -> cookies فعال/غیرفعال
 * - extLink -> لینک‌های خارجی توی مرورگر باز شن
 * - deepLink -> اسکیم‌های غیر http به اکشن مناسب
 * - خطاها -> ErrorActivity با نوع مناسب
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
        webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

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
            ): Boolean {
                val url = request.url.toString()

                // deepLink (اسکیم‌های غیر http/https)
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    if (web.deepLink) {
                        return try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                            true
                        } catch (e: Exception) {
                            true
                        }
                    }
                    return true
                }

                // extLink -> باز کردن لینک خارجی توی مرورگر
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
                // فقط خطای main frame مهمه
                if (request.isForMainFrame) {
                    val errorType = mapErrorCode(error.errorCode)
                    gotoError(errorType)
                }
            }
        }

        root.addView(webView)
        setContentView(root)

        ThemeHelper.applySystemBars(this, config)

        webView.loadUrl(web.url)
    }

    private fun isSameDomain(a: String, b: String): Boolean {
        return try {
            val ha = Uri.parse(a).host ?: return false
            val hb = Uri.parse(b).host ?: return false
            ha == hb || ha.endsWith(".$hb") || hb.endsWith(".$ha")
        } catch (e: Exception) {
            false
        }
    }

    private fun mapErrorCode(code: Int): String {
        // نگاشت کدهای WebView به کلیدهای json: offline, timeout, dns, ssl, e404, e500, down, unknown
        return when (code) {
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
    }

    private fun gotoError(type: String) {
        val i = Intent(this, ErrorActivity::class.java)
        i.putExtra("error_type", type)
        startActivity(i)
        finish()
    }

    override fun onBackPressed() {
        // اگه exit فعاله، دیالوگ نشون بده
        if (config.exit.active) {
            showExitDialog()
            return
        }
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun showExitDialog() {
        ExitDialog.show(this, config) {
            if (webView.canGoBack()) webView.goBack() else finish()
        }
    }
}
