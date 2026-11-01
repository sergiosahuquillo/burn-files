package es.sergiosahuquillo.burnfiles.spike.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The only place with literal colors (constitution V). Both the Material 3 and the tv-material
 * themes are built from these tokens; every pair meets WCAG AA (≥ 4.5:1) for body text.
 */
@Immutable
data class SpikeColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val warning: Color
)

val LightTokens =
    SpikeColors(
        primary = Color(0xFFA33F00),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBCC),
        onPrimaryContainer = Color(0xFF351000),
        secondary = Color(0xFF77574A),
        onSecondary = Color(0xFFFFFFFF),
        background = Color(0xFFFFF8F6),
        onBackground = Color(0xFF231A16),
        surface = Color(0xFFFFF8F6),
        onSurface = Color(0xFF231A16),
        surfaceVariant = Color(0xFFF5DED6),
        onSurfaceVariant = Color(0xFF53433E),
        outline = Color(0xFF85736D),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        success = Color(0xFF2E6B30),
        warning = Color(0xFF7A5900)
    )

val DarkTokens =
    SpikeColors(
        primary = Color(0xFFFFB595),
        onPrimary = Color(0xFF571E00),
        primaryContainer = Color(0xFF7C2E00),
        onPrimaryContainer = Color(0xFFFFDBCC),
        secondary = Color(0xFFE7BDAD),
        onSecondary = Color(0xFF442A1F),
        background = Color(0xFF1A110E),
        onBackground = Color(0xFFF1DFD9),
        surface = Color(0xFF1A110E),
        onSurface = Color(0xFFF1DFD9),
        surfaceVariant = Color(0xFF53433E),
        onSurfaceVariant = Color(0xFFD8C2BA),
        outline = Color(0xFFA08D86),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        success = Color(0xFF8FD88A),
        warning = Color(0xFFF3C04B)
    )

/** Status colors (success/warning) that neither Material theme has a slot for. */
val LocalSpikeColors = staticCompositionLocalOf { LightTokens }
