package com.thalyspenha.pipoca.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = ButterDark,
    primaryContainer = ButterContainerLight,
    secondary = BucketRed,
)

private val DarkColors = darkColorScheme(
    primary = Butter,
    primaryContainer = ButterContainerDark,
    secondary = BucketRedLight,
)

/**
 * Tema do app. Segue o modo claro/escuro do sistema.
 * [dynamicColor] usa as cores do papel de parede (Android 12+); desligado por padrão
 * para manter a identidade do app. Vira configuração do usuário numa fase futura.
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
