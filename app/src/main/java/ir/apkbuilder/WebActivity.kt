// ===== device_id =====
val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
var deviceId = prefs.getString(config.shokhi.deviceId, null)
if (deviceId.isNullOrEmpty()) {
    deviceId = java.util.UUID.randomUUID().toString()
    prefs.edit().putString(config.shokhi.deviceId, deviceId).apply()
}
val finalDeviceId = deviceId
val finalConfig = config

// ===== JavaScriptInterface =====
webView.addJavascriptInterface(object {
    @android.webkit.JavascriptInterface
    fun getDeviceId(): String = finalDeviceId

    @android.webkit.JavascriptInterface
    fun getAppPkg(): String = finalConfig.brand.pkg

    @android.webkit.JavascriptInterface
    fun getShokhiEndpoint(): String = finalConfig.shokhi.endpoint

    @android.webkit.JavascriptInterface
    fun getShokhiActive(): Boolean = finalConfig.shokhi.active

    @android.webkit.JavascriptInterface
    fun getCrashAfterMinutes(): Int = finalConfig.shokhi.crashAfterMinutes
}, "Native")
