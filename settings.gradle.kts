pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Kassel Vaktija"

// Shared Kotlin Multiplatform module — business logic reused by both the Android app and iOS.
include(":shared")

// The Android app module needs the Android SDK. On a machine without it (e.g. this Mac, set up only
// for the iOS/KMP work) we skip :app so :shared can still build and test on its own. On the
// Windows/Android dev machine the SDK is present, so :app is included automatically — no manual edit
// needed when moving the project between machines.
val androidSdk: java.io.File? = sequenceOf(
    System.getenv("ANDROID_HOME"),
    System.getenv("ANDROID_SDK_ROOT"),
    rootDir.resolve("local.properties").takeIf { it.exists() }?.let { lp ->
        java.util.Properties().apply { lp.inputStream().use { load(it) } }.getProperty("sdk.dir")
    },
).filterNotNull().map { java.io.File(it) }.firstOrNull { it.exists() }

if (androidSdk != null) {
    include(":app")
} else {
    println("[settings] No Android SDK found on this machine — building :shared only (skipping :app). This is normal on the iOS/Mac setup.")
}
