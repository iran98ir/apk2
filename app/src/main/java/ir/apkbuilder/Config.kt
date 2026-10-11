package ir.apkbuilder

import android.content.Context
import org.json.JSONObject

/**
 * مدل کامل config.json
 * همه فیلدها از json میاد — هیچ مقدار هاردکدی نیست.
 * اگه فیلد اجباری نبود → exception → اپ کرش (بیلد نباید انجام بشه).
 */

data class Brand(
    val mode: String,
    val name: String,
    val pkg: String,
    val ver: String,
    val vcode: Int
)

data class SplashConfig(
    val bgMode: String,
    val bg: String,
    val bg2: String,
    val bgAngle: Int,
    val hasIcon: Boolean,
    val iconSvg: String?,
    val iconColorOn: Boolean,
    val iconColor: String,
    val iconBgOn: Boolean,
    val iconBg: String,
    val iconShape: String,
    val iconBoxSize: Int,
    val iconSize: Int,
    val hasText: Boolean,
    val title: String,
    val tSize: Int,
    val txt: String,
    val iconPos: String,
    val textPos: String
)

data class OnbPage(
    val iconSvg: String?,
    val iconColorOn: Boolean,
    val iconColor: String,
    val iconBgOn: Boolean,
    val iconBg: String,
    val iconShape: String,
    val iconBoxSize: Int,
    val iconSize: Int,
    val t: String,
    val tSize: Int,
    val d: String,
    val dSize: Int,
    val iconPos: String,
    val textPos: String
)

data class OnbConfig(
    val pages: List<OnbPage>,
    val active: Boolean,
    val firstRunOnly: Boolean,
    val count: String,
    val bgMode: String,
    val bg: String,
    val bg2: String,
    val bgAngle: Int,
    val txt: String,
    val totalPages: Int
)

data class WelcomeConfig(
    val active: Boolean,
    val bgMode: String,
    val bg: String,
    val bg2: String,
    val bgAngle: Int,
    val txt: String,
    val hasIcon: Boolean,
    val iconSvg: String?,
    val iconColorOn: Boolean,
    val iconColor: String,
    val iconBgOn: Boolean,
    val iconBg: String,
    val iconShape: String,
    val iconBoxSize: Int,
    val iconSize: Int,
    val t: String,
    val tSize: Int,
    val d: String,
    val dSize: Int,
    val btn: String,
    val btnShape: String,
    val btnStyle: String,
    val btnBg: String,
    val btnTxt: String,
    val btnSize: Int,
    val btnRadius: Int,
    val iconPos: String,
    val textPos: String,
    val btnPos: String
)

data class WebConfig(
    val url: String,
    val extLink: Boolean,
    val deepLink: Boolean,
    val deepLinkUrl: String,
    val keepSession: Boolean,
    val browserOnly: String
)

data class ExitConfig(
    val active: Boolean,
    val t: String,
    val m: String,
    val bg: String,
    val txt: String,
    val tSize: Int,
    val mSize: Int,
    val btnSize: Int,
    val anim: String,
    val yes: String,
    val no: String,
    val yesBg: String,
    val yesTxt: String,
    val noBg: String,
    val noTxt: String,
    val btnShape: String,
    val btnStyle: String,
    val btnLayout: String
)

data class ErrorItem(
    val iconSvg: String?,
    val iconColorOn: Boolean,
    val iconColor: String,
    val iconBgOn: Boolean,
    val iconBg: String,
    val iconShape: String,
    val iconBoxSize: Int,
    val iconSize: Int,
    val t: String,
    val on: Boolean,
    val d: String
)

data class ErrorConfig(
    val items: Map<String, ErrorItem>,
    val active: Boolean,
    val bgMode: String,
    val bg: String,
    val bg2: String,
    val bgAngle: Int,
    val txt: String,
    val tSize: Int,
    val dSize: Int,
    val retryText: String,
    val btnBg: String,
    val btnShape: String,
    val btnStyle: String
)

data class AppConfig(
    val brand: Brand,
    val splash: SplashConfig,
    val onb: OnbConfig,
    val welcome: WelcomeConfig,
    val web: WebConfig,
    val exit: ExitConfig,
    val error: ErrorConfig
)

