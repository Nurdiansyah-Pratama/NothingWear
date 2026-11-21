package com.nothingwear.app.domain.model

enum class WatchFaceDialStyle(val label: String) {
    DIGITAL_CENTER("Digital Center"),
    DIGITAL_STACKED("Digital Stacked"),
    ANALOG_CLASSIC("Analog Classic"),
    HYBRID_DUAL("Hybrid Dual"),
}

enum class WatchFaceRingStyle(val label: String) {
    NONE("Clean"),
    TICKS_60("60 Ticks"),
    DOTS_12("12 Dots"),
    SEGMENTED_ARC("Cyber Arc"),
    TACTICAL_COMPASS("Tactical Ring"),
}

enum class WatchFaceFontStyle(val label: String) {
    DOT_INDUSTRIAL("Industrial Mono"),
    GEOMETRIC_SANS("Geometric Sans"),
    TECH_MONO("Tech Monospace"),
    EDITORIAL_SERIF("Editorial Serif"),
}

/**
 * Watch-face widget / complication slots.
 * Health/battery slots are strictly labeled as preview placeholders until verified M6 data exists.
 */
enum class WatchFaceComplication(val label: String) {
    NONE("None"),
    DATE("Date"),
    NEXT_EVENT("Next Event"),
    TIMEZONE("Timezone"),
    BATTERY_SLOT("Battery Slot"),
    HEART_RATE_SLOT("HR Slot"),
}

/**
 * Official Mi Fitness "Kustom" Photo Watch Face templates on Xiaomi Watch S4.
 * Selecting one enables Background-Only export & safe-zone framing so Mi Fitness's
 * live watch hands or digital clock never overlap NothingWear artwork.
 */
enum class MiFitnessCustomTemplate(
    val displayName: String,
    val safeZoneNote: String,
) {
    NONE(
        displayName = "Full Preview",
        safeZoneNote = "Renders NothingWear's own clock & widgets (for design preview only).",
    ),
    CUSTOM_ANALOG(
        displayName = "Custom analog",
        safeZoneNote = "Clears center for Mi Fitness live analog hands; keeps precision Nothing bezel ring & subtle dial frame.",
    ),
    CUSTOM_ELEGANCE(
        displayName = "Custom elegance",
        safeZoneNote = "Clears center time/date & bottom battery zone for Mi Fitness 'Custom elegance' overlay.",
    ),
    CUSTOM_DIGITAL(
        displayName = "Custom digital",
        safeZoneNote = "Keeps left half dark & clean for Mi Fitness left-aligned time/date/battery; places Nothing accents on the right.",
    ),
    CUSTOM_POSTCARD(
        displayName = "Custom postcard",
        safeZoneNote = "Frames center hero graphic while keeping 9 o'clock, 3 o'clock & 6 o'clock clear for Mi Fitness time/date/day.",
    ),
    CUSTOM_DIAMOND(
        displayName = "Custom diamond",
        safeZoneNote = "Adds subtle radial halo behind center while keeping the big 4-digit stacked clock zone clean.",
    ),
}

data class WatchFaceProject(
    val id: Long = 0L,
    val name: String,
    val category: WatchFaceCategory,
    val dialStyle: WatchFaceDialStyle,
    val ringStyle: WatchFaceRingStyle,
    val fontStyle: WatchFaceFontStyle,
    val backgroundColorArgb: Long,
    val accentColorArgb: Long,
    val textColorArgb: Long,
    val is24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val timeScale: Float = 1.0f,
    val topComplication: WatchFaceComplication = WatchFaceComplication.DATE,
    val bottomComplication: WatchFaceComplication = WatchFaceComplication.NEXT_EVENT,
    val leftComplication: WatchFaceComplication = WatchFaceComplication.NONE,
    val rightComplication: WatchFaceComplication = WatchFaceComplication.NONE,
    val updatedAtMillis: Long = 0L,
) {
    companion object {
        const val TARGET_RESOLUTION_PX = 466
    }
}

