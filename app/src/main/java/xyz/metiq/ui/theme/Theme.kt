package xyz.metiq.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalMetiqColors = compositionLocalOf { MetiqColors.Dark }

private val darkScheme = darkColorScheme(
    background = MetiqColors.Dark.background,
    surface = MetiqColors.Dark.foreground,
    onBackground = MetiqColors.Dark.textPrimary,
    onSurface = MetiqColors.Dark.textPrimary,
)

private val lightScheme = lightColorScheme(
    background = MetiqColors.Light.background,
    surface = MetiqColors.Light.foreground,
    onBackground = MetiqColors.Light.textPrimary,
    onSurface = MetiqColors.Light.textPrimary,
)

private fun MetiqColorTokens.withDynamic(
    scheme: ColorScheme,
    darkTheme: Boolean,
): MetiqColorTokens = copy(
    background = if (darkTheme) scheme.surfaceContainerLowest else scheme.surfaceContainerHighest,
    foreground = if (darkTheme) scheme.surfaceContainer else scheme.surfaceContainerLow,
    cellBackground = scheme.surfaceContainerHigh,
    textPrimary = scheme.onSurface,
    textSecondary = scheme.onSurface.copy(alpha = 0.50f),
    divider = scheme.onSurface.copy(alpha = 0.08f),
    subtleFill = scheme.onSurface.copy(alpha = 0.12f),
    sliderActiveFill = scheme.onSurface.copy(alpha = 0.55f),
    logo = scheme.primary,
)

@Composable
fun MetiqTheme(
    darkTheme: Boolean,
    dynamicColors: Boolean = false,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) MetiqColors.Dark else MetiqColors.Light
    val tokens: MetiqColorTokens
    val scheme: ColorScheme
    if (dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val dynamic = if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        tokens = base.withDynamic(dynamic, darkTheme)
        scheme = dynamic.copy(
            background = tokens.background,
            surface = tokens.foreground,
            onBackground = tokens.textPrimary,
            onSurface = tokens.textPrimary,
        )
    } else {
        tokens = base
        scheme = if (darkTheme) darkScheme else lightScheme
    }
    CompositionLocalProvider(LocalMetiqColors provides tokens) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MetiqTypography,
            content = content,
        )
    }
}
