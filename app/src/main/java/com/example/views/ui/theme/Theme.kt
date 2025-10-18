package com.example.views.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SageGreen80,
    secondary = SageGreenGrey80,
    tertiary = SageGreenAccent80
)

private val LightColorScheme = lightColorScheme(
    primary = SageGreen40,
    secondary = SageGreenGrey40,
    tertiary = SageGreenAccent40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ViewsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Remember the color scheme to avoid recomputation
    val colorScheme = remember(darkTheme, dynamicColor) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Will be calculated once per darkTheme change
                null // Placeholder for dynamic colors
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }
    
    // Handle dynamic colors separately since they need context
    val finalColorScheme = if (colorScheme == null && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        remember(context, darkTheme) {
            if (darkTheme) {
                dynamicDarkColorScheme(context).copy(
                    primary = SageGreen80,
                    onPrimary = Color.White
                )
            } else {
                dynamicLightColorScheme(context).copy(
                    primary = SageGreen40,
                    onPrimary = Color.White
                )
            }
        }
    } else {
        colorScheme ?: LightColorScheme
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        content = content
    )
}