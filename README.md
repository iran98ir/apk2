 فایل اصلی ساخت اپ
 # APK Builder

ساخت اپلیکیشن‌های WebView از روی `config.json`.

هر اپ = یک `config.json` + یک `icon.png` + یک `icon_round.png` + فونت.
هیچ مقدار هاردکدی توی کد نیست — همه چیز از `config.json` خونده میشه.

---

## 📁 فایل‌های لازم قبل از بیلد

قبل از اجرای workflow، این فایل‌ها **باید** توی ریپو باشن:

| مسیر | توضیح |
|---|---|
| `config.json` | تنظیمات کامل اپ (خروجی اپ‌ساز) |
| `icon.png` | آیکون اپ (PNG مربعی، مثلاً 512x512) |
| `icon_round.png` | آیکون گرد اپ (PNG) |
| `font/lalezar.ttf` | فایل فونت |

اگه هرکدوم نباشه → **بیلد fail می‌شه**.

---

## 🚀 چطوری بیلد بگیرم؟

### روش ۱: خودکار (با push)
هر بار `config.json` یا آیکون یا فونت رو تغییر بدی و push کنی، بیلد خودکار شروع می‌شه.

### روش ۲: دستی
برو به:
```
Actions → Build APK → Run workflow
```

### گرفتن خروجی
بعد از تموم شدن بیلد:
```
Actions → آخرین اجرا → Artifacts → دانلود APK
```

اسم فایل APK: `<package_name>-<version_code>.apk`

---

## 🔧 کلید امضا

توی این پروژه **کلید امضا خودکار ساخته میشه** — تو نیاز نداری کاری کنی.

⚠️ چون کلید موقته، هر بیلد امضاش با بیلد قبلی فرق داره.  
اگه بخوای اپدیت بدی که کاربر بدون uninstall نصب کنه، باید کلید ثابت داشته باشی (فعلاً پشتیبانی نمی‌شه — باگ نیست، فیچره!).

---

## 📝 ساختار `config.json`

### `brand` — اطلاعات پایه
```json
{
    "mode": "new",           // "new" یا "update"
    "name": "نام نمایشی",     // زیر آیکون دیده میشه
    "pkg": "ir.test.app",    // فقط حروف کوچک، عدد، نقطه
    "ver": "1.0",            // version name
    "vcode": 1               // version code (عدد صحیح)
}
```

### `splash` — صفحه اول
- `bgMode`: `single` یا `gradient`
- `bg`, `bg2`, `bgAngle`
- `hasIcon`, `iconSvg` (رشته SVG)
- `iconColorOn`, `iconColor`
- `iconBgOn`, `iconBg`
- `iconShape`: `square`, `circle`, `rounded`
- `iconBoxSize`, `iconSize`
- `hasText`, `title`, `tSize`, `txt`
- `iconPos`, `textPos`

### `onb` — آنبوردینگ
- `pages`: آرایه‌ای از صفحات (هر صفحه مثل splash)
- `active`: فعال/غیرفعال
- `firstRunOnly`: فقط بار اول
- `bgMode`, `bg`, `bg2`, `bgAngle`, `txt`

### `welcome` — خوش‌آمد
- مثل splash + `btn`, `btnShape`, `btnStyle`, `btnBg`, `btnTxt`, `btnSize`, `btnRadius`

### `web` — WebView
```json
{
    "url": "https://example.com",
    "extLink": true,         // لینک خارجی توی مرورگر
    "deepLink": true,        // اسکیم‌های غیر http
    "deepLinkUrl": "",
    "keepSession": true,     // کوکی‌ها حفظ بشن
    "browserOnly": ""
}
```

### `exit` — دیالوگ خروج
- `active`, `t`, `m`, `bg`, `txt`
- `yes`, `no`, رنگ‌ها، `btnLayout`: `row` یا `column`

### `error` — صفحه خطا
- `items`: ۹ نوع خطا (`offline`, `timeout`, `dns`, `ssl`, `e404`, `e500`, `down`, `unknown`)
- هر آیتم: `iconSvg`, `iconColorOn`, `iconColor`, `iconBgOn`, `iconBg`, `iconShape`, `t`, `on`, `d`

---

## 🎨 رنگ نوار بالا و پایین (Status Bar / Navigation Bar)

رنگ نوارها **همیشه** از `welcome.bg` خونده میشه.

آیکون‌ها (ساعت، باتری، بک و...) **خودکار** تعیین میشن:
- اگه `welcome.bg` **روشن** باشه → آیکون مشکی
- اگه `welcome.bg` **تیره** باشه → آیکون سفید

---

## 🔄 تفاوت new و update

فیلد `brand.mode`:
- `new` → اپ جدیده
- `update` → اپدیت اپ قبلی

**نکته:** چون کلید موقته، این فیلد فعلاً فقط برای اطلاعات استفاده میشه.  
اگه کلید ثابت اضافه شد (نسخه‌های بعدی)، این فیلد روی امضا اثر می‌ذاره.

---

## 🏗️ ساختار پروژه

```
.
├── .github/workflows/build.yml    ← pipeline بیلد
├── config.json                     ← ورودی اصلی
├── icon.png                        ← آیکون اپ
├── icon_round.png                  ← آیکون گرد
├── font/lalezar.ttf                ← فونت
├── scripts/prepare.py              ← json → منابع اندروید
├── app/                            ← پروژه اندروید
└── README.md
```

---

## 📦 تکنولوژی‌ها

- Android (Kotlin)
- `androidsvg` برای رندر SVG
- GitHub Actions برای بیلد

---

## 🛠️ ساخت دستی (لوکال)

```bash
pip install pillow cairosvg
python scripts/prepare.py
./gradlew assembleRelease
```

خروجی: `app/build/outputs/apk/release/`
