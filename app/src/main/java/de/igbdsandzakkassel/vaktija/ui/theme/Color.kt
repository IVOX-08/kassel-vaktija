package de.igbdsandzakkassel.vaktija.ui.theme

import androidx.compose.ui.graphics.Color

// --- Brand palette (mirrors res/values/colors.xml) ---
val BrandGreen = Color(0xFF2E7D32)
val BrandGreenDark = Color(0xFF1B5E20)
val BrandGreenLight = Color(0xFF66BB6A)
val BrandGold = Color(0xFFB8860B)
val BrandGoldLight = Color(0xFFD4AF37)

/** Tinted fills behind brand-coloured content — kept away from Material's lavender containers. */
val BrandGreenContainer = Color(0xFFDCEBDD)
val BrandGoldContainer = Color(0xFFF7EEDA)
val BrandGoldDeep = Color(0xFF5A4300)

// --- Neutrals ---
//
// Every neutral leans a few points towards the brand green rather than sitting on pure grey. A
// pure grey next to a green card reads as slightly pink by contrast; these do not.
val NearBlack = Color(0xFF1A1A1A)
val SurfaceLight = Color(0xFFFFFFFF)          // white cards
val PageBackgroundLight = Color(0xFFF4F4F4)   // page background (matches the website --bg-dark)
val SurfaceVariantLight = Color(0xFFF1F4F1)
val SurfaceContainerLight = Color(0xFFF7F9F7)
val SurfaceContainerHighLight = Color(0xFFEAEFEA)
val SurfaceDimLight = Color(0xFFE4E9E4)
val OnSurfaceMutedLight = Color(0xFF4A5A4E)   // secondary text — green-grey, never purple-grey
val OutlineLight = Color(0xFFBFCBC1)
val OutlineVariantLight = Color(0xFFDCE3DD)

// True black to exactly match the dark logo's #000000 background (blends seamlessly,
// and is OLED-friendly on the community's devices).
val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF121512)
val SurfaceVariantDark = Color(0xFF2A2F2A)
val SurfaceContainerLowestDark = Color(0xFF0A0C0A)
val SurfaceContainerLowDark = Color(0xFF141815)
val SurfaceContainerDark = Color(0xFF181D19)
val SurfaceContainerHighDark = Color(0xFF1F251F)
val SurfaceContainerHighestDark = Color(0xFF272E27)
val OnDark = Color(0xFFECECEC)
val OnSurfaceMutedDark = Color(0xFFB6C2B8)
val OutlineDark = Color(0xFF56604F)
val OutlineVariantDark = Color(0xFF333B34)

val ScrimColor = Color(0xFF000000)
