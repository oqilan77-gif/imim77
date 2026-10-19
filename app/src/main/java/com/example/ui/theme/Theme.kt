package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = OnSagePrimaryDark,
    primaryContainer = SagePrimaryContainerDark,
    onPrimaryContainer = OnSagePrimaryContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = OnSlateSecondaryDark,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = OnSlateSecondaryContainerDark,
    tertiary = WarmSandTertiary,
    onTertiary = OnWarmSandTertiary,
    tertiaryContainer = WarmSandTertiaryContainer,
    onTertiaryContainer = OnWarmSandTertiaryContainer,
    background = DeepPineBackgroundDark,
    onBackground = OnDeepPineBackgroundDark,
    surface = ObsidianSurfaceDark,
    onSurface = OnObsidianSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = OnSagePrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = OnSagePrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = OnSlateSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = OnSlateSecondaryContainer,
    tertiary = WarmSandTertiary,
    onTertiary = OnWarmSandTertiary,
    tertiaryContainer = WarmSandTertiaryContainer,
    onTertiaryContainer = OnWarmSandTertiaryContainer,
    background = AlabasterBackground,
    onBackground = OnAlabasterBackground,
    surface = PureWhiteSurface,
    onSurface = OnPureWhiteSurface,
    surfaceVariant = SoftSageSurfaceVariant,
    onSurfaceVariant = OnSoftSageSurfaceVariant,
    outline = CrispOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
