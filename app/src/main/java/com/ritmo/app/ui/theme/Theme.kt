package com.ritmo.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.ritmo.app.settings.AccentTheme
import com.ritmo.app.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF65558F), secondary = Color(0xFF625B71), tertiary = Color(0xFF7D5260),
    background = Color(0xFFFFF8F7), surface = Color(0xFFFFF8F7), surfaceVariant = Color(0xFFF1ECF4),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF), secondary = Color(0xFFCCC2DC), tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF141218), surface = Color(0xFF141218), surfaceVariant = Color(0xFF27242C),
)

@Composable
fun RitmoTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, accent: AccentTheme = AccentTheme.OCEAN, customColor: Long = 0xFF006C84, content: @Composable () -> Unit) {
    val dark = when (themeMode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val context = LocalContext.current
    val primary = when (accent) { AccentTheme.OCEAN -> Color(0xFF006C84); AccentTheme.VIOLET -> Color(0xFF6750A4); AccentTheme.FOREST -> Color(0xFF386A20); AccentTheme.SUNSET -> Color(0xFF9C4235); AccentTheme.CUSTOM -> Color(customColor) }
    val colors = themedScheme(primary, dark)
    MaterialTheme(colorScheme = colors, typography = RitmoTypography, content = content)
}

private fun themedScheme(seed: Color, dark: Boolean): androidx.compose.material3.ColorScheme {
    val primary = if (dark) lerp(seed, Color.White, .32f) else seed
    val onPrimary = if (primary.luminance() > .48f) Color.Black else Color.White
    val secondary = if (dark) lerp(seed, Color.White, .48f) else lerp(seed, Color(0xFF4F5960), .38f)
    val tertiarySeed = Color(seed.blue, seed.red, seed.green)
    val tertiary = if (dark) lerp(tertiarySeed, Color.White, .38f) else lerp(tertiarySeed, seed, .25f)
    return if (dark) darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = lerp(seed, Color.Black, .48f), onPrimaryContainer = lerp(seed, Color.White, .72f),
        secondary = secondary, secondaryContainer = lerp(seed, Color.Black, .62f), onSecondaryContainer = lerp(seed, Color.White, .82f),
        tertiary = tertiary, tertiaryContainer = lerp(tertiarySeed, Color.Black, .58f), onTertiaryContainer = lerp(tertiarySeed, Color.White, .82f),
        background = lerp(seed, Color.Black, .91f), surface = lerp(seed, Color.Black, .90f),
        surfaceVariant = lerp(seed, Color.Black, .76f), surfaceTint = primary,
        surfaceContainerLowest = lerp(seed, Color.Black, .94f), surfaceContainerLow = lerp(seed, Color.Black, .88f),
        surfaceContainer = lerp(seed, Color.Black, .84f), surfaceContainerHigh = lerp(seed, Color.Black, .79f), surfaceContainerHighest = lerp(seed, Color.Black, .73f),
    ) else lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = lerp(seed, Color.White, .78f), onPrimaryContainer = lerp(seed, Color.Black, .55f),
        secondary = secondary, secondaryContainer = lerp(seed, Color.White, .84f), onSecondaryContainer = lerp(seed, Color.Black, .58f),
        tertiary = tertiary, tertiaryContainer = lerp(tertiarySeed, Color.White, .80f), onTertiaryContainer = lerp(tertiarySeed, Color.Black, .58f),
        background = lerp(seed, Color.White, .965f), surface = lerp(seed, Color.White, .97f),
        surfaceVariant = lerp(seed, Color.White, .88f), surfaceTint = primary,
        surfaceContainerLowest = Color.White, surfaceContainerLow = lerp(seed, Color.White, .94f),
        surfaceContainer = lerp(seed, Color.White, .91f), surfaceContainerHigh = lerp(seed, Color.White, .87f), surfaceContainerHighest = lerp(seed, Color.White, .82f),
    )
}
