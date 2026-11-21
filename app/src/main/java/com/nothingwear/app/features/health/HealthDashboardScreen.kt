package com.nothingwear.app.features.health

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingStatusBadge
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingRadius
import com.nothingwear.app.core.designsystem.theme.NothingSpacing
import com.nothingwear.app.domain.model.BleHeartRateSample
import com.nothingwear.app.domain.model.HealthConnectAvailability
import com.nothingwear.app.domain.model.HealthMetricSummary
import com.nothingwear.app.domain.model.MetricAvailabilityStatus
import com.nothingwear.app.domain.model.UnifiedHeartRateSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HealthDashboardRoute(
    onBack: () -> Unit,
    onOpenDevices: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = viewModel::onHealthPermissionsResult,
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onRefreshAll()
    }

    HealthDashboardScreen(
        uiState = uiState,
        onBack = onBack,
        onRefreshAll = viewModel::onRefreshAll,
        onRequestHealthPermissions = {
            healthPermissionLauncher.launch(viewModel.requiredHealthPermissions)
        },
        onOpenHealthConnectSettings = {
            runCatching {
                context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
            }
        },
        onConnectWatchForHr = viewModel::onConnectWatchForHr,
        onToggleLiveBleHeartRate = viewModel::onToggleLiveBleHeartRate,
        onOpenDevices = onOpenDevices,
        modifier = modifier,
    )
}

