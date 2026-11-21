package com.nothingwear.app.data.watchface

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.nothingwear.app.domain.model.MiFitnessCustomTemplate
import com.nothingwear.app.domain.model.WatchFaceComplication
import com.nothingwear.app.domain.model.WatchFaceDialStyle
import com.nothingwear.app.domain.model.WatchFaceFontStyle
import com.nothingwear.app.domain.model.WatchFaceProject
import com.nothingwear.app.domain.model.WatchFaceRingStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

@Singleton
class WatchFaceBitmapRenderer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun render466Bitmap(
        project: WatchFaceProject,
        nowMillis: Long,
        zoneId: ZoneId,
        nextEventLabel: String?,
        backgroundOnly: Boolean = false,
        miFitnessTemplate: MiFitnessCustomTemplate = MiFitnessCustomTemplate.NONE,
    ): Bitmap {
        val size = WatchFaceProject.TARGET_RESOLUTION_PX
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val radius = size / 2f

        val clipPath = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clipPath)

        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = project.backgroundColorArgb.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val accentInt = project.accentColorArgb.toInt()
        val textInt = project.textColorArgb.toInt()
        val mutedInt = withAlpha(textInt, 0x99)

        // Outer ring
        drawRing(canvas, project.ringStyle, cx, cy, radius - 14f, accentInt, mutedInt)

        if (backgroundOnly) {
            drawMiFitnessTemplateBackdrop(
                canvas = canvas,
                template = miFitnessTemplate,
                cx = cx,
                cy = cy,
                accentColor = accentInt,
                textColor = textInt,
            )
            canvas.restore()
            return bitmap
        }

        val zoned = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val typeface = when (project.fontStyle) {
            WatchFaceFontStyle.DOT_INDUSTRIAL, WatchFaceFontStyle.TECH_MONO -> Typeface.MONOSPACE
            WatchFaceFontStyle.GEOMETRIC_SANS -> Typeface.SANS_SERIF
            WatchFaceFontStyle.EDITORIAL_SERIF -> Typeface.SERIF
        }

        // Complications
        drawComplicationText(
            canvas = canvas,
            label = formatComplication(project.topComplication, zoned, nextEventLabel),
            x = cx,
            y = cy - 118f,
            color = accentInt,
            typeface = typeface,
        )
        drawComplicationText(
            canvas = canvas,
            label = formatComplication(project.bottomComplication, zoned, nextEventLabel),
            x = cx,
            y = cy + 138f,
            color = mutedInt,
            typeface = typeface,
        )
        drawComplicationText(
            canvas = canvas,
            label = formatComplication(project.leftComplication, zoned, nextEventLabel),
            x = cx - 122f,
            y = cy + 6f,
            color = mutedInt,
            typeface = typeface,
        )
        drawComplicationText(
            canvas = canvas,
            label = formatComplication(project.rightComplication, zoned, nextEventLabel),
            x = cx + 122f,
            y = cy + 6f,
            color = mutedInt,
            typeface = typeface,
        )

        // Time dial
        val hourPattern = if (project.is24Hour) "HH" else "hh"
        val hourStr = zoned.format(DateTimeFormatter.ofPattern(hourPattern, Locale.US))
        val minStr = zoned.format(DateTimeFormatter.ofPattern("mm", Locale.US))
        val secStr = zoned.format(DateTimeFormatter.ofPattern("ss", Locale.US))
        val scale = project.timeScale.coerceIn(0.8f, 1.25f)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textInt
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        when (project.dialStyle) {
            WatchFaceDialStyle.DIGITAL_CENTER -> {
                timePaint.textSize = 94f * scale
                canvas.drawText("$hourStr:$minStr", cx, cy + 30f * scale, timePaint)
                if (project.showSeconds) {
                    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = accentInt
                        this.typeface = Typeface.MONOSPACE
                        textSize = 26f * scale
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(":$secStr", cx, cy + 68f * scale, secPaint)
                }
            }

            WatchFaceDialStyle.DIGITAL_STACKED -> {
                timePaint.textSize = 86f * scale
                canvas.drawText(hourStr, cx, cy - 8f, timePaint)
                timePaint.color = accentInt
                canvas.drawText(minStr, cx, cy + 78f * scale, timePaint)
            }

            WatchFaceDialStyle.ANALOG_CLASSIC -> {
                drawAnalogHands(
                    canvas = canvas,
                    cx = cx,
                    cy = cy,
                    radius = radius - 36f,
                    hour = zoned.hour,
                    minute = zoned.minute,
                    second = zoned.second,
                    showSeconds = project.showSeconds,
                    handColor = textInt,
                    accentColor = accentInt,
                )
            }

            WatchFaceDialStyle.HYBRID_DUAL -> {
                drawAnalogHands(
                    canvas = canvas,
                    cx = cx,
                    cy = cy,
                    radius = radius - 42f,
                    hour = zoned.hour,
                    minute = zoned.minute,
                    second = zoned.second,
                    showSeconds = project.showSeconds,
                    handColor = mutedInt,
                    accentColor = accentInt,
                )
                timePaint.textSize = 42f * scale
                canvas.drawText("$hourStr:$minStr", cx, cy + 84f, timePaint)
            }
        }

        canvas.restore()
        return bitmap
    }

    fun exportToFiles(
        project: WatchFaceProject,
        bitmap: Bitmap,
        exportedAtMillis: Long,
        saveToGallery: Boolean,
        backgroundOnly: Boolean = false,
        miFitnessTemplate: MiFitnessCustomTemplate = MiFitnessCustomTemplate.NONE,
    ): Triple<String, String, Boolean> {
        val baseDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "watchface-exports")
        baseDir.mkdirs()

        val slug = project.name.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "watchface" }
        val suffix = if (backgroundOnly) {
            if (miFitnessTemplate != MiFitnessCustomTemplate.NONE) {
                "_bg_${miFitnessTemplate.name.lowercase(Locale.US)}"
            } else {
                "_bg_only"
            }
        } else {
            ""
        }
        val fileBase = "${slug}${suffix}_${exportedAtMillis}"

        val pngFile = File(baseDir, "$fileBase.png")
        FileOutputStream(pngFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val jsonFile = File(baseDir, "$fileBase.json")
        jsonFile.writeText(buildProjectJson(project, exportedAtMillis, backgroundOnly, miFitnessTemplate))

        var gallerySaved = false
        if (saveToGallery && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            gallerySaved = savePngToMediaStore("$fileBase.png", bitmap)
        }

        return Triple(pngFile.absolutePath, jsonFile.absolutePath, gallerySaved)
    }

    private fun savePngToMediaStore(fileName: String, bitmap: Bitmap): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NothingWear")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: false
        }.getOrDefault(false)
    }

    private fun buildProjectJson(
        project: WatchFaceProject,
        exportedAtMillis: Long,
        backgroundOnly: Boolean,
        miFitnessTemplate: MiFitnessCustomTemplate,
    ): String =
        JSONObject().apply {
            put("schemaVersion", 1)
            put("targetResolutionPx", WatchFaceProject.TARGET_RESOLUTION_PX)
            put("installableBinaryWatchFace", false)
            put("backgroundOnly", backgroundOnly)
            put("miFitnessTemplate", miFitnessTemplate.name)
            put(
                "note",
                if (backgroundOnly) {
                    "466x466 clean background artwork for Mi Fitness Custom Photo Watch Face (${miFitnessTemplate.displayName}). Hands and clock text intentionally omitted to prevent overlap."
                } else {
                    "466x466 preview artwork and portable NothingWear Studio spec. Not a compiled Xiaomi Watch S4 .bin/.face binary."
                },
            )
            put("exportedAtMillis", exportedAtMillis)
            put("name", project.name)
            put("category", project.category.name)
            put("dialStyle", project.dialStyle.name)
            put("ringStyle", project.ringStyle.name)
            put("fontStyle", project.fontStyle.name)
            put("backgroundColorHex", String.format("#%08X", project.backgroundColorArgb))
            put("accentColorHex", String.format("#%08X", project.accentColorArgb))
            put("textColorHex", String.format("#%08X", project.textColorArgb))
            put("is24Hour", project.is24Hour)
            put("showSeconds", project.showSeconds)
            put("timeScale", project.timeScale.toDouble())
            put("topComplication", project.topComplication.name)
            put("bottomComplication", project.bottomComplication.name)
            put("leftComplication", project.leftComplication.name)
            put("rightComplication", project.rightComplication.name)
        }.toString(2)

    private fun drawMiFitnessTemplateBackdrop(
        canvas: Canvas,
        template: MiFitnessCustomTemplate,
        cx: Float,
        cy: Float,
        accentColor: Int,
        textColor: Int,
    ) {
        val subtleStroke = withAlpha(textColor, 0x38)
        val midStroke = withAlpha(textColor, 0x58)
        val softAccent = withAlpha(accentColor, 0x3A)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (template) {
            MiFitnessCustomTemplate.NONE -> Unit

            MiFitnessCustomTemplate.CUSTOM_ANALOG -> {
                // Precision inner dial track + dotted inner ring + cardinal brackets; center kept clean for live hands
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.6f
                paint.color = subtleStroke
                canvas.drawCircle(cx, cy, 166f, paint)

                paint.style = Paint.Style.FILL
                for (i in 0 until 24) {
                    val angle = Math.toRadians((i * 15.0) - 90.0)
                    paint.color = if (i % 6 == 0) withAlpha(accentColor, 0x88) else subtleStroke
                    canvas.drawCircle(
                        cx + (cos(angle) * 114f).toFloat(),
                        cy + (sin(angle) * 114f).toFloat(),
                        if (i % 6 == 0) 2.6f else 1.6f,
                        paint,
                    )
                }

                // Cardinal markers at 12/3/6/9 o'clock inside outer ring
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90.0) - 90.0)
                    paint.color = if (i == 0) accentColor else midStroke
                    paint.strokeWidth = if (i == 0) 4.5f else 3f
                    canvas.drawLine(
                        cx + (cos(angle) * 156f).toFloat(),
                        cy + (sin(angle) * 156f).toFloat(),
                        cx + (cos(angle) * 176f).toFloat(),
                        cy + (sin(angle) * 176f).toFloat(),
                        paint,
                    )
                }

                // Center pivot halo where Mi Fitness live hand cap sits
                paint.strokeWidth = 1.6f
                paint.color = midStroke
                canvas.drawCircle(cx, cy, 14f, paint)
            }

            MiFitnessCustomTemplate.CUSTOM_ELEGANCE -> {
                // Clean center time/date & bottom battery slot; sleek perimeter frame with bottom gap
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.8f
                paint.color = subtleStroke
                val outerOval = RectF(cx - 190f, cy - 190f, cx + 190f, cy + 190f)
                canvas.drawArc(outerOval, 114f, 312f, false, paint)

                // Left & right Nothing micro-dot accents framing the center time without touching it
                paint.style = Paint.Style.FILL
                for (deg in listOf(-24, -12, 0, 12, 24, 156, 168, 180, 192, 204)) {
                    val angle = Math.toRadians(deg.toDouble())
                    val isMid = deg == 0 || deg == 180
                    paint.color = if (isMid) accentColor else midStroke
                    canvas.drawCircle(
                        cx + (cos(angle) * 172f).toFloat(),
                        cy + (sin(angle) * 172f).toFloat(),
                        if (isMid) 3.4f else 2.0f,
                        paint,
                    )
                }

                // Top 12 o'clock Nothing red signal pill
                paint.color = accentColor
                canvas.drawCircle(cx, cy - 172f, 4.5f, paint)
            }

            MiFitnessCustomTemplate.CUSTOM_DIGITAL -> {
                // Left half stays clean/dark for Mi Fitness 9:28 / 8/16 / 80%; right half carries Nothing industrial motif
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.8f
                paint.color = subtleStroke
                val dividerX = cx + 28f
                canvas.drawLine(dividerX, cy - 108f, dividerX, cy + 108f, paint)

                paint.style = Paint.Style.FILL
                paint.color = accentColor
                canvas.drawCircle(dividerX, cy - 114f, 3.5f, paint)
                canvas.drawCircle(dividerX, cy + 114f, 3.5f, paint)

                // Right-half Nothing dot-matrix telemetry block (5 cols x 9 rows)
                val startX = cx + 52f
                val startY = cy - 76f
                val step = 19f
                for (row in 0 until 9) {
                    for (col in 0 until 5) {
                        val px = startX + col * step
                        val py = startY + row * step
                        val dx = px - cx
                        val dy = py - cy
                        if (dx * dx + dy * dy <= 168f * 168f) {
                            val isAccentDot = (row == col + 1) || (row == 7 - col && col >= 2)
                            paint.color = if (isAccentDot) accentColor else subtleStroke
                            canvas.drawCircle(px, py, if (isAccentDot) 3.2f else 2.2f, paint)
                        }
                    }
                }

                // Right-side accent arc
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3.5f
                paint.strokeCap = Paint.Cap.ROUND
                paint.color = softAccent
                val rightArc = RectF(cx - 182f, cy - 182f, cx + 182f, cy + 182f)
                canvas.drawArc(rightArc, -44f, 88f, false, paint)
            }

            MiFitnessCustomTemplate.CUSTOM_POSTCARD -> {
                // 9 o'clock (time), 3 o'clock (date), 6 o'clock (FRI) kept crystal clear; Nothing Glyph centerpiece in center
                val glyphCy = cy - 14f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND

                paint.strokeWidth = 4f
                paint.color = midStroke
                val outerGlyph = RectF(cx - 72f, glyphCy - 72f, cx + 72f, glyphCy + 72f)
                canvas.drawArc(outerGlyph, -145f, 110f, false, paint)
                canvas.drawArc(outerGlyph, 35f, 110f, false, paint)

                paint.strokeWidth = 3f
                paint.color = withAlpha(accentColor, 0xCC)
                val innerGlyph = RectF(cx - 46f, glyphCy - 46f, cx + 46f, glyphCy + 46f)
                canvas.drawArc(innerGlyph, -60f, 120f, false, paint)

                // Center dot-matrix crosshair
                paint.style = Paint.Style.FILL
                for (i in -2..2) {
                    paint.color = if (i == 0) accentColor else midStroke
                    canvas.drawCircle(cx + i * 11f, glyphCy, if (i == 0) 3.8f else 2.2f, paint)
                    if (i != 0) {
                        canvas.drawCircle(cx, glyphCy + i * 11f, 2.2f, paint)
                    }
                }

                // 3 top accent indicator dots
                for (i in -1..1) {
                    paint.color = if (i == 0) accentColor else subtleStroke
                    canvas.drawCircle(cx + i * 16f, cy - 122f, 3.2f, paint)
                }

                // Subtle bottom framing line above 6 o'clock FRI slot (clear of text)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.6f
                paint.color = subtleStroke
                canvas.drawLine(cx - 38f, cy + 114f, cx + 38f, cy + 114f, paint)
            }

            MiFitnessCustomTemplate.CUSTOM_DIAMOND -> {
                // Soft concentric radial aura behind the big stacked 09/28 digits + 4 diagonal corner accents
                paint.style = Paint.Style.FILL
                paint.color = withAlpha(accentColor, 0x16)
                canvas.drawCircle(cx, cy, 138f, paint)
                paint.color = withAlpha(accentColor, 0x1C)
                canvas.drawCircle(cx, cy, 94f, paint)
                paint.color = withAlpha(accentColor, 0x14)
                canvas.drawCircle(cx, cy, 52f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f
                paint.color = subtleStroke
                canvas.drawCircle(cx, cy, 152f, paint)

                // 4 diagonal diamond corner ticks (45°, 135°, 225°, 315°)
                paint.strokeWidth = 3f
                paint.strokeCap = Paint.Cap.ROUND
                for (deg in listOf(45, 135, 225, 315)) {
                    val angle = Math.toRadians(deg.toDouble())
                    paint.color = withAlpha(accentColor, 0xCC)
                    canvas.drawLine(
                        cx + (cos(angle) * 164f).toFloat(),
                        cy + (sin(angle) * 164f).toFloat(),
                        cx + (cos(angle) * 184f).toFloat(),
                        cy + (sin(angle) * 184f).toFloat(),
                        paint,
                    )
                }
            }
        }
    }

    private fun withAlpha(colorInt: Int, alpha: Int): Int =
        (colorInt and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

    private fun drawRing(
        canvas: Canvas,
        ringStyle: WatchFaceRingStyle,
        cx: Float,
        cy: Float,
        radius: Float,
        accentColor: Int,
        mutedColor: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (ringStyle) {
            WatchFaceRingStyle.NONE -> Unit

            WatchFaceRingStyle.TICKS_60 -> {
                for (i in 0 until 60) {
                    val isMajor = i % 5 == 0
                    val angle = Math.toRadians((i * 6.0) - 90.0)
                    val inner = radius - if (isMajor) 18f else 9f
                    paint.color = if (isMajor) accentColor else mutedColor
                    paint.strokeWidth = if (isMajor) 3.5f else 1.5f
                    canvas.drawLine(
                        cx + (cos(angle) * inner).toFloat(),
                        cy + (sin(angle) * inner).toFloat(),
                        cx + (cos(angle) * radius).toFloat(),
                        cy + (sin(angle) * radius).toFloat(),
                        paint,
                    )
                }
            }

            WatchFaceRingStyle.DOTS_12 -> {
                paint.style = Paint.Style.FILL
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30.0) - 90.0)
                    val isCardinal = i % 3 == 0
                    paint.color = if (isCardinal) accentColor else mutedColor
                    canvas.drawCircle(
                        cx + (cos(angle) * (radius - 8f)).toFloat(),
                        cy + (sin(angle) * (radius - 8f)).toFloat(),
                        if (isCardinal) 5.5f else 3f,
                        paint,
                    )
                }
            }

            WatchFaceRingStyle.SEGMENTED_ARC -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                for (i in 0 until 12) {
                    paint.color = if (i % 3 == 0) accentColor else mutedColor
                    canvas.drawArc(oval, i * 30f + 4f, 22f, false, paint)
                }
            }

            WatchFaceRingStyle.TACTICAL_COMPASS -> {
                paint.style = Paint.Style.STROKE
                paint.color = mutedColor
                paint.strokeWidth = 2f
                canvas.drawCircle(cx, cy, radius - 4f, paint)
                for (i in 0 until 24) {
                    val angle = Math.toRadians((i * 15.0) - 90.0)
                    val isCardinal = i % 6 == 0
                    paint.color = if (isCardinal) accentColor else mutedColor
                    paint.strokeWidth = if (isCardinal) 4f else 2f
                    val inner = radius - if (isCardinal) 22f else 12f
                    canvas.drawLine(
                        cx + (cos(angle) * inner).toFloat(),
                        cy + (sin(angle) * inner).toFloat(),
                        cx + (cos(angle) * (radius - 4f)).toFloat(),
                        cy + (sin(angle) * (radius - 4f)).toFloat(),
                        paint,
                    )
                }
            }
        }
    }

    private fun drawAnalogHands(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        hour: Int,
        minute: Int,
        second: Int,
        showSeconds: Boolean,
        handColor: Int,
        accentColor: Int,
    ) {
        val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val hourAngle = Math.toRadians((((hour % 12) + minute / 60.0) * 30.0) - 90.0)
        handPaint.color = handColor
        handPaint.strokeWidth = 9f
        canvas.drawLine(
            cx,
            cy,
            cx + (cos(hourAngle) * radius * 0.52f).toFloat(),
            cy + (sin(hourAngle) * radius * 0.52f).toFloat(),
            handPaint,
        )

        val minAngle = Math.toRadians(((minute + second / 60.0) * 6.0) - 90.0)
        handPaint.strokeWidth = 6f
        canvas.drawLine(
            cx,
            cy,
            cx + (cos(minAngle) * radius * 0.78f).toFloat(),
            cy + (sin(minAngle) * radius * 0.78f).toFloat(),
            handPaint,
        )

        if (showSeconds) {
            val secAngle = Math.toRadians((second * 6.0) - 90.0)
            handPaint.color = accentColor
            handPaint.strokeWidth = 3f
            canvas.drawLine(
                cx,
                cy,
                cx + (cos(secAngle) * radius * 0.86f).toFloat(),
                cy + (sin(secAngle) * radius * 0.86f).toFloat(),
                handPaint,
            )
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 8f, dotPaint)
    }

    private fun drawComplicationText(
        canvas: Canvas,
        label: String?,
        x: Float,
        y: Float,
        color: Int,
        typeface: Typeface,
    ) {
        if (label.isNullOrBlank()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.typeface = typeface
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label.take(22), x, y, paint)
    }

    private fun formatComplication(
        slot: WatchFaceComplication,
        zoned: java.time.ZonedDateTime,
        nextEventLabel: String?,
    ): String? = when (slot) {
        WatchFaceComplication.NONE -> null
        WatchFaceComplication.DATE -> zoned.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)).uppercase()
        WatchFaceComplication.NEXT_EVENT -> nextEventLabel?.take(20) ?: "NO EVENT"
        WatchFaceComplication.TIMEZONE -> zoned.zone.id.substringAfterLast('/').take(10).uppercase()
        WatchFaceComplication.BATTERY_SLOT -> "BAT --%"
        WatchFaceComplication.HEART_RATE_SLOT -> "HR --"
    }
}
