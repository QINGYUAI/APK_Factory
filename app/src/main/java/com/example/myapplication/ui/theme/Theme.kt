// Author: PKQ32
// Created: 2026-07-11
// Description: 深色赛博 Material3 主题（强制深色，关闭动态取色）

package com.example.myapplication.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberDarkScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00323C),
    primaryContainer = CyberCyanDim,
    onPrimaryContainer = CyberCyan,
    secondary = CyberBlue,
    onSecondary = Color(0xFF001B33),
    secondaryContainer = Color(0xFF123458),
    onSecondaryContainer = CyberBlue,
    tertiary = CyberSuccess,
    onTertiary = Color(0xFF003822),
    background = CyberBg,
    onBackground = CyberOnBg,
    surface = CyberSurface,
    onSurface = CyberOnBg,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberMuted,
    outline = CyberStroke,
    outlineVariant = Color(0xFF243B52),
    error = CyberError,
    onError = Color(0xFF3B0010),
    errorContainer = Color(0xFF5C1020),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = CyberDarkScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CyberBg.toArgb()
            window.navigationBarColor = CyberBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
