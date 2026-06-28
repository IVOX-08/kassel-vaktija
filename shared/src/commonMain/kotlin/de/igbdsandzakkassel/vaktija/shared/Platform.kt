package de.igbdsandzakkassel.vaktija.shared

/**
 * Name of the platform the shared code is currently running on.
 *
 * Concrete function declared in commonMain, so it exports to Swift under the stable facade
 * `PlatformKt.platformName()` on every target. The platform-specific detail comes from the
 * [platformNameImpl] expect/actual seam below — the core mechanism Kotlin Multiplatform uses for
 * platform-specific code.
 */
fun platformName(): String = platformNameImpl()

/** expect/actual seam — each target supplies its own platform name. Internal: not exported to Swift. */
internal expect fun platformNameImpl(): String
