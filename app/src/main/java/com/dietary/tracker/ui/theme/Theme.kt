package com.dietary.tracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    secondary = OrangeAccent,
    tertiary = BlueWater,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.WhiteSafe,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    secondary = OrangeAccent,
    tertiary = BlueWater,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.BlackSafe,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

@Composable
fun DietTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        androidx.compose.runtime.SideEffect {
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// Small helpers to avoid importing androidx.compose.ui.graphics.Color twice with a name clash
private object Color {
    val WhiteSafe = androidx.compose.ui.graphics.Color.White
    val BlackSafe = androidx.compose.ui.graphics.Color.Black
}
