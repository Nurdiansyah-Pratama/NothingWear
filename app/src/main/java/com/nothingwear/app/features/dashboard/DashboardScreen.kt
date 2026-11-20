package com.nothingwear.app.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Troubleshoot
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingStatusBadge
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingSpacing
import com.nothingwear.app.core.designsystem.theme.NothingWearTheme
import com.nothingwear.app.domain.model.AgendaItem
import com.nothingwear.app.domain.model.CalendarOverview
import com.nothingwear.app.domain.model.CalendarSyncState
import com.nothingwear.app.domain.model.DashboardOverview
import com.nothingwear.app.domain.model.DeviceStatusSummary
import com.nothingwear.app.domain.model.WatchConnectionState

@Composable
fun DashboardRoute(
    onOpenCalendar: () -> Unit,
    onOpenWatchSync: () -> Unit,
    onOpenEventDetail: (String) -> Unit,
    onOpenWatchFaceStudio: () -> Unit,
    onOpenDeviceDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        uiState = uiState,
        onOpenCalendar = onOpenCalendar,
        onOpenWatchSync = onOpenWatchSync,
        onOpenEventDetail = onOpenEventDetail,
        onOpenWatchFaceStudio = onOpenWatchFaceStudio,
        onOpenDeviceDiagnostics = onOpenDeviceDiagnostics,
        modifier = modifier,
    )
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onOpenCalendar: () -> Unit,
    onOpenWatchSync: () -> Unit,
    onOpenEventDetail: (String) -> Unit,
    onOpenWatchFaceStudio: () -> Unit,
    onOpenDeviceDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        DashboardUiState.Loading -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(NothingSpacing.md),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "INITIALIZING NOTHINGWEAR...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is DashboardUiState.Ready -> {
            val overview = uiState.overview
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = NothingSpacing.md,
                    vertical = NothingSpacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
            ) {
                item {
                    DashboardHeader(milestoneLabel = overview.currentMilestone)
                }

                item {
                    DeviceStatusCard(
                        deviceStatus = overview.deviceStatus,
                        onOpenDeviceDiagnostics = onOpenDeviceDiagnostics,
                    )
                }

                item {
                    CalendarSyncStatusCard(
                        calendarOverview = overview.calendarOverview,
                        onOpenCalendar = onOpenWatchSync,
                    )
                }

                item {
                    val todayEvents = overview.calendarOverview.todayEvents
                    NothingSectionHeader(
                        title = "Today's Agenda",
                        trailingContent = {
                            NothingStatusBadge(
                                label = "${todayEvents.size} Items",
                                dotColor = if (todayEvents.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    if (todayEvents.isEmpty()) {
                        val (emptyTitle, emptyDescription) = when (overview.calendarOverview.localCalendarState) {
                            CalendarSyncState.PERMISSION_REQUIRED -> {
                                "Calendar permission needed" to
                                    "Open the Calendar tab to grant READ_CALENDAR and load today's events offline."
                            }

                            CalendarSyncState.LOCAL_ONLY -> {
                                "No events scheduled today" to
                                    "Your selected local calendars have no events today. Tap to inspect or refresh."
                            }

                            else -> {
                                "No local agenda loaded" to
                                    "Open the Calendar tab and tap Sync to cache Android & Google calendars offline."
                            }
                        }
                        AgendaEmptyStateCard(
                            icon = Icons.Outlined.CalendarMonth,
                            title = emptyTitle,
                            description = emptyDescription,
                            onClick = onOpenCalendar,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                            todayEvents.take(3).forEach { agendaItem ->
                                DashboardAgendaItemCard(
                                    item = agendaItem,
                                    showDate = false,
                                    onClick = { onOpenEventDetail(agendaItem.id) },
                                )
                            }
                            if (todayEvents.size > 3) {
                                NothingSurfaceCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentPadding = PaddingValues(
                                        horizontal = NothingSpacing.md,
                                        vertical = NothingSpacing.sm,
                                    ),
                                    onClick = onOpenCalendar,
                                ) {
                                    Text(
                                        text = "+${todayEvents.size - 3} more today · Open Calendar",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    val upcomingEvents = overview.calendarOverview.upcomingEvents
                    NothingSectionHeader(
                        title = "Upcoming Events",
                        trailingContent = {
                            NothingStatusBadge(label = "${upcomingEvents.size} Shown")
                        },
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    if (upcomingEvents.isEmpty()) {
                        val upcomingDescription = when (overview.calendarOverview.localCalendarState) {
                            CalendarSyncState.LOCAL_ONLY ->
                                "No upcoming events found in the next 30 days for your selected calendars."

                            else ->
                                "Sync your Android calendars in the Calendar tab to populate upcoming agendas offline."
                        }
                        AgendaEmptyStateCard(
                            icon = Icons.AutoMirrored.Outlined.EventNote,
                            title = "No upcoming events cached",
                            description = upcomingDescription,
                            onClick = onOpenCalendar,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                            upcomingEvents.take(4).forEach { agendaItem ->
                                DashboardAgendaItemCard(
                                    item = agendaItem,
                                    showDate = true,
                                    onClick = { onOpenEventDetail(agendaItem.id) },
                                )
                            }
                        }
                    }
                }

                item {
                    NothingSectionHeader(title = "Quick Shortcuts")
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        ShortcutTile(
                            icon = Icons.Outlined.Palette,
                            badge = "M5",
                            title = "Watch Face Studio",
                            subtitle = "6 themes · 466×466",
                            onClick = onOpenWatchFaceStudio,
                            modifier = Modifier.weight(1f),
                        )
                        ShortcutTile(
                            icon = Icons.Outlined.Troubleshoot,
                            badge = "M2",
                            title = "Device Diagnostics",
                            subtitle = "BLE & GATT inspector",
                            onClick = onOpenDeviceDiagnostics,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(milestoneLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "NOTHINGWEAR",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "INDEPENDENT WEARABLE COMPANION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NothingStatusBadge(
            label = milestoneLabel,
            dotColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun DeviceStatusCard(
    deviceStatus: DeviceStatusSummary,
    onOpenDeviceDiagnostics: () -> Unit,
) {
    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenDeviceDiagnostics,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Watch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "DEVICE STATUS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NothingStatusBadge(
                label = deviceStatus.connectionState.name,
                dotColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(NothingSpacing.sm))

        Text(
            text = deviceStatus.targetDeviceName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(NothingSpacing.xxs))

        Text(
            text = deviceStatus.statusDetail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(NothingSpacing.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(NothingSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.BluetoothDisabled,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "BATTERY: ${deviceStatus.batteryPercent?.let { "$it%" } ?: "UNAVAILABLE"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Open device diagnostics",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun CalendarSyncStatusCard(
    calendarOverview: CalendarOverview,
    onOpenCalendar: () -> Unit,
) {
    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onOpenCalendar,
    ) {
        Text(
            text = "SYNCHRONIZATION STATUS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))

        StatusRow(
            label = "Android Calendar",
            value = calendarOverview.localCalendarState.name.replace('_', ' '),
            helper = calendarOverview.localStatusMessage,
        )

        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(NothingSpacing.sm))

        StatusRow(
            label = "Watch S4 Native Events",
            value = calendarOverview.nativeWatchSyncState.name.replace('_', ' '),
            helper = calendarOverview.nativeWatchSyncMessage,
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    helper: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xxs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = helper,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DashboardAgendaItemCard(
    item: AgendaItem,
    showDate: Boolean,
    onClick: () -> Unit,
) {
    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(item.calendarColor)),
                )
                Text(
                    text = if (showDate) "${item.dateLabel} · ${item.timeLabel}" else item.timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = item.calendarName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xxs))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!item.location.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AgendaEmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xxs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    icon: ImageVector,
    badge: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NothingSurfaceCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(NothingSpacing.xxs))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    NothingWearTheme {
        DashboardScreen(
            uiState = DashboardUiState.Ready(
                DashboardOverview(
                    deviceStatus = DeviceStatusSummary(
                        targetDeviceName = "Xiaomi Watch S4",
                        connectionState = WatchConnectionState.DISCONNECTED,
                        statusDetail = "BLE discovery planned in M2.",
                    ),
                    calendarOverview = CalendarOverview(
                        localCalendarState = CalendarSyncState.NOT_CONFIGURED,
                        localStatusMessage = "Calendar Provider starts in M1.",
                        nativeWatchSyncState = WatchConnectionState.PROTOCOL_UNKNOWN,
                        nativeWatchSyncMessage = "Requires M3 research & M4 verification.",
                        todayEvents = emptyList(),
                        upcomingEvents = emptyList(),
                    ),
                    currentMilestone = "M4 · Sync & Bridge",
                ),
            ),
            onOpenCalendar = {},
            onOpenWatchSync = {},
            onOpenEventDetail = {},
            onOpenWatchFaceStudio = {},
            onOpenDeviceDiagnostics = {},
        )
    }
}
