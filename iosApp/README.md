# iosApp — the iOS foundation app

A minimal SwiftUI app that displays today's Kassel prayer times, computed entirely by the shared
Kotlin module (`:shared`). It exists to prove the Kotlin Multiplatform → iOS toolchain end-to-end
(Gradle builds a `Shared.framework`, Xcode links it, the Simulator runs it).

## Build & run (once Xcode is installed)

```bash
# 1. Generate the Xcode project from project.yml
cd iosApp
xcodegen generate

# 2. Open it
open iosApp.xcodeproj
```

In Xcode: pick an **iPhone Simulator** scheme and press ▶︎. The "Build Kotlin Shared framework"
pre-build phase runs Gradle's `:shared:embedAndSignAppleFrameworkForXcode`, which compiles the
shared Kotlin into `Shared.framework` for the simulator's architecture (this is an **Intel** Mac, so
that is `iosX64`).

## How it links the shared code

- `project.yml` adds a pre-build script that calls `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.
- `FRAMEWORK_SEARCH_PATHS` / `OTHER_LDFLAGS` point the linker at the generated `Shared` framework.
- `ContentView.swift` does `import Shared` and calls `PrayerRowsKt.prayerRowsForToday()`.

> Note: `FRAMEWORK_SEARCH_PATHS` reflects the conventional Kotlin output path. It is verified (and
> corrected if needed) on the first real build, which requires Xcode.
