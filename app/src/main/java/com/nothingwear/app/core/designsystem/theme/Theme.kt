package com.nothingwear.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.nothingwear.app.domain.model.AccentColorOption
import com.nothingwear.app.domain.model.ThemeMode

fun buildNothingColorScheme(
    darkTheme: Boolean,
    accentColor: AccentColorOption,
): ColorScheme {
    val accent = accentColor.toAccentSpec()
    return if (darkTheme) {
        darkColorScheme(
            primary = accent.darkPrimary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkContainer,
            onPrimaryContainer = accent.darkOnContainer,
            secondary = NothingColors.DarkOnSurfaceMuted,
            onSecondary = NothingColors.DarkBackground,
            secondaryContainer = NothingColors.DarkSurfaceContainerHigh,
            onSecondaryContainer = NothingColors.DarkOnSurface,
            tertiary = accent.darkPrimary,
            onTertiary = accent.darkOnPrimary,
            background = NothingColors.DarkBackground,
            onBackground = NothingColors.DarkOnSurface,
            surface = NothingColors.DarkSurface,
            onSurface = NothingColors.DarkOnSurface,
            surfaceVariant = NothingColors.DarkSurfaceContainer,
            onSurfaceVariant = NothingColors.DarkOnSurfaceMuted,
            surfaceContainerLowest = NothingColors.DarkBackground,
            surfaceContainerLow = NothingColors.DarkSurface,
            surfaceContainer = NothingColors.DarkSurfaceContainer,
            surfaceContainerHigh = NothingColors.DarkSurfaceContainerHigh,
            outline = NothingColors.DarkOutline,
            outlineVariant = NothingColors.DarkOutlineSubtle,
            error = NothingColors.StatusErrorDark,
            onError = NothingColors.DarkBackground,
        )
    } else {
        lightColorScheme(
            primary = accent.lightPrimary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightContainer,
            onPrimaryContainer = accent.lightOnContainer,
            secondary = NothingColors.LightOnSurfaceMuted,
            onSecondary = NothingColors.LightSurface,
            secondaryContainer = NothingColors.LightSurfaceContainerHigh,
            onSecondaryContainer = NothingColors.LightOnSurface,
            tertiary = accent.lightPrimary,
            onTertiary = accent.lightOnPrimary,
            background = NothingColors.LightBackground,
            onBackground = NothingColors.LightOnSurface,
            surface = NothingColors.LightSurface,
            onSurface = NothingColors.LightOnSurface,
            surfaceVariant = NothingColors.LightSurfaceContainer,
            onSurfaceVariant = NothingColors.LightOnSurfaceMuted,
            surfaceContainerLowest = NothingColors.LightSurface,
            surfaceContainerLow = NothingColors.LightBackground,
            surfaceContainer = NothingColors.LightSurfaceContainer,
            surfaceContainerHigh = NothingColors.LightSurfaceContainerHigh,
            outline = NothingColors.LightOutline,
            outlineVariant = NothingColors.LightOutlineSubtle,
            error = NothingColors.StatusErrorLight,
            onError = NothingColors.LightSurface,
        )
    }
}

@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun NothingWearTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColorOption = AccentColorOption.SIGNAL_RED,
    content: @Composable () -> Unit,
) {
    val isDark = resolveDarkTheme(themeMode)
    val colorScheme = buildNothingColorScheme(
        darkTheme = isDark,
        accentColor = accentColor,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NothingTypography,
        content = content,
    )
}
