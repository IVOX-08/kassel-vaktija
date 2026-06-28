package de.igbdsandzakkassel.vaktija.shared

internal actual fun platformNameImpl(): String = "JVM " + System.getProperty("java.version")
