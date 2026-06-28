package de.igbdsandzakkassel.vaktija.shared

/**
 * Tiny `expect`/`actual` demonstration: returns the name of the platform the shared code is
 * currently running on. Each target (JVM, iOS, later Android) supplies its own `actual`
 * implementation — this is the core mechanism Kotlin Multiplatform uses for platform-specific code.
 */
expect fun platformName(): String