data class WatchFaceExportArtifact(
    val projectName: String,
    val pngFilePath: String,
    val jsonFilePath: String,
    val savedToGallery: Boolean,
    val backgroundOnly: Boolean = false,
    val miFitnessTemplate: MiFitnessCustomTemplate = MiFitnessCustomTemplate.NONE,
    val widthPx: Int = WatchFaceProject.TARGET_RESOLUTION_PX,
    val heightPx: Int = WatchFaceProject.TARGET_RESOLUTION_PX,
    val exportedAtMillis: Long,
)

sealed interface WatchFaceExportResult {
    data class ExportedPreviewAndSpec(val artifact: WatchFaceExportArtifact) : WatchFaceExportResult
    data class Failed(val message: String) : WatchFaceExportResult
}

data class WatchFaceAssetPackArtifact(
    val projectName: String,
    val zipFilePath: String,
    val savedToDownloads: Boolean,
    val includedEntries: List<String>,
    val miFitnessTemplate: MiFitnessCustomTemplate,
    val exportedAtMillis: Long,
)

sealed interface WatchFaceAssetPackResult {
    data class Exported(val artifact: WatchFaceAssetPackArtifact) : WatchFaceAssetPackResult
    data class Failed(val message: String) : WatchFaceAssetPackResult
}

data class WatchFaceColorPreset(
    val name: String,
    val argb: Long,
)

object WatchFacePresets {

    val backgroundColors: List<WatchFaceColorPreset> = listOf(
        WatchFaceColorPreset("OLED Black", 0xFF000000),
        WatchFaceColorPreset("Carbon", 0xFF121216),
        WatchFaceColorPreset("Deep Slate", 0xFF0B111C),
        WatchFaceColorPreset("Tactical Olive", 0xFF0E1611),
        WatchFaceColorPreset("Warm Paper", 0xFFF2F1EC),
    )

    val accentColors: List<WatchFaceColorPreset> = listOf(
        WatchFaceColorPreset("Signal Red", 0xFFD71921),
        WatchFaceColorPreset("Pure White", 0xFFF5F5F5),
        WatchFaceColorPreset("Cyber Cyan", 0xFF06B6D4),
        WatchFaceColorPreset("Amber Pulse", 0xFFF59E0B),
        WatchFaceColorPreset("Emerald", 0xFF10B981),
        WatchFaceColorPreset("Cobalt", 0xFF3B82F6),
        WatchFaceColorPreset("Neon Magenta", 0xFFEC4899),
    )

    val textColors: List<WatchFaceColorPreset> = listOf(
        WatchFaceColorPreset("Crisp White", 0xFFF5F5F5),
        WatchFaceColorPreset("Soft Silver", 0xFFD4D4D8),
        WatchFaceColorPreset("Signal Red", 0xFFD71921),
        WatchFaceColorPreset("Cyber Cyan", 0xFF67E8F9),
        WatchFaceColorPreset("Obsidian", 0xFF111114),
    )

