package de.igbdsandzakkassel.vaktija.shared

actual fun platformName(): String = "JVM " + System.getProperty("java.version")
