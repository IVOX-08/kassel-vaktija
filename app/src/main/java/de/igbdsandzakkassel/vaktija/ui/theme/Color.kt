package de.igbdsandzakkassel.vaktija.ui.theme

import androidx.compose.ui.graphics.Color

// --- Brand palette ---
//
// The protected colours from chapter 2.1 of the graphic standards of the Islamska zajednica u
// Bosni i Hercegovini, version 1.0. These are exact and are not to be nudged: the app used
// Material's green 800 and a dark goldenrod, which merely looked close.
//
//   IZ zelena   Pantone 356 C   #008348
//   IZ zlatna   Pantone 871 C   #A59573
//   Crna        Pantone Black   #000000
val BrandGreen = Color(0xFF008348)
val BrandGold = Color(0xFFA59573)

/** Darker and lighter greens for pressed states, dark mode and headings. */
val BrandGreenDark = Color(0xFF00612F)
val BrandGreenLight = Color(0xFF4CB783)

/**
 * Gold for TEXT.
 *
 * The protected gold is a light colour: as small print on a white card it reaches about 3:1, which
 * is under the line for body text. The mark always uses [BrandGold]; wording uses this darker
 * shade of the same hue on light surfaces and [BrandGoldLight] on dark ones.
 */
val BrandGoldText = Color(0xFF7C6C45)
// A light tint of the same gold, for wording ON the green card and on black. At #C9BC9A a prayer
// name sat at 2.4:1 against IZ Zelena; this clears 3:1.
val BrandGoldLight = Color(0xFFDCD2B8)

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
