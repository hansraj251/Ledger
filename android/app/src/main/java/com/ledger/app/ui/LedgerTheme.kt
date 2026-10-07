package com.ledger.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Color(0xFF3157D5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7ECFF),
    onPrimaryContainer = Color(0xFF10245F),
    secondary = Color(0xFF536174),
    secondaryContainer = Color(0xFFE8EDF5),
    onSecondaryContainer = Color(0xFF18202C),
    tertiary = Color(0xFF00866A),
    tertiaryContainer = Color(0xFFB7F2E1),
    onTertiaryContainer = Color(0xFF00382D),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF171A21),
    surface = Color.White,
    onSurface = Color(0xFF171A21),
    surfaceVariant = Color(0xFFECEEF4),
    onSurfaceVariant = Color(0xFF606573),
    outline = Color(0xFFD7DAE2),
    outlineVariant = Color(0xFFE5E7ED),
    error = Color(0xFFD33B45),
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF410006)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7C5FF),
    onPrimary = Color(0xFF142A70),
    primaryContainer = Color(0xFF273F91),
    onPrimaryContainer = Color(0xFFE0E6FF),
    secondary = Color(0xFFBBC4D4),
    secondaryContainer = Color(0xFF3B4351),
    onSecondaryContainer = Color(0xFFE0E5F0),
    tertiary = Color(0xFF65DBC0),
    tertiaryContainer = Color(0xFF005142),
    onTertiaryContainer = Color(0xFF83F8DC),
    background = Color(0xFF101217),
    onBackground = Color(0xFFE8E9EF),
    surface = Color(0xFF17191F),
    onSurface = Color(0xFFE8E9EF),
    surfaceVariant = Color(0xFF252830),
    onSurfaceVariant = Color(0xFFBEC2CD),
    outline = Color(0xFF41444D),
    outlineVariant = Color(0xFF30333A),
    error = Color(0xFFFFB3B4),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD9)
)

private val LedgerTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(
            fontWeight = FontWeight.Bold
        ),
        headlineLarge = headlineLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        headlineMedium = headlineMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        titleLarge = titleLarge.copy(
            fontWeight = FontWeight.SemiBold
        ),
        titleMedium = titleMedium.copy(
            fontWeight = FontWeight.SemiBold
        ),
        labelLarge = labelLarge.copy(
            fontWeight = FontWeight.SemiBold
        )
    )
}

@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors: ColorScheme = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = LedgerTypography,
        content = content
    )
}
