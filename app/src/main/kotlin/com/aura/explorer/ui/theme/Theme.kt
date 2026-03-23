package com.aura.explorer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary             = Color(0xFFFFCC80),
    onPrimary           = Color(0xFF3E2200),
    primaryContainer    = Color(0xFF5A3300),
    onPrimaryContainer  = Color(0xFFFFDDB3),
    secondary           = Color(0xFFE2BF91),
    onSecondary         = Color(0xFF412D05),
    secondaryContainer  = Color(0xFF5A431A),
    onSecondaryContainer= Color(0xFFFFDDAD),
    tertiary            = Color(0xFFB8CEA1),
    onTertiary          = Color(0xFF243516),
    tertiaryContainer   = Color(0xFF3A4C2A),
    onTertiaryContainer = Color(0xFFD4EABB),
    background          = Color(0xFF1A1110),
    onBackground        = Color(0xFFF0DDD8),
    surface             = Color(0xFF1A1110),
    onSurface           = Color(0xFFF0DDD8),
    surfaceVariant      = Color(0xFF52443C),
    onSurfaceVariant    = Color(0xFFD7C3B9),
    outline             = Color(0xFF9F8D84),
    outlineVariant      = Color(0xFF52443C),
    inverseSurface      = Color(0xFFF0DDD8),
    inverseOnSurface    = Color(0xFF392E2B),
    inversePrimary      = Color(0xFF7B4400),
    surfaceTint         = Color(0xFFFFCC80),
)

private val LightColors = lightColorScheme(
    primary             = Color(0xFF7B4400),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDDB3),
    onPrimaryContainer  = Color(0xFF281400),
    secondary           = Color(0xFF735A2F),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDDAD),
    onSecondaryContainer= Color(0xFF281805),
    tertiary            = Color(0xFF506440),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFD4EABB),
    onTertiaryContainer = Color(0xFF0F2004),
    background          = Color(0xFFFFFBFF),
    onBackground        = Color(0xFF201A17),
    surface             = Color(0xFFFFFBFF),
    onSurface           = Color(0xFF201A17),
    surfaceVariant      = Color(0xFFF3DDD3),
    onSurfaceVariant    = Color(0xFF52443C),
    outline             = Color(0xFF85736A),
    outlineVariant      = Color(0xFFD7C3B9),
    inverseSurface      = Color(0xFF362F2C),
    inverseOnSurface    = Color(0xFFFBEEE9),
    inversePrimary      = Color(0xFFFFCC80),
    surfaceTint         = Color(0xFF7B4400),
)

@Composable
fun AuraTheme(
    darkTheme    : Boolean = isSystemInDarkTheme(),
    dynamicColor : Boolean = true,
    content      : @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AuraTypography,
        shapes      = AuraShapes,
        content     = content,
    )
}
