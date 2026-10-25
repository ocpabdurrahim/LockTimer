package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberBluePrimary,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = CyberTealSecondary,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFF73F8E7),
    tertiary = AmberWarning,
    onTertiary = Color(0xFF452B00),
    background = DarkNavyBase,
    onBackground = TextHighEmphasisDark,
    surface = DarkNavySurface,
    onSurface = TextHighEmphasisDark,
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = TextMediumEmphasisDark,
    outline = DarkBorder,
    error = CrimsonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyberBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF006A60),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF73F8E7),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF865300),
    onTertiary = Color.White,
    background = LightBase,
    onBackground = TextHighEmphasisLight,
    surface = LightSurface,
    onSurface = TextHighEmphasisLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextMediumEmphasisLight,
    outline = LightBorder,
    error = CrimsonError,
    onError = Color.White
)

@Composable
fun TabLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional cyber styling
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