@Composable
fun HealthDashboardScreen(
    uiState: HealthDashboardUiState,
    onBack: () -> Unit,
    onRefreshAll: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onOpenHealthConnectSettings: () -> Unit,
    onConnectWatchForHr: () -> Unit,
    onToggleLiveBleHeartRate: () -> Unit,
    onOpenDevices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val health = uiState.healthSnapshot
    val unifiedHr = uiState.unifiedHeartRate
    val session = uiState.gattSession
    val hrState = uiState.bleHeartRate

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(NothingSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                    Column {
                        Text(
                            text = "HEALTH & DEVICE",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "M6 VERIFIED TELEMETRY · ZERO FABRICATED DATA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onRefreshAll, enabled = !uiState.isRefreshing) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh verified telemetry",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (!uiState.feedbackMessage.isNullOrBlank()) {
            item {
                NothingSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    borderColor = MaterialTheme.colorScheme.primary,
                    contentPadding = PaddingValues(NothingSpacing.sm),
                ) {
                    Text(
                        text = uiState.feedbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // 1. Wearable Device Info & Verified Capabilities
        item {
            NothingSectionHeader(
                title = "Wearable Telemetry",
                trailingContent = {
                    NothingStatusBadge(
                        label = session.connectionState.name,
                        dotColor = if (session.services.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.deviceName ?: uiState.detectedWatch?.displayName ?: "Xiaomi Watch S4",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                val mac = session.redactedAddress ?: uiState.detectedWatch?.redactedAddress
                                if (mac != null) append("$mac · ")
                                val transport = uiState.detectedWatch?.transportType?.label
                                if (transport != null) append("$transport · ")
                                append("GATT services: ${session.services.size}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(NothingSpacing.sm))

                val stdInfo = session.standardDeviceInfo
                TelemetryKeyValueRow(
                    label = "Watch Battery (0x2A19)",
                    value = stdInfo.batteryPercent?.let { "$it%" } ?: "UNAVAILABLE",
                    isVerified = stdInfo.batteryPercent != null,
                    note = if (stdInfo.batteryPercent != null) {
                        "Verified standard BLE Battery Service."
                    } else {
                        "Not exposed by Xiaomi Watch S4 standard GATT."
                    },
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                TelemetryKeyValueRow(
                    label = "Device Info (0x180A)",
                    value = listOfNotNull(stdInfo.manufacturerName, stdInfo.modelNumber, stdInfo.firmwareRevision)
                        .joinToString(" · ")
                        .ifBlank { "UNAVAILABLE" },
                    isVerified = !stdInfo.manufacturerName.isNullOrBlank() || !stdInfo.firmwareRevision.isNullOrBlank(),
                    note = "Standard DIS strings (read-only when exposed).",
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                TelemetryKeyValueRow(
                    label = "BLE Heart Rate (0x180D)",
                    value = if (hrState.isServiceExposed) "EXPOSED (0x2A37)" else "NOT INSPECTED",
                    isVerified = hrState.isServiceExposed,
                    note = "Standard Bluetooth SIG Heart Rate Measurement characteristic.",
                )

                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                ) {
                    if (session.services.isEmpty() && uiState.detectedWatch != null) {
                        Button(
                            onClick = onConnectWatchForHr,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BluetoothConnected,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Inspect Watch S4")
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenDevices,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(NothingRadius.sm),
                    ) {
                        Text("BLE Diagnostics")
                    }
                }
            }
        }

        // 2. Unified Heart Rate Monitor (Auto-Switch: Live BLE 0x2A37 -> Health Connect Auto -> Last Verified)
        item {
            NothingSectionHeader(
                title = "Heart Rate (Live & Auto-Sync)",
                trailingContent = {
                    NothingStatusBadge(
                        label = unifiedHr.source.badgeLabel,
                        dotColor = if (unifiedHr.source == UnifiedHeartRateSource.LIVE_BLE ||
                            unifiedHr.source == UnifiedHeartRateSource.HEALTH_CONNECT
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MonitorHeart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Column {
                            Text(
                                text = "${unifiedHr.primaryBpmText} BPM",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = buildString {
                                    append(unifiedHr.sourceCaption)
                                    unifiedHr.timestampMillis?.let { ts ->
                                        append(" · ")
                                        append(formatShortTime(ts))
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (hrState.recentSamples.size >= 2) {
                        HeartRateMiniBars(
                            samples = hrState.recentSamples,
                            modifier = Modifier
                                .size(width = 92.dp, height = 38.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Text(
                    text = unifiedHr.detailNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (hrState.isListening && !hrState.statusNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NothingSpacing.xxs))
                    Text(
                        text = hrState.statusNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Button(
                    onClick = onToggleLiveBleHeartRate,
                    enabled = hrState.isServiceExposed || hrState.isListening,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(NothingRadius.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (hrState.isListening) {
                            "Stop Live BLE Broadcast Stream"
                        } else {
                            "Listen Live BLE Broadcast (0x2A37)"
                        },
                    )
                }
            }
        }

        // 3. Verified Health Connect Metrics (Steps, Heart Rate, Sleep)
        item {
            NothingSectionHeader(
                title = "Health Connect (Read-Only)",
                trailingContent = {
                    NothingStatusBadge(
                        label = health.healthConnectAvailability.label,
                        dotColor = if (health.hasHealthPermissions) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))

            if (health.healthConnectAvailability == HealthConnectAvailability.AVAILABLE && !health.hasHealthPermissions) {
                NothingSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Read Verified Watch S4 Metrics via Health Connect",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.xxs))
                            Text(
                                text = "NothingWear requests read-only access to Steps, Heart Rate, and Sleep from Android Health Connect. Enable Health Connect sync inside Mi Fitness → Profile → Third-party data so Watch S4 metrics flow cleanly here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.sm))
                            Button(
                                onClick = onRequestHealthPermissions,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(NothingRadius.sm),
                            ) {
                                Text("Grant Health Connect Read Access")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
            }

            Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                HealthMetricCard(metric = health.stepsToday)
                HealthMetricCard(metric = health.heartRateSummary)
                HealthMetricCard(metric = health.sleepLast24h)
            }

            if (health.healthConnectAvailability == HealthConnectAvailability.AVAILABLE) {
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                OutlinedButton(
                    onClick = onOpenHealthConnectSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(NothingRadius.sm),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Open Android Health Connect Settings")
                }
            }

            if (health.dataOrigins.isNotEmpty()) {
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Text(
                    text = "Verified Health Connect origins: ${health.dataOrigins.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!health.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Text(
                    text = "Health Connect note: ${health.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // 4. Host Phone Companion Telemetry (Clearly Separated from Watch)
        item {
            NothingSectionHeader(title = "Host Phone Environment")
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                val host = health.hostPhone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = host.deviceModel,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${host.androidRelease} (API ${host.sdkInt}) · BT ${uiState.adapterStatus.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BatteryChargingFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = host.batteryPercent?.let {
                                "$it%${if (host.isCharging) " ⚡" else ""}"
                            } ?: "--",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Text(
                    text = "Phone battery shown strictly as host companion telemetry — never substituted for Xiaomi Watch S4 battery.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 5. Data Provenance & Honesty Boundary
        item {
            NothingSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "DATA PROVENANCE & HONESTY POLICY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Text(
                    text = "• Live BLE Heart Rate uses only the official Bluetooth SIG Heart Rate Service (0x180D / 0x2A37) — zero proprietary 0xFDAB writes.\n" +
                        "• Steps, Heart Rate history, and Sleep are read exclusively from Android Health Connect when explicitly authorized.\n" +
                        "• Watch S4 battery & firmware are reported as UNAVAILABLE unless exposed via standard GATT 0x180F / 0x180A.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TelemetryKeyValueRow(
    label: String,
    value: String,
    isVerified: Boolean,
    note: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = if (isVerified) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HealthMetricCard(metric: HealthMetricSummary) {
    val isVerified = metric.status == MetricAvailabilityStatus.VERIFIED
    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = metric.title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NothingStatusBadge(
                label = metric.status.label,
                dotColor = if (isVerified) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = metric.primaryValue,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (metric.unit.isNotBlank()) {
                Text(
                    text = metric.unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xxs))
        Text(
            text = metric.detailNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeartRateMiniBars(
    samples: List<BleHeartRateSample>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val visible = samples.takeLast(14)
        if (visible.isEmpty()) return@Canvas
        val minBpm = (visible.minOf { it.bpm } - 8).coerceAtLeast(30)
        val maxBpm = (visible.maxOf { it.bpm } + 8).coerceAtLeast(minBpm + 10)
        val gap = 3f
        val barWidth = ((size.width - gap * (visible.size - 1)) / visible.size).coerceAtLeast(2f)
        visible.forEachIndexed { idx, sample ->
            val ratio = ((sample.bpm - minBpm).toFloat() / (maxBpm - minBpm).toFloat()).coerceIn(0.15f, 1f)
            val barHeight = size.height * ratio
            val x = idx * (barWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

private fun formatShortTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US))
