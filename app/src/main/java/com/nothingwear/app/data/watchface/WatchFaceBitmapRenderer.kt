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
        val mutedInt = (textInt and 0x00FFFFFF) or 0x99000000.toInt()

        // Outer ring
        drawRing(canvas, project.ringStyle, cx, cy, radius - 14f, accentInt, mutedInt)

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
    ): Triple<String, String, Boolean> {
        val baseDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "watchface-exports")
        baseDir.mkdirs()

        val slug = project.name.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "watchface" }
        val fileBase = "${slug}_${exportedAtMillis}"

        val pngFile = File(baseDir, "$fileBase.png")
        FileOutputStream(pngFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val jsonFile = File(baseDir, "$fileBase.json")
        jsonFile.writeText(buildProjectJson(project, exportedAtMillis))

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

    private fun buildProjectJson(project: WatchFaceProject, exportedAtMillis: Long): String =
        JSONObject().apply {
            put("schemaVersion", 1)
            put("targetResolutionPx", WatchFaceProject.TARGET_RESOLUTION_PX)
            put("installableBinaryWatchFace", false)
            put("note", "466x466 preview artwork and portable NothingWear Studio spec. Not a compiled Xiaomi Watch S4 .bin/.face binary.")
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
