package com.kreedaankana.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand Palette ────────────────────────────────────────────────────────────
val SportGreen        = Color(0xFF1B5E20)
val SportGreenMid     = Color(0xFF2E7D32)
val SportGreenLight   = Color(0xFF4CAF50)
val SportGreenSurface = Color(0xFFE8F5E9)

val SportOrange       = Color(0xFFE65100)
val SportOrangeLight  = Color(0xFFFF6D00)
val SportOrangeSurface= Color(0xFFFFF3E0)

val SlotAvailable     = Color(0xFF43A047)
val SlotBooked        = Color(0xFFE65100)
val SlotPast          = Color(0xFF9E9E9E)
val SlotMaintenance   = Color(0xFF1565C0)

val DarkBackground    = Color(0xFF0D1B0E)
val DarkSurface       = Color(0xFF1A2E1B)
val DarkCard          = Color(0xFF1F3520)

// ── Color Schemes ────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = SportGreenMid,
    onPrimary          = Color.White,
    primaryContainer   = SportGreenSurface,
    onPrimaryContainer = SportGreen,
    secondary          = SportOrange,
    onSecondary        = Color.White,
    secondaryContainer = SportOrangeSurface,
    onSecondaryContainer = SportOrange,
    background         = Color(0xFFF9FBF9),
    onBackground       = Color(0xFF1A1C1A),
    surface            = Color.White,
    onSurface          = Color(0xFF1A1C1A),
    surfaceVariant     = Color(0xFFDCE5DC),
    error              = Color(0xFFB71C1C),
)

private val DarkColorScheme = darkColorScheme(
    primary            = SportGreenLight,
    onPrimary          = Color(0xFF003909),
    primaryContainer   = SportGreen,
    onPrimaryContainer = Color(0xFFA8F5A2),
    secondary          = SportOrangeLight,
    onSecondary        = Color(0xFF4A1800),
    secondaryContainer = SportOrange,
    onSecondaryContainer = Color(0xFFFFDBCA),
    background         = DarkBackground,
    onBackground       = Color(0xFFE0E3E0),
    surface            = DarkSurface,
    onSurface          = Color(0xFFE0E3E0),
    surfaceVariant     = DarkCard,
)

@Composable
fun KreedaAnkanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