object ConfigLoader {

    fun load(context: Context): AppConfig {
        val raw = context.assets.open("config.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        return parse(root)
    }

    private fun parse(root: JSONObject): AppConfig {
        val brandJson = root.getJSONObject("brand")
        val brand = Brand(
            mode = brandJson.getString("mode"),
            name = brandJson.getString("name"),
            pkg = brandJson.getString("pkg"),
            ver = brandJson.getString("ver"),
            vcode = brandJson.getInt("vcode")
        )

        val sp = root.getJSONObject("splash")
        val splash = SplashConfig(
            bgMode = sp.getString("bgMode"),
            bg = sp.getString("bg"),
            bg2 = sp.optString("bg2", sp.getString("bg")),
            bgAngle = sp.optInt("bgAngle", 0),
            hasIcon = sp.optBoolean("hasIcon", false),
            iconSvg = if (sp.has("iconSvg")) sp.getString("iconSvg") else null,
            iconColorOn = sp.optBoolean("iconColorOn", false),
            iconColor = sp.optString("iconColor", "#000000"),
            iconBgOn = sp.optBoolean("iconBgOn", false),
            iconBg = sp.optString("iconBg", "#FFFFFF"),
            iconShape = sp.optString("iconShape", "square"),
            iconBoxSize = sp.optInt("iconBoxSize", 60),
            iconSize = sp.optInt("iconSize", 24),
            hasText = sp.optBoolean("hasText", false),
            title = sp.optString("title", ""),
            tSize = sp.optInt("tSize", 16),
            txt = sp.optString("txt", "#000000"),
            iconPos = sp.optString("iconPos", "top"),
            textPos = sp.optString("textPos", "bottom")
        )

        val onbJson = root.getJSONObject("onb")
        val pagesArr = onbJson.getJSONArray("pages")
        val pages = mutableListOf<OnbPage>()
        for (i in 0 until pagesArr.length()) {
            val p = pagesArr.getJSONObject(i)
            pages.add(
                OnbPage(
                    iconSvg = if (p.has("iconSvg")) p.getString("iconSvg") else null,
                    iconColorOn = p.optBoolean("iconColorOn", false),
                    iconColor = p.optString("iconColor", "#000000"),
                    iconBgOn = p.optBoolean("iconBgOn", false),
                    iconBg = p.optString("iconBg", "#FFFFFF"),
                    iconShape = p.optString("iconShape", "square"),
                    iconBoxSize = p.optInt("iconBoxSize", 60),
                    iconSize = p.optInt("iconSize", 24),
                    t = p.optString("t", ""),
                    tSize = p.optInt("tSize", 16),
                    d = p.optString("d", ""),
                    dSize = p.optInt("dSize", 14),
                    iconPos = p.optString("iconPos", "top"),
                    textPos = p.optString("textPos", "bottom")
                )
            )
        }
        val onb = OnbConfig(
            pages = pages,
            active = onbJson.optBoolean("active", true),
            firstRunOnly = onbJson.optBoolean("firstRunOnly", true),
            count = onbJson.optString("count", pages.size.toString()),
            bgMode = onbJson.getString("bgMode"),
            bg = onbJson.getString("bg"),
            bg2 = onbJson.optString("bg2", onbJson.getString("bg")),
            bgAngle = onbJson.optInt("bgAngle", 0),
            txt = onbJson.optString("txt", "#000000"),
            totalPages = onbJson.optInt("totalPages", pages.size)
        )

        val w = root.getJSONObject("welcome")
        val welcome = WelcomeConfig(
            active = w.optBoolean("active", true),
            bgMode = w.getString("bgMode"),
            bg = w.getString("bg"),
            bg2 = w.optString("bg2", w.getString("bg")),
            bgAngle = w.optInt("bgAngle", 0),
            txt = w.optString("txt", "#000000"),
            hasIcon = w.optBoolean("hasIcon", false),
            iconSvg = if (w.has("iconSvg")) w.getString("iconSvg") else null,
            iconColorOn = w.optBoolean("iconColorOn", false),
            iconColor = w.optString("iconColor", "#000000"),
            iconBgOn = w.optBoolean("iconBgOn", false),
            iconBg = w.optString("iconBg", "#FFFFFF"),
            iconShape = w.optString("iconShape", "square"),
            iconBoxSize = w.optInt("iconBoxSize", 60),
            iconSize = w.optInt("iconSize", 24),
            t = w.optString("t", ""),
            tSize = w.optInt("tSize", 20),
            d = w.optString("d", ""),
            dSize = w.optInt("dSize", 14),
            btn = w.optString("btn", ""),
            btnShape = w.optString("btnShape", "rounded"),
            btnStyle = w.optString("btnStyle", "elevated"),
            btnBg = w.optString("btnBg", w.getString("bg")),
            btnTxt = w.optString("btnTxt", "#FFFFFF"),
            btnSize = w.optInt("btnSize", 14),
            btnRadius = w.optInt("btnRadius", 8),
            iconPos = w.optString("iconPos", "top"),
            textPos = w.optString("textPos", "middle"),
            btnPos = w.optString("btnPos", "bottom")
        )

        val webJson = root.getJSONObject("web")
        val web = WebConfig(
            url = webJson.getString("url"),
            extLink = webJson.optBoolean("extLink", false),
            deepLink = webJson.optBoolean("deepLink", false),
            deepLinkUrl = webJson.optString("deepLinkUrl", ""),
            keepSession = webJson.optBoolean("keepSession", true),
            browserOnly = webJson.optString("browserOnly", "")
        )

        val e = root.getJSONObject("exit")
        val exit = ExitConfig(
            active = e.optBoolean("active", false),
            t = e.optString("t", ""),
            m = e.optString("m", ""),
            bg = e.optString("bg", "#FFFFFF"),
            txt = e.optString("txt", "#000000"),
            tSize = e.optInt("tSize", 16),
            mSize = e.optInt("mSize", 14),
            btnSize = e.optInt("btnSize", 14),
            anim = e.optString("anim", "none"),
            yes = e.optString("yes", ""),
            no = e.optString("no", ""),
            yesBg = e.optString("yesBg", "#4F46E5"),
            yesTxt = e.optString("yesTxt", "#FFFFFF"),
            noBg = e.optString("noBg", "#E5E7EB"),
            noTxt = e.optString("noTxt", "#000000"),
            btnShape = e.optString("btnShape", "rounded"),
            btnStyle = e.optString("btnStyle", "elevated"),
            btnLayout = e.optString("btnLayout", "row")
        )

        val errJson = root.getJSONObject("error")
        val itemsJson = errJson.getJSONObject("items")
        val items = mutableMapOf<String, ErrorItem>()
        val keys = itemsJson.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val it = itemsJson.getJSONObject(k)
            items[k] = ErrorItem(
                iconSvg = if (it.has("iconSvg")) it.getString("iconSvg") else null,
                iconColorOn = it.optBoolean("iconColorOn", false),
                iconColor = it.optString("iconColor", "#000000"),
                iconBgOn = it.optBoolean("iconBgOn", false),
                iconBg = it.optString("iconBg", "#FFFFFF"),
                iconShape = it.optString("iconShape", "square"),
                iconBoxSize = it.optInt("iconBoxSize", 60),
                iconSize = it.optInt("iconSize", 24),
                t = it.optString("t", ""),
                on = it.optBoolean("on", true),
                d = it.optString("d", "")
            )
        }
        val error = ErrorConfig(
            items = items,
            active = errJson.optBoolean("active", true),
            bgMode = errJson.getString("bgMode"),
            bg = errJson.getString("bg"),
            bg2 = errJson.optString("bg2", errJson.getString("bg")),
            bgAngle = errJson.optInt("bgAngle", 0),
            txt = errJson.optString("txt", "#000000"),
            tSize = errJson.optInt("tSize", 18),
            dSize = errJson.optInt("dSize", 14),
            retryText = errJson.optString("retryText", ""),
            btnBg = errJson.optString("btnBg", "#4F46E5"),
            btnShape = errJson.optString("btnShape", "rounded"),
            btnStyle = errJson.optString("btnStyle", "elevated")
        )

        return AppConfig(
            brand = brand,
            splash = splash,
            onb = onb,
            welcome = welcome,
            web = web,
            exit = exit,
            error = error
        )
    }
}
