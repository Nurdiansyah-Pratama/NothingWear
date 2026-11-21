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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nothingwear.app.domain.model.MiFitnessCustomTemplate
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
    miFitnessTemplate: MiFitnessCustomTemplate = MiFitnessCustomTemplate.NONE,
    showSafeZoneGuides: Boolean = true,
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

            if (miFitnessTemplate != MiFitnessCustomTemplate.NONE) {
                drawPreviewMiFitnessBackdrop(
                    template = miFitnessTemplate,
                    cx = cx,
                    cy = cy,
                    scaleFactor = scaleFactor,
                    accentColor = accentColor,
                    textColor = textColor,
                )
                if (showSafeZoneGuides) {
                    drawMiFitnessSafeZoneGuide(
                        template = miFitnessTemplate,
                        cx = cx,
                        cy = cy,
                        radius = radius,
                        scaleFactor = scaleFactor,
                        zoned = zoned,
                    )
                }
                return@Canvas
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

private fun DrawScope.drawPreviewMiFitnessBackdrop(
    template: MiFitnessCustomTemplate,
    cx: Float,
    cy: Float,
    scaleFactor: Float,
    accentColor: Color,
    textColor: Color,
) {
    val subtleStroke = textColor.copy(alpha = 0.22f)
    val midStroke = textColor.copy(alpha = 0.35f)
    val softAccent = accentColor.copy(alpha = 0.23f)

    when (template) {
        MiFitnessCustomTemplate.NONE -> Unit

        MiFitnessCustomTemplate.CUSTOM_ANALOG -> {
            drawCircle(
                color = subtleStroke,
                radius = 166f * scaleFactor,
                center = Offset(cx, cy),
                style = Stroke(width = 1.6f * scaleFactor),
            )
            for (i in 0 until 24) {
                val angle = Math.toRadians((i * 15.0) - 90.0)
                val r = 114f * scaleFactor
                drawCircle(
                    color = if (i % 6 == 0) accentColor.copy(alpha = 0.55f) else subtleStroke,
                    radius = (if (i % 6 == 0) 2.6f else 1.6f) * scaleFactor,
                    center = Offset(
                        cx + (cos(angle) * r).toFloat(),
                        cy + (sin(angle) * r).toFloat(),
                    ),
                )
            }
            for (i in 0 until 4) {
                val angle = Math.toRadians((i * 90.0) - 90.0)
                drawLine(
                    color = if (i == 0) accentColor else midStroke,
                    start = Offset(
                        cx + (cos(angle) * 156f * scaleFactor).toFloat(),
                        cy + (sin(angle) * 156f * scaleFactor).toFloat(),
                    ),
                    end = Offset(
                        cx + (cos(angle) * 176f * scaleFactor).toFloat(),
                        cy + (sin(angle) * 176f * scaleFactor).toFloat(),
                    ),
                    strokeWidth = (if (i == 0) 4.5f else 3f) * scaleFactor,
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(
                color = midStroke,
                radius = 14f * scaleFactor,
                center = Offset(cx, cy),
                style = Stroke(width = 1.6f * scaleFactor),
            )
        }

        MiFitnessCustomTemplate.CUSTOM_ELEGANCE -> {
            val r = 190f * scaleFactor
            drawArc(
                color = subtleStroke,
                startAngle = 114f,
                sweepAngle = 312f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = 1.8f * scaleFactor),
            )
            for (deg in listOf(-24, -12, 0, 12, 24, 156, 168, 180, 192, 204)) {
                val angle = Math.toRadians(deg.toDouble())
                val isMid = deg == 0 || deg == 180
                drawCircle(
                    color = if (isMid) accentColor else midStroke,
                    radius = (if (isMid) 3.4f else 2.0f) * scaleFactor,
                    center = Offset(
                        cx + (cos(angle) * 172f * scaleFactor).toFloat(),
                        cy + (sin(angle) * 172f * scaleFactor).toFloat(),
                    ),
                )
            }
            drawCircle(
                color = accentColor,
                radius = 4.5f * scaleFactor,
                center = Offset(cx, cy - 172f * scaleFactor),
            )
        }

        MiFitnessCustomTemplate.CUSTOM_DIGITAL -> {
            val dividerX = cx + 28f * scaleFactor
            drawLine(
                color = subtleStroke,
                start = Offset(dividerX, cy - 108f * scaleFactor),
                end = Offset(dividerX, cy + 108f * scaleFactor),
                strokeWidth = 1.8f * scaleFactor,
            )
            drawCircle(accentColor, 3.5f * scaleFactor, Offset(dividerX, cy - 114f * scaleFactor))
            drawCircle(accentColor, 3.5f * scaleFactor, Offset(dividerX, cy + 114f * scaleFactor))

            val startX = cx + 52f * scaleFactor
            val startY = cy - 76f * scaleFactor
            val step = 19f * scaleFactor
            val maxR = 168f * scaleFactor
            for (row in 0 until 9) {
                for (col in 0 until 5) {
                    val px = startX + col * step
                    val py = startY + row * step
                    val dx = px - cx
                    val dy = py - cy
                    if (dx * dx + dy * dy <= maxR * maxR) {
                        val isAccentDot = (row == col + 1) || (row == 7 - col && col >= 2)
                        drawCircle(
                            color = if (isAccentDot) accentColor else subtleStroke,
                            radius = (if (isAccentDot) 3.2f else 2.2f) * scaleFactor,
                            center = Offset(px, py),
                        )
                    }
                }
            }
            val arcR = 182f * scaleFactor
            drawArc(
                color = softAccent,
                startAngle = -44f,
                sweepAngle = 88f,
                useCenter = false,
                topLeft = Offset(cx - arcR, cy - arcR),
                size = Size(arcR * 2f, arcR * 2f),
                style = Stroke(width = 3.5f * scaleFactor, cap = StrokeCap.Round),
            )
        }

        MiFitnessCustomTemplate.CUSTOM_POSTCARD -> {
            val glyphCy = cy - 14f * scaleFactor
            val outerR = 72f * scaleFactor
            drawArc(
                color = midStroke,
                startAngle = -145f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(cx - outerR, glyphCy - outerR),
                size = Size(outerR * 2f, outerR * 2f),
                style = Stroke(width = 4f * scaleFactor, cap = StrokeCap.Round),
            )
            drawArc(
                color = midStroke,
                startAngle = 35f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(cx - outerR, glyphCy - outerR),
                size = Size(outerR * 2f, outerR * 2f),
                style = Stroke(width = 4f * scaleFactor, cap = StrokeCap.Round),
            )
            val innerR = 46f * scaleFactor
            drawArc(
                color = accentColor.copy(alpha = 0.8f),
                startAngle = -60f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(cx - innerR, glyphCy - innerR),
                size = Size(innerR * 2f, innerR * 2f),
                style = Stroke(width = 3f * scaleFactor, cap = StrokeCap.Round),
            )
            for (i in -2..2) {
                drawCircle(
                    color = if (i == 0) accentColor else midStroke,
                    radius = (if (i == 0) 3.8f else 2.2f) * scaleFactor,
                    center = Offset(cx + i * 11f * scaleFactor, glyphCy),
                )
                if (i != 0) {
                    drawCircle(
                        color = midStroke,
                        radius = 2.2f * scaleFactor,
                        center = Offset(cx, glyphCy + i * 11f * scaleFactor),
                    )
                }
            }
            for (i in -1..1) {
                drawCircle(
                    color = if (i == 0) accentColor else subtleStroke,
                    radius = 3.2f * scaleFactor,
                    center = Offset(cx + i * 16f * scaleFactor, cy - 122f * scaleFactor),
                )
            }
            drawLine(
                color = subtleStroke,
                start = Offset(cx - 38f * scaleFactor, cy + 114f * scaleFactor),
                end = Offset(cx + 38f * scaleFactor, cy + 114f * scaleFactor),
                strokeWidth = 1.6f * scaleFactor,
            )
        }

        MiFitnessCustomTemplate.CUSTOM_DIAMOND -> {
            drawCircle(accentColor.copy(alpha = 0.09f), 138f * scaleFactor, Offset(cx, cy))
            drawCircle(accentColor.copy(alpha = 0.11f), 94f * scaleFactor, Offset(cx, cy))
            drawCircle(accentColor.copy(alpha = 0.08f), 52f * scaleFactor, Offset(cx, cy))
            drawCircle(
                color = subtleStroke,
                radius = 152f * scaleFactor,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f * scaleFactor),
            )
            for (deg in listOf(45, 135, 225, 315)) {
                val angle = Math.toRadians(deg.toDouble())
                drawLine(
                    color = accentColor.copy(alpha = 0.8f),
                    start = Offset(
                        cx + (cos(angle) * 164f * scaleFactor).toFloat(),
                        cy + (sin(angle) * 164f * scaleFactor).toFloat(),
                    ),
                    end = Offset(
                        cx + (cos(angle) * 184f * scaleFactor).toFloat(),
                        cy + (sin(angle) * 184f * scaleFactor).toFloat(),
                    ),
                    strokeWidth = 3f * scaleFactor,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * Preview-only translucent guide showing where Mi Fitness renders its live hands or clock
 * on top of the exported background-only image. Never rendered into exported PNGs.
 */
private fun DrawScope.drawMiFitnessSafeZoneGuide(
    template: MiFitnessCustomTemplate,
    cx: Float,
    cy: Float,
    radius: Float,
    scaleFactor: Float,
    zoned: ZonedDateTime,
) {
    val guideBoxColor = Color(0x6606B6D4)
    val dashedStroke = Stroke(
        width = 1.4f * scaleFactor,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * scaleFactor, 5f * scaleFactor)),
    )
    val ghostWhite = Color(0x99FFFFFF)
    val ghostTextInt = 0xAAFFFFFF.toInt()
    val hourStr = zoned.format(DateTimeFormatter.ofPattern("HH", Locale.US))
    val minStr = zoned.format(DateTimeFormatter.ofPattern("mm", Locale.US))
    val dateStr = zoned.format(DateTimeFormatter.ofPattern("M/d EEE", Locale.US)).uppercase()
    val shortDate = zoned.format(DateTimeFormatter.ofPattern("MM/dd", Locale.US))
    val dayStr = zoned.format(DateTimeFormatter.ofPattern("EEE", Locale.US)).uppercase()

    when (template) {
        MiFitnessCustomTemplate.NONE -> Unit

        MiFitnessCustomTemplate.CUSTOM_ANALOG -> {
            val handRadius = radius - 34f * scaleFactor
            val hourAngle = Math.toRadians((((zoned.hour % 12) + zoned.minute / 60.0) * 30.0) - 90.0)
            drawLine(
                color = ghostWhite,
                start = Offset(cx, cy),
                end = Offset(
                    cx + (cos(hourAngle) * handRadius * 0.54f).toFloat(),
                    cy + (sin(hourAngle) * handRadius * 0.54f).toFloat(),
                ),
                strokeWidth = 8f * scaleFactor,
                cap = StrokeCap.Round,
            )
            val minAngle = Math.toRadians(((zoned.minute + zoned.second / 60.0) * 6.0) - 90.0)
            drawLine(
                color = ghostWhite,
                start = Offset(cx, cy),
                end = Offset(
                    cx + (cos(minAngle) * handRadius * 0.80f).toFloat(),
                    cy + (sin(minAngle) * handRadius * 0.80f).toFloat(),
                ),
                strokeWidth = 5f * scaleFactor,
                cap = StrokeCap.Round,
            )
            val secAngle = Math.toRadians((zoned.second * 6.0) - 90.0)
            drawLine(
                color = Color(0x88E5E7EB),
                start = Offset(cx, cy),
                end = Offset(
                    cx + (cos(secAngle) * handRadius * 0.88f).toFloat(),
                    cy + (sin(secAngle) * handRadius * 0.88f).toFloat(),
                ),
                strokeWidth = 2f * scaleFactor,
                cap = StrokeCap.Round,
            )
            drawCircle(ghostWhite, 6f * scaleFactor, Offset(cx, cy))
        }

        MiFitnessCustomTemplate.CUSTOM_ELEGANCE -> {
            drawRoundRect(
                color = guideBoxColor,
                topLeft = Offset(cx - 108f * scaleFactor, cy - 72f * scaleFactor),
                size = Size(216f * scaleFactor, 146f * scaleFactor),
                cornerRadius = CornerRadius(12f * scaleFactor),
                style = dashedStroke,
            )
            drawRoundRect(
                color = guideBoxColor,
                topLeft = Offset(cx - 26f * scaleFactor, cy + 168f * scaleFactor),
                size = Size(52f * scaleFactor, 24f * scaleFactor),
                cornerRadius = CornerRadius(8f * scaleFactor),
                style = dashedStroke,
            )
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = ghostTextInt
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                    textSize = 68f * scaleFactor
                }
                nc.drawText("$hourStr:$minStr", cx, cy + 16f * scaleFactor, p)
                p.textSize = 20f * scaleFactor
                p.typeface = android.graphics.Typeface.SANS_SERIF
                nc.drawText(dateStr, cx, cy + 52f * scaleFactor, p)
                p.textSize = 14f * scaleFactor
                nc.drawText("BAT", cx, cy + 185f * scaleFactor, p)
            }
        }

        MiFitnessCustomTemplate.CUSTOM_DIGITAL -> {
            drawRoundRect(
                color = guideBoxColor,
                topLeft = Offset(cx - 156f * scaleFactor, cy - 102f * scaleFactor),
                size = Size(164f * scaleFactor, 198f * scaleFactor),
                cornerRadius = CornerRadius(12f * scaleFactor),
                style = dashedStroke,
            )
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = ghostTextInt
                    textAlign = android.graphics.Paint.Align.LEFT
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    textSize = 46f * scaleFactor
                }
                val lx = cx - 144f * scaleFactor
                nc.drawText("$hourStr:$minStr", lx, cy - 42f * scaleFactor, p)
                nc.drawText(shortDate, lx, cy + 14f * scaleFactor, p)
                nc.drawText("80%", lx, cy + 70f * scaleFactor, p)
            }
        }

        MiFitnessCustomTemplate.CUSTOM_POSTCARD -> {
            // 9 o'clock time box
            drawRoundRect(
                color = guideBoxColor,
                topLeft = Offset(cx - 196f * scaleFactor, cy - 20f * scaleFactor),
                size = Size(84f * scaleFactor, 36f * scaleFactor),
                cornerRadius = CornerRadius(6f * scaleFactor),
                style = dashedStroke,
            )
            // 3 o'clock date box
            drawRoundRect(
                color = guideBoxColor,
                topLeft = Offset(cx + 112f * scaleFactor, cy - 20f * scaleFactor),
                size = Size(84f * scaleFactor, 36f * scaleFactor),
                cornerRadius = CornerRadius(6f * scaleFactor),
                style = dashedStroke,
            )
            // 6 o'clock vertical day box
            drawRoundRect(
                color = guideBoxColor,
                topLeft = Offset(cx - 20f * scaleFactor, cy + 124f * scaleFactor),
                size = Size(40f * scaleFactor, 72f * scaleFactor),
                cornerRadius = CornerRadius(6f * scaleFactor),
                style = dashedStroke,
            )
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = ghostTextInt
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    textSize = 21f * scaleFactor
                }
                nc.drawText("$hourStr:$minStr", cx - 154f * scaleFactor, cy + 6f * scaleFactor, p)
                nc.drawText(shortDate, cx + 154f * scaleFactor, cy + 6f * scaleFactor, p)
                p.textSize = 16f * scaleFactor
                dayStr.take(3).forEachIndexed { idx, ch ->
                    nc.drawText(ch.toString(), cx, cy + (144f + idx * 19f) * scaleFactor, p)
                }
            }
        }

        MiFitnessCustomTemplate.CUSTOM_DIAMOND -> {
            drawCircle(
                color = guideBoxColor,
                radius = 128f * scaleFactor,
                center = Offset(cx, cy),
                style = dashedStroke,
            )
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = ghostTextInt
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                    textSize = 90f * scaleFactor
                }
                nc.drawText(hourStr[0].toString(), cx - 32f * scaleFactor, cy - 22f * scaleFactor, p)
                nc.drawText(hourStr[1].toString(), cx + 38f * scaleFactor, cy + 2f * scaleFactor, p)
                nc.drawText(minStr[0].toString(), cx - 34f * scaleFactor, cy + 68f * scaleFactor, p)
                nc.drawText(minStr[1].toString(), cx + 24f * scaleFactor, cy + 96f * scaleFactor, p)
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