    fun defaultFor(category: WatchFaceCategory): WatchFaceProject = when (category) {
        WatchFaceCategory.NOTHING -> WatchFaceProject(
            name = "Nothing Dot Matrix",
            category = WatchFaceCategory.NOTHING,
            dialStyle = WatchFaceDialStyle.DIGITAL_STACKED,
            ringStyle = WatchFaceRingStyle.DOTS_12,
            fontStyle = WatchFaceFontStyle.DOT_INDUSTRIAL,
            backgroundColorArgb = 0xFF000000,
            accentColorArgb = 0xFFD71921,
            textColorArgb = 0xFFF5F5F5,
            is24Hour = true,
            showSeconds = true,
            timeScale = 1.05f,
            topComplication = WatchFaceComplication.DATE,
            bottomComplication = WatchFaceComplication.NEXT_EVENT,
            leftComplication = WatchFaceComplication.BATTERY_SLOT,
            rightComplication = WatchFaceComplication.TIMEZONE,
        )

        WatchFaceCategory.MINIMAL -> WatchFaceProject(
            name = "Minimal Pure",
            category = WatchFaceCategory.MINIMAL,
            dialStyle = WatchFaceDialStyle.DIGITAL_CENTER,
            ringStyle = WatchFaceRingStyle.NONE,
            fontStyle = WatchFaceFontStyle.GEOMETRIC_SANS,
            backgroundColorArgb = 0xFF000000,
            accentColorArgb = 0xFFF5F5F5,
            textColorArgb = 0xFFF5F5F5,
            is24Hour = true,
            showSeconds = false,
            timeScale = 1.1f,
            topComplication = WatchFaceComplication.DATE,
            bottomComplication = WatchFaceComplication.NONE,
            leftComplication = WatchFaceComplication.NONE,
            rightComplication = WatchFaceComplication.NONE,
        )

        WatchFaceCategory.ANALOG -> WatchFaceProject(
            name = "Precision Analog",
            category = WatchFaceCategory.ANALOG,
            dialStyle = WatchFaceDialStyle.ANALOG_CLASSIC,
            ringStyle = WatchFaceRingStyle.TICKS_60,
            fontStyle = WatchFaceFontStyle.EDITORIAL_SERIF,
            backgroundColorArgb = 0xFF121216,
            accentColorArgb = 0xFFD71921,
            textColorArgb = 0xFFF5F5F5,
            is24Hour = true,
            showSeconds = true,
            timeScale = 1.0f,
            topComplication = WatchFaceComplication.DATE,
            bottomComplication = WatchFaceComplication.NEXT_EVENT,
            leftComplication = WatchFaceComplication.NONE,
            rightComplication = WatchFaceComplication.NONE,
        )

        WatchFaceCategory.CYBERPUNK -> WatchFaceProject(
            name = "Cyber Telemetry",
            category = WatchFaceCategory.CYBERPUNK,
            dialStyle = WatchFaceDialStyle.DIGITAL_CENTER,
            ringStyle = WatchFaceRingStyle.SEGMENTED_ARC,
            fontStyle = WatchFaceFontStyle.TECH_MONO,
            backgroundColorArgb = 0xFF0B111C,
            accentColorArgb = 0xFF06B6D4,
            textColorArgb = 0xFF67E8F9,
            is24Hour = true,
            showSeconds = true,
            timeScale = 1.0f,
            topComplication = WatchFaceComplication.DATE,
            bottomComplication = WatchFaceComplication.NEXT_EVENT,
            leftComplication = WatchFaceComplication.BATTERY_SLOT,
            rightComplication = WatchFaceComplication.HEART_RATE_SLOT,
        )

        WatchFaceCategory.TACTICAL -> WatchFaceProject(
            name = "Tactical Field",
            category = WatchFaceCategory.TACTICAL,
            dialStyle = WatchFaceDialStyle.HYBRID_DUAL,
            ringStyle = WatchFaceRingStyle.TACTICAL_COMPASS,
            fontStyle = WatchFaceFontStyle.TECH_MONO,
            backgroundColorArgb = 0xFF0E1611,
            accentColorArgb = 0xFFF59E0B,
            textColorArgb = 0xFFF5F5F5,
            is24Hour = true,
            showSeconds = true,
            timeScale = 0.95f,
            topComplication = WatchFaceComplication.DATE,
            bottomComplication = WatchFaceComplication.TIMEZONE,
            leftComplication = WatchFaceComplication.BATTERY_SLOT,
            rightComplication = WatchFaceComplication.NEXT_EVENT,
        )

        WatchFaceCategory.CUSTOM -> WatchFaceProject(
            name = "Custom Studio",
            category = WatchFaceCategory.CUSTOM,
            dialStyle = WatchFaceDialStyle.HYBRID_DUAL,
            ringStyle = WatchFaceRingStyle.DOTS_12,
            fontStyle = WatchFaceFontStyle.GEOMETRIC_SANS,
            backgroundColorArgb = 0xFF121216,
            accentColorArgb = 0xFF10B981,
            textColorArgb = 0xFFF5F5F5,
            is24Hour = true,
            showSeconds = true,
            timeScale = 1.0f,
            topComplication = WatchFaceComplication.DATE,
            bottomComplication = WatchFaceComplication.NEXT_EVENT,
            leftComplication = WatchFaceComplication.NONE,
            rightComplication = WatchFaceComplication.NONE,
        )
    }
}
