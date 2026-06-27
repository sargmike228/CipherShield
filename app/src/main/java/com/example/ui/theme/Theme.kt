package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BentoColorScheme = lightColorScheme(
    primary = BentoDeepViolet,
    onPrimary = BentoLavender,
    secondary = BentoDeepBlue,
    onSecondary = BentoBlue,
    tertiary = BentoDeepPink,
    onTertiary = BentoPink,
    background = BentoBackground,
    onBackground = BentoTextPrimary,
    surface = BentoLavender,
    onSurface = BentoDeepViolet,
    error = TavernRed,
    onError = BentoBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force Bento light/creative vibe
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BentoColorScheme,
        typography = Typography,
        content = content
    )
}
