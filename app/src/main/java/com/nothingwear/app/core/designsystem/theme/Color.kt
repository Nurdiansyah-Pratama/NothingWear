package com.nothingwear.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.nothingwear.app.domain.model.AccentColorOption

/**
 * Nothing-inspired minimalist color tokens.
 * Uses pure OLED dark surfaces and warm editorial paper light surfaces without
 * copying any proprietary brand assets.
 */
object NothingColors {
    // Dark palette surfaces
    val DarkBackground = Color(0xFF000000)
    val DarkSurface = Color(0xFF0D0D10)
    val DarkSurfaceContainer = Color(0xFF141418)
    val DarkSurfaceContainerHigh = Color(0xFF1C1C22)
    val DarkOnSurface = Color(0xFFF5F5F5)
    val DarkOnSurfaceMuted = Color(0xFFA1A1AA)
    val DarkOutline = Color(0xFF27272E)
    val DarkOutlineSubtle = Color(0xFF1E1E24)

    // Light palette surfaces
    val LightBackground = Color(0xFFF4F4F0)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceContainer = Color(0xFFEBEBE6)
    val LightSurfaceContainerHigh = Color(0xFFE1E1DB)
    val LightOnSurface = Color(0xFF111114)
    val LightOnSurfaceMuted = Color(0xFF52525B)
    val LightOutline = Color(0xFFD4D4CE)
    val LightOutlineSubtle = Color(0xFFE4E4DE)

    // Semantic status colors
    val StatusWarningDark = Color(0xFFF59E0B)
    val StatusWarningLight = Color(0xFFB45309)
    val StatusSuccessDark = Color(0xFF22C55E)
    val StatusSuccessLight = Color(0xFF15803D)
    val StatusErrorDark = Color(0xFFEF4444)
    val StatusErrorLight = Color(0xFFB91C1C)
}

data class AccentSpec(
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val previewSwatch: Color,
)

fun AccentColorOption.toAccentSpec(): AccentSpec = when (this) {
    AccentColorOption.SIGNAL_RED -> AccentSpec(
        darkPrimary = Color(0xFFD71921),
        darkOnPrimary = Color(0xFFFFFFFF),
        darkContainer = Color(0xFF3A0D10),
        darkOnContainer = Color(0xFFFFDADA),
        lightPrimary = Color(0xFFC4141C),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFFDE4E5),
        lightOnContainer = Color(0xFF5C070B),
        previewSwatch = Color(0xFFD71921),
    )

    AccentColorOption.MONOCHROME -> AccentSpec(
        darkPrimary = Color(0xFFF4F4F5),
        darkOnPrimary = Color(0xFF09090B),
        darkContainer = Color(0xFF27272A),
        darkOnContainer = Color(0xFFF4F4F5),
        lightPrimary = Color(0xFF18181B),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFE4E4E7),
        lightOnContainer = Color(0xFF18181B),
        previewSwatch = Color(0xFF71717A),
    )

    AccentColorOption.AMBER_PULSE -> AccentSpec(
        darkPrimary = Color(0xFFF59E0B),
        darkOnPrimary = Color(0xFF140D00),
        darkContainer = Color(0xFF382403),
        darkOnContainer = Color(0xFFFDE68A),
        lightPrimary = Color(0xFFB45309),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFFEF3C7),
        lightOnContainer = Color(0xFF451A03),
        previewSwatch = Color(0xFFF59E0B),
    )

    AccentColorOption.CYBER_CYAN -> AccentSpec(
        darkPrimary = Color(0xFF06B6D4),
        darkOnPrimary = Color(0xFF00141A),
        darkContainer = Color(0xFF062C34),
        darkOnContainer = Color(0xFFA5F3FC),
        lightPrimary = Color(0xFF0E7490),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFCFFAFE),
        lightOnContainer = Color(0xFF083344),
        previewSwatch = Color(0xFF06B6D4),
    )

    AccentColorOption.EMERALD_SIGNAL -> AccentSpec(
        darkPrimary = Color(0xFF10B981),
        darkOnPrimary = Color(0xFF00170E),
        darkContainer = Color(0xFF052E20),
        darkOnContainer = Color(0xFFA7F3D0),
        lightPrimary = Color(0xFF047857),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFD1FAE5),
        lightOnContainer = Color(0xFF064E3B),
        previewSwatch = Color(0xFF10B981),
    )

    AccentColorOption.COBALT -> AccentSpec(
        darkPrimary = Color(0xFF3B82F6),
        darkOnPrimary = Color(0xFFFFFFFF),
        darkContainer = Color(0xFF0C234A),
        darkOnContainer = Color(0xFFBFDBFE),
        lightPrimary = Color(0xFF1D4ED8),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFDBEAFE),
        lightOnContainer = Color(0xFF1E3A8A),
        previewSwatch = Color(0xFF3B82F6),
    )
}
