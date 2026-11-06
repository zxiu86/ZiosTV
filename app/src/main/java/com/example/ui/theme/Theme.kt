package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SoualJawabDarkColorScheme = darkColorScheme(
    primary = BuzzerGold,
    onPrimary = Color.Black,
    primaryContainer = GameSurfaceElevated,
    onPrimaryContainer = BuzzerGoldBright,
    secondary = QuizBlue,
    onSecondary = Color.Black,
    secondaryContainer = GameSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = GlowGreen,
    onTertiary = Color.Black,
    background = GameBackground,
    onBackground = TextPrimary,
    surface = GameSurface,
    onSurface = TextPrimary,
    surfaceVariant = GameSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GameSurfaceBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = GameBackground.toArgb()
                window.navigationBarColor = GameBackground.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = SoualJawabDarkColorScheme,
        typography = Typography,
        content = content
    )
}
