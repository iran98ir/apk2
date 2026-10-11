// ⚠️ این فایل فقط placeholder است.
// موقع بیلد توی GitHub Actions، اسکریپت scripts/prepare.py
// این فایل رو دوباره با مقادیر config.json می‌سازه و بازنویسی می‌کنه.
// پس اگه دستی چیزی اینجا تغییر بدی، موقع بیلد پاک میشه.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.apkbuilder"
    compileSdk = 34

    defaultConfig {
        applicationId = "ir.apkbuilder.placeholder"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("com.google.android.material:material:1.12.0")
}
