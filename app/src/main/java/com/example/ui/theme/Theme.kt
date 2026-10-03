package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TrustBlue,
    onPrimary = Color.White,
    primaryContainer = TrustBlueDark,
    onPrimaryContainer = TrustBlueLight,
    secondary = CreditEmerald,
    onSecondary = Color.Black,
    secondaryContainer = CreditEmeraldDark,
    onSecondaryContainer = CreditEmeraldLight,
    tertiary = DebitCoral,
    onTertiary = Color.White,
    tertiaryContainer = DebitCoralDark,
    onTertiaryContainer = DebitCoralLight,
    background = DarkCanvasBase,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceLevel1,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceLevel2,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkSurfaceLevel2,
    surfaceContainerHigh = DarkSurfaceLevel3,
    outline = DarkSurfaceOutline,
    outlineVariant = DarkSurfaceOutline
)

private val LightColorScheme = lightColorScheme(
    primary = TrustBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = TrustBlueDark,
    secondary = CreditEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = CreditEmeraldDark,
    tertiary = DebitCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEE2E2),
    onTertiaryContainer = DebitCoralDark,
    background = LightCanvasBase,
    onBackground = LightTextPrimary,
    surface = LightSurfaceLevel1,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceLevel2,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurfaceLevel2,
    surfaceContainerHigh = LightSurfaceLevel3,
    outline = LightSurfaceOutline,
    outlineVariant = LightSurfaceOutline
)

@Composable
fun VyaparSetuTheme(
    darkTheme: Boolean = true, // User selectable merchant dark/light mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Keep backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    VyaparSetuTheme(darkTheme = darkTheme, content = content)
}
