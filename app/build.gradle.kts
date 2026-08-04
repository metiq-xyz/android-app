import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "xyz.metiq"
    compileSdk = 36

    defaultConfig {
        applicationId = "xyz.metiq"
        minSdk = 28
        targetSdk = 36
        versionCode = 30
        versionName = "0.16.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    androidResources {
        localeFilters += listOf("en", "it", "es", "fr", "pt", "pl", "zh")
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (keystoreProps.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    flavorDimensions += "store"
    productFlavors {
        create("fdroid") {
            dimension = "store"
            isDefault = true
            // F-Droid has no ratings: the "rate" action is starring the GitHub repo
            // (no direct star-action URL exists — starring is authenticated — so it
            // lands on the repo page), and written feedback goes to GitHub issues.
            buildConfigField("boolean", "SHOW_FEEDBACK_CTA", "true")
            buildConfigField("String", "STORE_RATE_URL", "\"https://github.com/metiq-xyz/android-app\"")
            buildConfigField("String", "STORE_RATE_FALLBACK_URL", "\"https://github.com/metiq-xyz/android-app\"")
            buildConfigField("String", "STORE_NAME", "\"GitHub\"")
        }
        create("play") {
            dimension = "store"
            // Play reviews are the feedback channel, so no separate feedback CTA.
            buildConfigField("boolean", "SHOW_FEEDBACK_CTA", "false")
            buildConfigField("String", "STORE_RATE_URL", "\"market://details?id=xyz.metiq\"")
            buildConfigField("String", "STORE_RATE_FALLBACK_URL", "\"https://play.google.com/store/apps/details?id=xyz.metiq\"")
            buildConfigField("String", "STORE_NAME", "\"Play Store\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)

    debugImplementation(libs.androidx.ui.tooling)
}
