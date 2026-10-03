package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================================
// VyaparSetu Design System - Bharat Mercantile Palette (Dark & Light)
// =========================================================================

// --- Dark Theme Base Palette (Tactile Midnight Mercantile) ---
val DarkCanvasBase = Color(0xFF0B0F19)
val DarkSurfaceDark = Color(0xFF0B0F19)
val DarkSurfaceLevel1 = Color(0xFF111827)
val DarkSurfaceLevel2 = Color(0xFF161F30)
val DarkSurfaceLevel3 = Color(0xFF1E293B)
val DarkSurfaceOutline = Color(0xFF334155)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFFCBD5E1)
val DarkTextMuted = Color(0xFF94A3B8)

// --- Light Theme Base Palette (Crisp High-Contrast Daylight Mercantile) ---
val LightCanvasBase = Color(0xFFF8FAFC)
val LightSurfaceDark = Color(0xFFFFFFFF)
val LightSurfaceLevel1 = Color(0xFFFFFFFF)
val LightSurfaceLevel2 = Color(0xFFF1F5F9)
val LightSurfaceLevel3 = Color(0xFFE2E8F0)
val LightSurfaceOutline = Color(0xFFCBD5E1)
val LightTextPrimary = Color(0xFF0F172A)      // Deep Slate / Charcoal for crisp readability
val LightTextSecondary = Color(0xFF334155)    // Slate Gray for secondary labels
val LightTextMuted = Color(0xFF64748B)        // Cool Gray for hints / placeholders

// --- Accent Tokens ---
val TrustBlue = Color(0xFF3B82F6)
val TrustBlueDark = Color(0xFF1D4ED8)
val TrustBlueLight = Color(0xFF93C5FD)

val CreditEmerald = Color(0xFF10B981)
val CreditEmeraldDark = Color(0xFF064E3B)
val CreditEmeraldLight = Color(0xFF34D399)
val CreditMint = CreditEmerald

val DebitCoral = Color(0xFFEF4444)
val DebitCoralDark = Color(0xFF7F1D1D)
val DebitCoralLight = Color(0xFFFCA5A5)

val AmberInsight = Color(0xFFF59E0B)

// Local composition to track theme state globally
val LocalIsDarkTheme = staticCompositionLocalOf { true }

// --- Dynamic Semantic Theme Properties ---
// Automatically adapt to the active theme mode in any @Composable scope
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

