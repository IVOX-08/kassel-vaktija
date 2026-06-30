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
    compileSdk = 35

    defaultConfig {
        applicationId = "de.igbdsandzakkassel.vaktija"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Locales the app ships translations for. Keep in sync with res/values-* and
        // res/xml/locales_config.xml. Adding a new values-XX/ should "just work".
        resourceConfigurations += listOf("bs", "de", "ar", "tr", "sq", "en", "ur", "ru")

        // Admin's Firebase Auth UID. Used to show admin tools to the right person; the real
        // protection is the Firestore security rule on the server, which checks this same UID.
        buildConfigField("String", "ADMIN_UID", "\"1a7xqRgIYDR0RZqa3KghBlz98PK2\"")

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
            // R8/minify can be enabled later after a full release QA pass; off for now to avoid
            // shipping a subtly-stripped build without device verification.
            isMinifyEnabled = false
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
}
