package com.example.pkdmart.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = NeutralSurface,
    primaryContainer = BrandGreenLight,
    onPrimaryContainer = BrandGreenDark,

    secondary = BrandAccent,
    onSecondary = NeutralText,
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = Color(0xFF7A5B00),

    tertiary = BrandGreenDark,
    background = NeutralBg,
    onBackground = NeutralText,
    surface = NeutralSurface,
    onSurface = NeutralText
)

@Composable
fun PkdmartTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
