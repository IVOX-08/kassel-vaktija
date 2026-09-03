package de.igbdsandzakkassel.vaktija.core.text

import android.annotation.SuppressLint

/**
 * Wraps a left-to-right value — a street address, a phone number — so it keeps its natural order
 * even when the surrounding UI is right-to-left.
 *
 * Without this, "0176 3037 2402" is rendered as "2402 3037 0176" inside an Arabic or Urdu layout,
 * and an address comes out with the house number at the wrong end. The app ships in Arabic and
 * Urdu, so this is not a hypothetical.
 *
 * LRI (U+2066) opens the isolate, PDI (U+2069) closes it.
 *
 * It lives here rather than beside one screen because it was needed in two, and the second one
 * wrote the two characters out by hand — which works, but hides what it is doing and does not
 * carry the explanation with it.
 */
// Lint flags these isolate characters as "misleading text". Here they are the opposite of
// misleading: they are what stops the text from being shown backwards.
@SuppressLint("BidiSpoofing")
fun ltr(value: String): String = "⁦$value⁩"
