plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    // JVM target — lets us compile and run the shared unit tests on ANY machine with just a JDK
    // (no Android SDK, no Xcode). This is what verifies the shared logic on the Mac right now.
    jvm()

    // iOS targets. iosX64 = iPhone Simulator on Intel Macs (this machine); iosArm64 = real iPhones;
    // iosSimulatorArm64 = Simulator on Apple-Silicon Macs. Each builds a "Shared" framework that the
    // SwiftUI app will `import Shared`. Compiling these needs Xcode (still downloading); the JVM
    // target above does not, so we can make progress and test today.
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.adhan2)            // multiplatform prayer-time math
            implementation(libs.kotlinx.datetime)  // multiplatform dates/times (works on iOS too)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Print stdout from the shared unit tests so we can see the real computed prayer times.
tasks.withType<Test>().configureEach {
    testLogging { showStandardStreams = true }
}
