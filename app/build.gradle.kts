import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// Release signing credentials, loaded from the gitignored keystore.properties (if present).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) FileInputStream(keystorePropertiesFile).use { load(it) }
}

// Gemini (announcement translation) API key, from the gitignored gemini.properties (if present).
// Empty when absent → the app falls back to on-device ML Kit translation, so it still builds/runs.
val geminiPropertiesFile = rootProject.file("gemini.properties")
val geminiProperties = Properties().apply {
    if (geminiPropertiesFile.exists()) FileInputStream(geminiPropertiesFile).use { load(it) }
}
val geminiApiKey = (geminiProperties["GEMINI_API_KEY"] as String?).orEmpty()

android {
    namespace = "de.igbdsandzakkassel.vaktija"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.igbdsandzakkassel.vaktija"
        minSdk = 26
        targetSdk = 36
        versionCode = 19
        versionName = "1.2.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Locales the app ships translations for. Keep in sync with res/values-* and
        // res/xml/locales_config.xml. Adding a new values-XX/ should "just work".
        resourceConfigurations += listOf("bs", "de", "ar", "tr", "sq", "en", "ur", "ru")

        // Gemini API key for high-quality announcement translation (admin device only, at post time).
        // Supplied via gitignored gemini.properties; empty → ML Kit fallback.
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            // R8 on. Play measures how much of the code is obfuscated and warns below 25 %; with
            // this off it sat at 1 %. See proguard-rules.pro — every keep rule there answers a
            // specific place where the app would otherwise break, and the dangerous one is enum
            // constant names, which are written into DataStore, Intents and Firestore as text.
            isMinifyEnabled = true
            // Deliberately NOT enabled: the adhan and notification sounds are looked up by name at
            // runtime (resources.getIdentifier(..., "raw", ...)), so every sound file looks unused
            // to the resource shrinker. It would drop them and the app would simply stay silent at
            // prayer time. This is unrelated to the Play warning, which is about code.
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    bundle {
        // The in-app language picker lets users choose any of the 8 languages regardless of their
        // device language. Play's per-language bundle splits would only install the device-language
        // resources, so any other choice fell back to the system locale. Ship all languages instead.
        language {
            enableSplit = false
        }
    }
}

dependencies {
    // --- AndroidX core / lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat) // per-app language via AppCompatDelegate
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    // --- Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- DI (Hilt) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // --- Storage / background (used from Phase 1 onward) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // --- Networking (used from Phase 1 onward) ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // --- Prayer-time calculation (month calendar) ---
    implementation(libs.adhan2)
    implementation(libs.kotlinx.datetime)

    // --- Widgets (Phase 5) ---
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // --- Permissions helper (Phase 3+) ---
    implementation(libs.accompanist.permissions)

    // --- Firebase (admin-edited community rules + news) ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging) // instant announcement push (activated once a Cloud Function is deployed)
    implementation(libs.kotlinx.coroutines.play.services)

    // --- Google Play in-app updates (prompt users to update when a newer version is published) ---
    implementation(libs.play.app.update)

    // --- On-device translation (auto-translate announcements; models download on demand) ---
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)

    // --- QR codes on the TV board, drawn from a link that Firestore can change ---
    // Pure Java, no Android dependency: the encoder only turns a string into a bit matrix and
    // we paint it ourselves, so nothing here has to be kept in step with the Compose version.
    implementation(libs.zxing.core)
}
