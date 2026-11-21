package com.nothingwear.app.features.watchface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nothingwear.app.domain.model.WatchFaceComplication
import com.nothingwear.app.domain.model.WatchFaceDialStyle
import com.nothingwear.app.domain.model.WatchFaceFontStyle
import com.nothingwear.app.domain.model.WatchFaceProject
import com.nothingwear.app.domain.model.WatchFaceRingStyle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Live 1:1 circular preview scaled proportionally from the Xiaomi Watch S4 466 × 466 px canvas.
 */
@Composable
fun WatchFaceCanvasPreview(
    project: WatchFaceProject,
    nextEventLabel: String?,
    modifier: Modifier = Modifier,
    diameter: Dp = 236.dp,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(project.showSeconds) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(if (project.showSeconds) 1_000L else 15_000L)
        }
    }

    val zoneId = remember { ZoneId.systemDefault() }
    val zoned = Instant.ofEpochMilli(nowMillis).atZone(zoneId)

    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(Color(0xFF18181D))
            .border(3.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .clip(CircleShape),
        ) {
            val canvasPx = size.minDimension
            val scaleFactor = canvasPx / WatchFaceProject.TARGET_RESOLUTION_PX.toFloat()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = canvasPx / 2f

            val bgColor = Color(project.backgroundColorArgb)
            val accentColor = Color(project.accentColorArgb)
            val textColor = Color(project.textColorArgb)
            val mutedColor = textColor.copy(alpha = 0.62f)

            drawCircle(color = bgColor, radius = radius, center = Offset(cx, cy))

            // Ring / indices
            val ringRadius = radius - (14f * scaleFactor)
            when (project.ringStyle) {
                WatchFaceRingStyle.NONE -> Unit

                WatchFaceRingStyle.TICKS_60 -> {
                    for (i in 0 until 60) {
                        val major = i % 5 == 0
                        val angle = Math.toRadians((i * 6.0) - 90.0)
                        val inner = ringRadius - (if (major) 18f else 9f) * scaleFactor
                        drawLine(
                            color = if (major) accentColor else mutedColor,
                            start = Offset(
                                cx + (cos(angle) * inner).toFloat(),
                                cy + (sin(angle) * inner).toFloat(),
                            ),
                            end = Offset(
                                cx + (cos(angle) * ringRadius).toFloat(),
                                cy + (sin(angle) * ringRadius).toFloat(),
                            ),
                            strokeWidth = (if (major) 3.5f else 1.5f) * scaleFactor,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                WatchFaceRingStyle.DOTS_12 -> {
                    for (i in 0 until 12) {
                        val cardinal = i % 3 == 0
                        val angle = Math.toRadians((i * 30.0) - 90.0)
                        val r = ringRadius - 8f * scaleFactor
                        drawCircle(
                            color = if (cardinal) accentColor else mutedColor,
                            radius = (if (cardinal) 5.5f else 3f) * scaleFactor,
                            center = Offset(
                                cx + (cos(angle) * r).toFloat(),
                                cy + (sin(angle) * r).toFloat(),
                            ),
                        )
                    }
                }

                WatchFaceRingStyle.SEGMENTED_ARC -> {
                    val stroke = Stroke(width = 6f * scaleFactor, cap = StrokeCap.Butt)
                    val topLeft = Offset(cx - ringRadius, cy - ringRadius)
                    val arcSize = Size(ringRadius * 2f, ringRadius * 2f)
                    for (i in 0 until 12) {
                        drawArc(
                            color = if (i % 3 == 0) accentColor else mutedColor,
                            startAngle = i * 30f + 4f,
                            sweepAngle = 22f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke,
                        )
                    }
                }

                WatchFaceRingStyle.TACTICAL_COMPASS -> {
                    drawCircle(
                        color = mutedColor,
                        radius = ringRadius - 4f * scaleFactor,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f * scaleFactor),
                    )
                    for (i in 0 until 24) {
                        val cardinal = i % 6 == 0
                        val angle = Math.toRadians((i * 15.0) - 90.0)
                        val outer = ringRadius - 4f * scaleFactor
                        val inner = outer - (if (cardinal) 20f else 10f) * scaleFactor
                        drawLine(
                            color = if (cardinal) accentColor else mutedColor,
                            start = Offset(
                                cx + (cos(angle) * inner).toFloat(),
                                cy + (sin(angle) * inner).toFloat(),
                            ),
                            end = Offset(
                                cx + (cos(angle) * outer).toFloat(),
                                cy + (sin(angle) * outer).toFloat(),
                            ),
                            strokeWidth = (if (cardinal) 4f else 2f) * scaleFactor,
                        )
                    }
                }
            }

            // Analog hands for ANALOG_CLASSIC and HYBRID_DUAL
            if (project.dialStyle == WatchFaceDialStyle.ANALOG_CLASSIC ||
                project.dialStyle == WatchFaceDialStyle.HYBRID_DUAL
            ) {
                val handRadius = radius - 38f * scaleFactor
                val primaryHandColor = if (project.dialStyle == WatchFaceDialStyle.HYBRID_DUAL) {
                    mutedColor
                } else {
                    textColor
                }
                val hourAngle = Math.toRadians((((zoned.hour % 12) + zoned.minute / 60.0) * 30.0) - 90.0)
                drawLine(
                    color = primaryHandColor,
                    start = Offset(cx, cy),
                    end = Offset(
                        cx + (cos(hourAngle) * handRadius * 0.52f).toFloat(),
                        cy + (sin(hourAngle) * handRadius * 0.52f).toFloat(),
                    ),
                    strokeWidth = 9f * scaleFactor,
                    cap = StrokeCap.Round,
                )
                val minAngle = Math.toRadians(((zoned.minute + zoned.second / 60.0) * 6.0) - 90.0)
                drawLine(
                    color = primaryHandColor,
                    start = Offset(cx, cy),
                    end = Offset(
                        cx + (cos(minAngle) * handRadius * 0.78f).toFloat(),
                        cy + (sin(minAngle) * handRadius * 0.78f).toFloat(),
                    ),
                    strokeWidth = 6f * scaleFactor,
                    cap = StrokeCap.Round,
                )
                if (project.showSeconds) {
                    val secAngle = Math.toRadians((zoned.second * 6.0) - 90.0)
                    drawLine(
                        color = accentColor,
                        start = Offset(cx, cy),
                        end = Offset(
                            cx + (cos(secAngle) * handRadius * 0.86f).toFloat(),
                            cy + (sin(secAngle) * handRadius * 0.86f).toFloat(),
                        ),
                        strokeWidth = 3f * scaleFactor,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(color = accentColor, radius = 8f * scaleFactor, center = Offset(cx, cy))
            }

            // Text elements (Digital time + Complications) rendered proportionally to 466px
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                val baseTypeface = when (project.fontStyle) {
                    WatchFaceFontStyle.DOT_INDUSTRIAL, WatchFaceFontStyle.TECH_MONO -> android.graphics.Typeface.MONOSPACE
                    WatchFaceFontStyle.GEOMETRIC_SANS -> android.graphics.Typeface.SANS_SERIF
                    WatchFaceFontStyle.EDITORIAL_SERIF -> android.graphics.Typeface.SERIF
                }

                val accentInt = project.accentColorArgb.toInt()
                val textInt = project.textColorArgb.toInt()
                val mutedInt = (textInt and 0x00FFFFFF) or 0xAA000000.toInt()

                fun drawSlot(slot: WatchFaceComplication, x: Float, y: Float, colorInt: Int) {
                    val text = formatPreviewComplication(slot, zoned, nextEventLabel) ?: return
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = colorInt
                        typeface = baseTypeface
                        textSize = 18f * scaleFactor
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    nativeCanvas.drawText(text.take(22), x, y, paint)
                }

                drawSlot(project.topComplication, cx, cy - 118f * scaleFactor, accentInt)
                drawSlot(project.bottomComplication, cx, cy + 138f * scaleFactor, mutedInt)
                drawSlot(project.leftComplication, cx - 122f * scaleFactor, cy + 6f * scaleFactor, mutedInt)
                drawSlot(project.rightComplication, cx + 122f * scaleFactor, cy + 6f * scaleFactor, mutedInt)

                val hourPattern = if (project.is24Hour) "HH" else "hh"
                val hourStr = zoned.format(DateTimeFormatter.ofPattern(hourPattern, Locale.US))
                val minStr = zoned.format(DateTimeFormatter.ofPattern("mm", Locale.US))
                val secStr = zoned.format(DateTimeFormatter.ofPattern("ss", Locale.US))
                val userScale = project.timeScale.coerceIn(0.8f, 1.25f)

                val timePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = textInt
                    typeface = android.graphics.Typeface.create(baseTypeface, android.graphics.Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                when (project.dialStyle) {
                    WatchFaceDialStyle.DIGITAL_CENTER -> {
                        timePaint.textSize = 94f * scaleFactor * userScale
                        nativeCanvas.drawText("$hourStr:$minStr", cx, cy + 30f * scaleFactor * userScale, timePaint)
                        if (project.showSeconds) {
                            val secPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = accentInt
                                typeface = android.graphics.Typeface.MONOSPACE
                                textSize = 26f * scaleFactor * userScale
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            nativeCanvas.drawText(":$secStr", cx, cy + 68f * scaleFactor * userScale, secPaint)
                        }
                    }

                    WatchFaceDialStyle.DIGITAL_STACKED -> {
                        timePaint.textSize = 86f * scaleFactor * userScale
                        nativeCanvas.drawText(hourStr, cx, cy - 8f * scaleFactor, timePaint)
                        timePaint.color = accentInt
                        nativeCanvas.drawText(minStr, cx, cy + 78f * scaleFactor * userScale, timePaint)
                    }

                    WatchFaceDialStyle.HYBRID_DUAL -> {
                        timePaint.textSize = 42f * scaleFactor * userScale
                        nativeCanvas.drawText("$hourStr:$minStr", cx, cy + 84f * scaleFactor, timePaint)
                    }

                    WatchFaceDialStyle.ANALOG_CLASSIC -> Unit
                }
            }
        }
    }
}

private fun formatPreviewComplication(
    slot: WatchFaceComplication,
    zoned: ZonedDateTime,
    nextEventLabel: String?,
): String? = when (slot) {
    WatchFaceComplication.NONE -> null
    WatchFaceComplication.DATE -> zoned.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)).uppercase()
    WatchFaceComplication.NEXT_EVENT -> nextEventLabel?.take(20) ?: "NO EVENT"
    WatchFaceComplication.TIMEZONE -> zoned.zone.id.substringAfterLast('/').take(10).uppercase()
    WatchFaceComplication.BATTERY_SLOT -> "BAT --%"
    WatchFaceComplication.HEART_RATE_SLOT -> "HR --"
}
