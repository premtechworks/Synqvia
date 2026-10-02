package com.github.premtechworks.synqvia.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Synqvia Redesign - Dark Navy & Cyan Palette
// ==========================================

// Screen Backgrounds (Vertical Gradient)
val BgTop = Color(0xFF050B18)
val BgBottom = Color(0xFF0A1428)

// Surfaces & Containers
val SurfaceDark = Color(0xFF101B2F)     // Cards
val SurfaceInset = Color(0xFF0B1426)    // Panels nested inside cards, bottom nav bar
val SurfaceHigh = Color(0xFF15233B)     // Circle icon buttons, unselected chips

// Borders & Dividers
val OutlineDark = Color(0xFF1F2D47)     // 1dp card borders
val DividerDark = Color(0x0FFFFFFF)     // White @ 6% alpha (0x0F / 0xFF ~ 6%)

// Typography Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8FA0BC)
val TextTertiary = Color(0xFF7A8CAB)        // Raised to #7A8CAB (5:1 contrast on card surface)
val DecorativeTertiary = Color(0xFF5F7191)  // Retained tone for purely decorative non-text elements

// Primary Accent (Cyan)
val PrimaryCyan = Color(0xFF12D4FF)
val OnPrimaryCyan = Color(0xFF04111F)

// Functional Accent Colors
val AccentBlue = Color(0xFF3D8BFF)      // Sent / "To PC"
val AccentGreen = Color(0xFF2BD97E)     // Received / "From PC" / Active
val AccentRed = Color(0xFFFF4D5E)       // Loss / Delete / Error
val AccentGold = Color(0xFFFFC21A)      // Pinned
val AccentAmber = Color(0xFFF59E0B)     // Warnings / Connecting

// Tint Helpers
fun Color.tintedContainer(alpha: Float = 0.14f): Color = this.copy(alpha = alpha)
fun Color.tintedBorder(alpha: Float = 0.35f): Color = this.copy(alpha = alpha)

// Light Scheme Palette Tokens (Prompt 5)
val PrimaryCyanLight = Color(0xFF12D4FF) // Cyan remains #12D4FF
val TextPrimaryLight = Color(0xFF0B1426)
val TextSecondaryLight = Color(0xFF5A6A85)
val TextTertiaryLight = Color(0xFF8FA0BC)

val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceInsetLight = Color(0xFFF4F6F9)
val SurfaceHighLight = Color(0xFFE2E8F0)
val OutlineLight = Color(0xFFE2E8F0)
val DividerLight = Color(0x140B1426)

val BgTopLight = Color(0xFFF4F6F9)
val BgBottomLight = Color(0xFFF4F6F9)

// ==========================================
// Legacy / Backwards Compatibility Aliases
// ==========================================
val CyanPrimary = PrimaryCyan
val CyanPrimaryDark = Color(0xFF00B0FF)
val CyanGlow = PrimaryCyan.copy(alpha = 0.3f)
val BlueAccent = AccentBlue
val BlueDeep = Color(0xFF1565C0)

val DarkNavyBackground = BgTop
val DarkNavySurface = SurfaceDark
val GlassSurface = SurfaceDark
val GlassSurfaceElevated = SurfaceHigh
val GlassSurfaceHover = SurfaceHigh
val GlassBorder = OutlineDark
val GlassBorderSubtle = OutlineDark

val StatusConnected = AccentGreen
val StatusConnectedGlow = AccentGreen.copy(alpha = 0.3f)
val StatusRetrying = AccentAmber
val StatusRetryingGlow = AccentAmber.copy(alpha = 0.3f)
val StatusOffline = AccentRed
val StatusOfflineGlow = AccentRed.copy(alpha = 0.3f)
val StatusSyncing = PrimaryCyan

val TextHighEmphasis = TextPrimary
val TextMediumEmphasis = TextSecondary
val TextDisabled = TextTertiary

val LocalSendColor = AccentBlue
val RemoteRecvColor = AccentGreen
val ConflictLoserColor = AccentRed
