package es.sergiosahuquillo.burnfiles.spike.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/** Dark by default (platform convention); light only when the system explicitly asks for it. */
@Composable
fun tvPrefersDark(): Boolean =
    LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_NO

@Composable
fun TvTheme(dark: Boolean = tvPrefersDark(), content: @Composable () -> Unit) {
    val tokens = if (dark) DarkTokens else LightTokens
    CompositionLocalProvider(LocalSpikeColors provides tokens) {
        MaterialTheme(colorScheme = tokens.toTv(dark), typography = TvTypography, content = content)
    }
}

/** Body text ≥ 18 sp for reading at 3 m (contracts/ui-navigation.md). */
private val TvTypography =
    Typography().let { base ->
        base.copy(
            bodyMedium = base.bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
            bodyLarge = base.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
            labelLarge = base.labelLarge.copy(fontSize = 18.sp)
        )
    }

private fun SpikeColors.toTv(dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        // Focused rows and buttons invert to these: the onSurface/surface pair, swapped.
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        border = outline,
        error = error,
        onError = onError
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        // Focused rows and buttons invert to these: the onSurface/surface pair, swapped.
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        border = outline,
        error = error,
        onError = onError
    )
}
