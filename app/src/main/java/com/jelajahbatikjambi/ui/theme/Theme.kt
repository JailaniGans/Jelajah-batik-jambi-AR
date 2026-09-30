package com.jelajahbatikjambi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BatikGoldLight,
    onPrimary = BatikBrownDark,
    secondary = BatikMaroonLight,
    onSecondary = BatikOnDark,
    tertiary = BatikGreen,
    onTertiary = BatikOnDark,
    background = BatikSurfaceDark,
    onBackground = BatikOnDark,
    surface = BatikSurfaceDark,
    onSurface = BatikOnDark
)

private val LightColorScheme = lightColorScheme(
    primary = BatikMaroon,
    onPrimary = BatikCream,
    secondary = BatikGold,
    onSecondary = BatikBrownDark,
    tertiary = BatikGreen,
    onTertiary = BatikCream,
    background = BatikCream,
    onBackground = BatikBrownDark,
    surface = BatikCreamDark,
    onSurface = BatikBrownDark
)

@Composable
fun JelajahBatikJambiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BatikShapes,
        content = content
    )
}
