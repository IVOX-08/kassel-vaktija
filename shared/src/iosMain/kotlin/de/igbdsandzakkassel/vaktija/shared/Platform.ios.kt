package de.igbdsandzakkassel.vaktija.shared

import platform.UIKit.UIDevice

internal actual fun platformNameImpl(): String =
    UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
