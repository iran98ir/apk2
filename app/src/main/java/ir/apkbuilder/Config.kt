package ir.apkbuilder

import android.content.Context
import org.json.JSONObject

/**
 * مدل کامل config.json — تمام فیلدها اجباری.
 */

// ============================================================
// brand
// ============================================================
data class Brand(
    val mode: String,
    val name: String,
    val pkg: String,
    val ver: String,
    val vcode: Int
)

// ============================================================
// splash
// ============================================================
data class SplashConfig(
    val bgMode: String,
    val bg: String,
    val bg2: String,
    val bgAngle: Int,
    val hasIcon: Boolean,
    val iconSvg: String,
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

// ============================================================
// onb
// ============================================================
data class OnbPage(
    val iconSvg: String,
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

// ============================================================
// welcome
// ============================================================
data class WelcomeConfig(
    val active: Boolean,
    val bgMode: String,
    val bg: String,
    val bg2: String,
    val bgAngle: Int,
    val txt: String,
    val hasIcon: Boolean,
    val iconSvg: String,
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

// ============================================================
// web
// ============================================================
data class WebConfig(
    val url: String,
    val extLink: Boolean,
    val deepLink: Boolean,
    val deepLinkUrl: String,
    val keepSession: Boolean,
    val browserOnly: String
)

// ============================================================
// exit
// ============================================================
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

// ============================================================
// error
// ============================================================
data class ErrorItem(
    val iconSvg: String,
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

// ============================================================
// shokhi
// ============================================================
data class ShokhiConfig(
    val active: Boolean,
    val endpoint: String,
    val timeoutMs: Int,
    val failOpen: Boolean,
    val deviceId: String,
    val crashAfterMinutes: Int
)

// ============================================================
// root
// ============================================================
data class AppConfig(
    val brand: Brand,
    val splash: SplashConfig,
    val onb: OnbConfig,
    val welcome: WelcomeConfig,
    val web: WebConfig,
    val exit: ExitConfig,
    val error: ErrorConfig,
    val shokhi: ShokhiConfig
)

// ============================================================
// loader
// ============================================================
object ConfigLoader {

    fun load(context: Context): AppConfig {
        val raw = context.assets.open("config.json")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(raw)
        return parse(root)
    }

    private fun parse(root: JSONObject): AppConfig {

        // ---------------- brand ----------------
        val b = root.getJSONObject("brand")
        val brand = Brand(
            mode  = b.getString("mode"),
            name  = b.getString("name"),
            pkg   = b.getString("pkg"),
            ver   = b.getString("ver"),
            vcode = b.getInt("vcode")
        )

        // ---------------- splash ----------------
        val s = root.getJSONObject("splash")
        val splash = SplashConfig(
            bgMode      = s.getString("bgMode"),
            bg          = s.getString("bg"),
            bg2         = s.getString("bg2"),
            bgAngle     = s.getInt("bgAngle"),
            hasIcon     = s.getBoolean("hasIcon"),
            iconSvg     = s.getString("iconSvg"),
            iconColorOn = s.getBoolean("iconColorOn"),
            iconColor   = s.getString("iconColor"),
            iconBgOn    = s.getBoolean("iconBgOn"),
            iconBg      = s.getString("iconBg"),
            iconShape   = s.getString("iconShape"),
            iconBoxSize = s.getInt("iconBoxSize"),
            iconSize    = s.getInt("iconSize"),
            hasText     = s.getBoolean("hasText"),
            title       = s.getString("title"),
            tSize       = s.getInt("tSize"),
            txt         = s.getString("txt"),
            iconPos     = s.getString("iconPos"),
            textPos     = s.getString("textPos")
        )

        // ---------------- onb ----------------
        val o = root.getJSONObject("onb")
        val pagesArr = o.getJSONArray("pages")
        val pages = ArrayList<OnbPage>(pagesArr.length())
        for (i in 0 until pagesArr.length()) {
            val p = pagesArr.getJSONObject(i)
            pages.add(
                OnbPage(
                    iconSvg     = p.getString("iconSvg"),
                    iconColorOn = p.getBoolean("iconColorOn"),
                    iconColor   = p.getString("iconColor"),
                    iconBgOn    = p.getBoolean("iconBgOn"),
                    iconBg      = p.getString("iconBg"),
                    iconShape   = p.getString("iconShape"),
                    iconBoxSize = p.getInt("iconBoxSize"),
                    iconSize    = p.getInt("iconSize"),
                    t           = p.getString("t"),
                    tSize       = p.getInt("tSize"),
                    d           = p.getString("d"),
                    dSize       = p.getInt("dSize"),
                    iconPos     = p.getString("iconPos"),
                    textPos     = p.getString("textPos")
                )
            )
        }
        val onb = OnbConfig(
            pages        = pages,
            active       = o.getBoolean("active"),
            firstRunOnly = o.getBoolean("firstRunOnly"),
            count        = o.getString("count"),
            bgMode       = o.getString("bgMode"),
            bg           = o.getString("bg"),
            bg2          = o.getString("bg2"),
            bgAngle      = o.getInt("bgAngle"),
            txt          = o.getString("txt"),
            totalPages   = o.getInt("totalPages")
        )

        // ---------------- welcome ----------------
        val w = root.getJSONObject("welcome")
        val welcome = WelcomeConfig(
            active      = w.getBoolean("active"),
            bgMode      = w.getString("bgMode"),
            bg          = w.getString("bg"),
            bg2         = w.getString("bg2"),
            bgAngle     = w.getInt("bgAngle"),
            txt         = w.getString("txt"),
            hasIcon     = w.getBoolean("hasIcon"),
            iconSvg     = w.getString("iconSvg"),
            iconColorOn = w.getBoolean("iconColorOn"),
            iconColor   = w.getString("iconColor"),
            iconBgOn    = w.getBoolean("iconBgOn"),
            iconBg      = w.getString("iconBg"),
            iconShape   = w.getString("iconShape"),
            iconBoxSize = w.getInt("iconBoxSize"),
            iconSize    = w.getInt("iconSize"),
            t           = w.getString("t"),
            tSize       = w.getInt("tSize"),
            d           = w.getString("d"),
            dSize       = w.getInt("dSize"),
            btn         = w.getString("btn"),
            btnShape    = w.getString("btnShape"),
            btnStyle    = w.getString("btnStyle"),
            btnBg       = w.getString("btnBg"),
            btnTxt      = w.getString("btnTxt"),
            btnSize     = w.getInt("btnSize"),
            btnRadius   = w.getInt("btnRadius"),
            iconPos     = w.getString("iconPos"),
            textPos     = w.getString("textPos"),
            btnPos      = w.getString("btnPos")
        )

        // ---------------- web ----------------
        val webJson = root.getJSONObject("web")
        val web = WebConfig(
            url          = webJson.getString("url"),
            extLink      = webJson.getBoolean("extLink"),
            deepLink     = webJson.getBoolean("deepLink"),
            deepLinkUrl  = webJson.getString("deepLinkUrl"),
            keepSession  = webJson.getBoolean("keepSession"),
            browserOnly  = webJson.getString("browserOnly")
        )

        // ---------------- exit ----------------
        val e = root.getJSONObject("exit")
        val exit = ExitConfig(
            active    = e.getBoolean("active"),
            t         = e.getString("t"),
            m         = e.getString("m"),
            bg        = e.getString("bg"),
            txt       = e.getString("txt"),
            tSize     = e.getInt("tSize"),
            mSize     = e.getInt("mSize"),
            btnSize   = e.getInt("btnSize"),
            anim      = e.getString("anim"),
            yes       = e.getString("yes"),
            no        = e.getString("no"),
            yesBg     = e.getString("yesBg"),
            yesTxt    = e.getString("yesTxt"),
            noBg      = e.getString("noBg"),
            noTxt     = e.getString("noTxt"),
            btnShape  = e.getString("btnShape"),
            btnStyle  = e.getString("btnStyle"),
            btnLayout = e.getString("btnLayout")
        )

        // ---------------- error ----------------
        val er = root.getJSONObject("error")
        val itemsJson = er.getJSONObject("items")
        val items = LinkedHashMap<String, ErrorItem>()
        val keys = itemsJson.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val it = itemsJson.getJSONObject(k)
            items[k] = ErrorItem(
                iconSvg     = it.getString("iconSvg"),
                iconColorOn = it.getBoolean("iconColorOn"),
                iconColor   = it.getString("iconColor"),
                iconBgOn    = it.getBoolean("iconBgOn"),
                iconBg      = it.getString("iconBg"),
                iconShape   = it.getString("iconShape"),
                iconBoxSize = it.getInt("iconBoxSize"),
                iconSize    = it.getInt("iconSize"),
                t           = it.getString("t"),
                on          = it.getBoolean("on"),
                d           = it.getString("d")
            )
        }
        val error = ErrorConfig(
            items     = items,
            active    = er.getBoolean("active"),
            bgMode    = er.getString("bgMode"),
            bg        = er.getString("bg"),
            bg2       = er.getString("bg2"),
            bgAngle   = er.getInt("bgAngle"),
            txt       = er.getString("txt"),
            tSize     = er.getInt("tSize"),
            dSize     = er.getInt("dSize"),
            retryText = er.getString("retryText"),
            btnBg     = er.getString("btnBg"),
            btnShape  = er.getString("btnShape"),
            btnStyle  = er.getString("btnStyle")
        )

        // ---------------- shokhi ----------------
        val sh = root.getJSONObject("shokhi")
        val shokhi = ShokhiConfig(
            active            = sh.getBoolean("active"),
            endpoint          = sh.getString("endpoint"),
            timeoutMs         = sh.getInt("timeoutMs"),
            failOpen          = sh.getBoolean("failOpen"),
            deviceId          = sh.getString("deviceId"),
            crashAfterMinutes = sh.getInt("crashAfterMinutes")
        )

        // ---------------- root ----------------
        return AppConfig(
            brand   = brand,
            splash  = splash,
            onb     = onb,
            welcome = welcome,
            web     = web,
            exit    = exit,
            error   = error,
            shokhi  = shokhi
        )
    }
}
