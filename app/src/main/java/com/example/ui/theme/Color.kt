package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// =========================================================================
// VyaparSetu Design System - Luxury iOS-Grade Mercantile Palette
// =========================================================================

// --- Refined Primary & Semantic Tokens ---
val DeepNavy = Color(0xFF0A192F)
val ObsidianDark = Color(0xFF0F172A)
val SoftSurfaceWhite = Color(0xFFFAFAFC)
val EmeraldGreen = Color(0xFF00C853)

// --- Dark Theme Base Palette (Obsidian & Deep Navy Depth) ---
val DarkCanvasBase = ObsidianDark
val DarkSurfaceDark = DeepNavy
val DarkSurfaceLevel1 = Color(0xFF132238)
val DarkSurfaceLevel2 = Color(0xFF1A2E4B)
val DarkSurfaceLevel3 = Color(0xFF223A5E)
val DarkSurfaceOutline = Color(0xFF2A4365)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFFCBD5E1)
val DarkTextMuted = Color(0xFF8892B0)

// --- Light Theme Base Palette (Crisp High-Contrast Daylight Mercantile) ---
val LightCanvasBase = SoftSurfaceWhite
val LightSurfaceDark = Color(0xFFFFFFFF)
val LightSurfaceLevel1 = Color(0xFFFFFFFF)
val LightSurfaceLevel2 = Color(0xFFF1F5F9)
val LightSurfaceLevel3 = Color(0xFFE2E8F0)
val LightSurfaceOutline = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0A192F)      // Deep Navy for crisp readability
val LightTextSecondary = Color(0xFF334155)    // Slate Gray for secondary labels
val LightTextMuted = Color(0xFF64748B)        // Cool Gray for hints / placeholders

// --- Accent Tokens ---
val TrustBlue = Color(0xFF2563EB)
val TrustBlueDark = Color(0xFF1D4ED8)
val TrustBlueLight = Color(0xFF60A5FA)

val CreditEmerald = EmeraldGreen
val CreditEmeraldDark = Color(0xFF059669)
val CreditEmeraldLight = Color(0xFF34D399)
val CreditMint = CreditEmerald

val DebitCoral = Color(0xFFEF4444)
val DebitCoralDark = Color(0xFF991B1B)
val DebitCoralLight = Color(0xFFFCA5A5)

val AmberInsight = Color(0xFFF59E0B)

// --- iOS-Grade Glassmorphism & Micro-Texture Tokens ---
val GlassBorderStrokeDark = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
val GlassBorderStrokeLight = BorderStroke(0.5.dp, Color(0xFF0A192F).copy(alpha = 0.08f))
val GlassSurfaceDark = Color(0xFF132238).copy(alpha = 0.85f)
val GlassSurfaceLight = Color(0xFFFFFFFF).copy(alpha = 0.90f)

// Local composition to track theme state globally
val LocalIsDarkTheme = staticCompositionLocalOf { true }

// --- Dynamic Semantic Theme Properties ---
val CanvasBase: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkCanvasBase else LightCanvasBase

val SurfaceDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkSurfaceDark else LightSurfaceDark

val SurfaceLevel1: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkSurfaceLevel1 else LightSurfaceLevel1

val SurfaceLevel2: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkSurfaceLevel2 else LightSurfaceLevel2

val SurfaceLevel3: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkSurfaceLevel3 else LightSurfaceLevel3

val SurfaceOutline: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkSurfaceOutline else LightSurfaceOutline

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkTextPrimary else LightTextPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkTextSecondary else LightTextSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkTextMuted else LightTextMuted

val LuxuryGlassBorder: BorderStroke
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) GlassBorderStrokeDark else GlassBorderStrokeLight

val LuxuryGlassSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) GlassSurfaceDark else GlassSurfaceLight
