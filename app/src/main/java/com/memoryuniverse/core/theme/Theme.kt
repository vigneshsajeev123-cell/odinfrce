package com.memoryuniverse.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CosmicViolet,
    onPrimary = StarlightWhite,
    primaryContainer = CosmicVioletDark,
    onPrimaryContainer = StarlightWhite,
    secondary = AstralBlue,
    onSecondary = SpaceBlack,
    secondaryContainer = DarkNebula,
    onSecondaryContainer = AstralBlue,
    tertiary = CyanPulsar,
    onTertiary = SpaceBlack,
    background = SpaceBlack,
    onBackground = StarlightWhite,
    surface = DeepVoid,
    onSurface = StarlightWhite,
    surfaceVariant = DarkNebula,
    onSurfaceVariant = StarlightSecondary,
    outline = GlassBorder,
    outlineVariant = StarlightGhost
)

@Composable
fun MemoryUniverseTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SpaceBlack.toArgb()
            window.navigationBarColor = SpaceBlack.toArgb()
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = false
            windowInsetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
