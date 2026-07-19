package com.headdown.healthbridge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Green tones (primary / health)
private val Green10 = Color(0xFF002204)
private val Green20 = Color(0xFF00390D)
private val Green30 = Color(0xFF00531A)
private val Green40 = Color(0xFF1B5E20)
private val Green80 = Color(0xFF9FD89A)
private val Green90 = Color(0xFFB9F6B5)

// Blue tones (secondary / trust)
private val Blue10 = Color(0xFF001945)
private val Blue20 = Color(0xFF002D6D)
private val Blue30 = Color(0xFF00429A)
private val Blue40 = Color(0xFF0D47A1)
private val Blue80 = Color(0xFF9FCAFF)
private val Blue90 = Color(0xFFD1E4FF)

// Neutral tones
private val Gray10 = Color(0xFF191C1A)
private val Gray90 = Color(0xFFE1E3DE)
private val ErrorLight = Color(0xFFBA1A1A)
private val ErrorDark = Color(0xFFFFB4AB)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Blue40,
    onSecondary = Color.White,
    secondaryContainer = Blue90,
    onSecondaryContainer = Blue10,
    background = Color(0xFFFCFDF7),
    onBackground = Gray10,
    surface = Color(0xFFFAFDF8),
    onSurface = Gray10,
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BF),
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Blue80,
    onSecondary = Blue20,
    secondaryContainer = Blue30,
    onSecondaryContainer = Blue90,
    background = Gray10,
    onBackground = Gray90,
    surface = Gray10,
    onSurface = Gray90,
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BF),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940),
    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val HealthBridgeShapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun HealthBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = HealthBridgeShapes,
        typography = MaterialTheme.typography,
        content = content
    )
}
