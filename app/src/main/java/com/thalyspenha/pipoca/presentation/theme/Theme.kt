package com.thalyspenha.pipoca.presentation.theme

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

private val LightColors = lightColorScheme(
    primary = ButterDark,
    onPrimary = Color.White,
    primaryContainer = ButterContainerLight,
    onPrimaryContainer = OnButterContainerLight,
    secondary = BucketRed,
    onSecondary = Color.White,
    secondaryContainer = BucketContainerLight,
    onSecondaryContainer = OnBucketContainerLight,
    tertiary = BucketRed,
    onTertiary = Color.White,
    tertiaryContainer = BucketContainerLight,
    onTertiaryContainer = OnBucketContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

private val DarkColors = darkColorScheme(
    primary = Butter,
    onPrimary = OnButterDark,
    primaryContainer = ButterContainerDark,
    onPrimaryContainer = ButterContainerLight,
    secondary = BucketRedLight,
    onSecondary = OnBucketDark,
    secondaryContainer = BucketContainerDark,
    onSecondaryContainer = BucketContainerLight,
    tertiary = BucketRedLight,
    onTertiary = OnBucketDark,
    tertiaryContainer = BucketContainerDark,
    onTertiaryContainer = BucketContainerLight,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

/**
 * Tema do app. [darkTheme] vem da escolha nas Configurações (`MainActivity`, D-057); padrão segue o sistema. Esquemas completos
 * na paleta própria (D-056): nenhum papel cai no lilás padrão do Material.
 * [dynamicColor] usa as cores do papel de parede (Android 12+); desligado para manter a identidade do app.
 */
@Composable
fun PipocaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
