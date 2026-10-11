#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
prepare.py
خواندن config.json و ساخت تمام منابع لازم برای بیلد:
- strings.xml (app_name)
- colors.xml (رنگ‌های welcome)
- AndroidManifest.xml (applicationId, versionName, versionCode)
- کپی icon.png / icon_round.png به mipmap با همه سایزها
- کپی font/lalezar.ttf به res/font
- کپی config.json به assets
- ساخت themes.xml (بدون هاردکد، از welcome.bg)
"""

import json
import os
import shutil
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
CONFIG_PATH = ROOT / "config.json"
ICON_PATH = ROOT / "icon.png"
ICON_ROUND_PATH = ROOT / "icon_round.png"
FONT_PATH = ROOT / "font" / "lalezar.ttf"

APP_DIR = ROOT / "app" / "src" / "main"
RES_DIR = APP_DIR / "res"
ASSETS_DIR = APP_DIR / "assets"


def fail(msg: str):
    print(f"ERROR: {msg}", file=sys.stderr)
    sys.exit(1)


def load_config() -> dict:
    if not CONFIG_PATH.exists():
        fail("config.json not found")
    try:
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            cfg = json.load(f)
    except Exception as e:
        fail(f"config.json is not valid JSON: {e}")
    return cfg


def require(cfg: dict, path: str):
    cur = cfg
    for key in path.split("."):
        if not isinstance(cur, dict) or key not in cur:
            fail(f"missing required field in config.json: {path}")
        cur = cur[key]
    return cur


def ensure_dirs():
    for d in [
        RES_DIR / "values",
        RES_DIR / "font",
        RES_DIR / "mipmap-mdpi",
        RES_DIR / "mipmap-hdpi",
        RES_DIR / "mipmap-xhdpi",
        RES_DIR / "mipmap-xxhdpi",
        RES_DIR / "mipmap-xxxhdpi",
        ASSETS_DIR,
    ]:
        d.mkdir(parents=True, exist_ok=True)


def esc_xml(s: str) -> str:
    return (
        s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "\\'")
    )


def write_strings(name: str):
    content = f'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{esc_xml(name)}</string>
</resources>
'''
    (RES_DIR / "values" / "strings.xml").write_text(content, encoding="utf-8")


def write_colors(welcome_bg: str):
    content = f'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="welcome_bg">{"#" + welcome_bg.lstrip("#")}</color>
</resources>
'''
    (RES_DIR / "values" / "colors.xml").write_text(content, encoding="utf-8")


def write_themes():
    content = '''<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="AppTheme" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="android:windowBackground">@color/welcome_bg</item>
        <item name="android:statusBarColor">@color/welcome_bg</item>
        <item name="android:navigationBarColor">@color/welcome_bg</item>
        <item name="android:windowLightStatusBar" tools:targetApi="m">false</item>
        <item name="android:windowLightNavigationBar" tools:targetApi="o_mr1">false</item>
    </style>
</resources>
'''
    (RES_DIR / "values" / "themes.xml").write_text(content, encoding="utf-8")


def write_manifest(pkg: str, version_name: str, version_code: int):
    content = f'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:usesCleartextTraffic="true"
        android:theme="@style/AppTheme">

        <activity
            android:name=".SplashActivity"
            android:exported="true"
            android:theme="@style/AppTheme">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".OnboardingActivity" android:exported="false" />
        <activity android:name=".WelcomeActivity" android:exported="false" />
        <activity android:name=".WebActivity" android:exported="false" />
        <activity android:name=".ErrorActivity" android:exported="false" />

    </application>

</manifest>
'''
    # pkg/version_name/version_code در build.gradle.kts استفاده می‌شن،
    # نه در manifest — ولی برای آینده اینجا پارام می‌گیرن
    (APP_DIR / "AndroidManifest.xml").write_text(content, encoding="utf-8")


def copy_icon(src: Path, res: Path):
    if not src.exists():
        fail(f"missing icon file: {src}")
    img = Image.open(src).convert("RGBA")

    sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in sizes.items():
        out = res / folder
        out.mkdir(parents=True, exist_ok=True)
        resized = img.resize((size, size), Image.LANCZOS)
        resized.save(out / "ic_launcher.png", "PNG")

    # round
    if ICON_ROUND_PATH.exists():
        img2 = Image.open(ICON_ROUND_PATH).convert("RGBA")
        for folder, size in sizes.items():
            out = res / folder
            resized = img2.resize((size, size), Image.LANCZOS)
            resized.save(out / "ic_launcher_round.png", "PNG")
    else:
        # اگه آیکون گرد ندادی، همون عادی رو کپی کن
        for folder, size in sizes.items():
            out = res / folder
            resized = img.resize((size, size), Image.LANCZOS)
            resized.save(out / "ic_launcher_round.png", "PNG")


def copy_font():
    if not FONT_PATH.exists():
        fail(f"missing font file: {FONT_PATH}")
    dst = RES_DIR / "font" / "lalezar.ttf"
    shutil.copy2(FONT_PATH, dst)


def copy_config_to_assets(cfg: dict):
    dst = ASSETS_DIR / "config.json"
    dst.write_text(
        json.dumps(cfg, ensure_ascii=False, indent=2), encoding="utf-8"
    )


def write_gradle_app(pkg: str, version_name: str, version_code: int):
    content = f'''plugins {{
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}}

android {{
    namespace = "ir.apkbuilder"
    compileSdk = 34

    defaultConfig {{
        applicationId = "{pkg}"
        minSdk = 21
        targetSdk = 34
        versionCode = {version_code}
        versionName = "{version_name}"
    }}

    buildTypes {{
        release {{
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }}
    }}

    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}

    kotlinOptions {{
        jvmTarget = "17"
    }}

    buildFeatures {{
        viewBinding = false
    }}
}}

dependencies {{
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("com.google.android.material:material:1.12.0")
}}
'''
    (ROOT / "app" / "build.gradle.kts").write_text(content, encoding="utf-8")


def main():
    cfg = load_config()

    # فیلدهای اجباری (اگه نبودن -> خطا)
    name = require(cfg, "brand.name")
    pkg = require(cfg, "brand.pkg")
    ver = require(cfg, "brand.ver")
    vcode = require(cfg, "brand.vcode")
    welcome_bg = require(cfg, "welcome.bg")

    if not isinstance(vcode, int):
        fail("brand.vcode must be an integer")

    # اعتبارسنجی pkg
    if not all(c.islower() or c.isdigit() or c == "." for c in pkg):
        fail(f"brand.pkg must contain only lowercase letters, digits and dots: {pkg}")

    ensure_dirs()
    write_strings(name)
    write_colors(welcome_bg)
    write_themes()
    write_manifest(pkg, ver, vcode)
    write_gradle_app(pkg, ver, vcode)
    copy_icon(ICON_PATH, RES_DIR)
    copy_font()
    copy_config_to_assets(cfg)

    print("=== prepare.py OK ===")
    print(f"app_name   : {name}")
    print(f"pkg        : {pkg}")
    print(f"version    : {ver} ({vcode})")
    print(f"welcome_bg : {welcome_bg}")


if __name__ == "__main__":
    main()
