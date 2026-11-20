package com.nothingwear.app.features.calendar

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingStatusBadge
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingRadius
import com.nothingwear.app.core.designsystem.theme.NothingSpacing
import com.nothingwear.app.domain.model.AgendaRange
import com.nothingwear.app.domain.model.CalendarSource

@Composable
fun CalendarRoute(
    onOpenEventDetail: (String) -> Unit,
    onOpenWatchSync: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onPermissionResult,
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onRefreshPermissionState()
    }

    CalendarScreen(
        uiState = uiState,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        },
        onOpenAppSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
            context.startActivity(intent)
        },
        onSelectRange = viewModel::onSelectRange,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSyncNow = viewModel::onSyncNow,
        onShowFilterSheet = viewModel::onShowCalendarFilterSheet,
        onToggleCalendar = viewModel::onToggleCalendar,
        onOpenEventDetail = onOpenEventDetail,
        onOpenWatchSync = onOpenWatchSync,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSelectRange: (AgendaRange) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSyncNow: () -> Unit,
    onShowFilterSheet: (Boolean) -> Unit,
    onToggleCalendar: (Long, Boolean) -> Unit,
    onOpenEventDetail: (String) -> Unit,
    onOpenWatchSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(NothingSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        item {
            CalendarHeader(
                isSyncing = uiState.isSyncing,
                hasPermission = uiState.hasPermission,
                selectedCalendars = uiState.syncMetadata.selectedCalendarCount,
                totalCalendars = uiState.syncMetadata.totalCalendarCount,
                onOpenFilter = { onShowFilterSheet(true) },
                onSyncNow = if (uiState.hasPermission) onSyncNow else onRequestPermission,
            )
        }

        if (!uiState.hasPermission) {
            item {
                CalendarPermissionCard(
                    permissionDeniedOnce = uiState.permissionDeniedOnce,
                    hasCachedEvents = uiState.syncMetadata.cachedEventCount > 0,
                    onRequestPermission = onRequestPermission,
                    onOpenAppSettings = onOpenAppSettings,
                )
            }
        }

        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Search title, location, or calendar...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(NothingRadius.md),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                AgendaRange.entries.forEach { range ->
                    RangeChip(
                        label = range.label.uppercase(),
                        selected = uiState.selectedRange == range,
                        onClick = { onSelectRange(range) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            SyncStatusBanner(
                lastSyncedLabel = uiState.lastSyncedLabel,
                feedbackMessage = uiState.syncFeedbackMessage,
                filteredCount = uiState.totalFilteredEvents,
                onOpenWatchSync = onOpenWatchSync,
            )
        }

        if (uiState.groupedEvents.isEmpty()) {
            item {
                EmptyAgendaCard(
                    hasPermission = uiState.hasPermission,
                    hasCalendars = uiState.calendars.isNotEmpty(),
                    selectedCalendarCount = uiState.syncMetadata.selectedCalendarCount,
                    searchQuery = uiState.searchQuery,
                    selectedRange = uiState.selectedRange,
                    onSyncNow = if (uiState.hasPermission) onSyncNow else onRequestPermission,
                    onOpenFilter = { onShowFilterSheet(true) },
                )
            }
        } else {
            uiState.groupedEvents.forEach { group ->
                item(key = "header_${group.dateHeader}") {
                    NothingSectionHeader(
                        title = group.dateHeader,
                        trailingContent = {
                            Text(
                                text = "${group.events.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                items(
                    items = group.events,
                    key = { it.instanceKey },
                ) { event ->
                    AgendaEventCard(
                        event = event,
                        onClick = { onOpenEventDetail(event.instanceKey) },
                    )
                }
            }
        }
    }

    if (uiState.showCalendarFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { onShowFilterSheet(false) },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CalendarSelectionSheetContent(
                calendars = uiState.calendars,
                onToggleCalendar = onToggleCalendar,
                onDismiss = { onShowFilterSheet(false) },
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    isSyncing: Boolean,
    hasPermission: Boolean,
    selectedCalendars: Int,
    totalCalendars: Int,
    onOpenFilter: () -> Unit,
    onSyncNow: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CALENDAR",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (totalCalendars > 0) {
                    "$selectedCalendars OF $totalCalendars CALENDARS SELECTED · OFFLINE CACHE"
                } else {
                    "ANDROID CALENDAR PROVIDER · LOCAL CACHE"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onOpenFilter,
                enabled = totalCalendars > 0,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Select calendars",
                    tint = if (totalCalendars > 0) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(
                onClick = onSyncNow,
                enabled = !isSyncing,
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = if (hasPermission) "Sync calendars" else "Grant calendar permission",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarPermissionCard(
    permissionDeniedOnce: Boolean,
    hasCachedEvents: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Outlined.LockOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "READ_CALENDAR PERMISSION REQUIRED",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = "NothingWear reads calendars synced to Android (including Google Calendar) in strict read-only mode and caches selected events offline using Room. It never modifies your calendars and does not automatically push events to Watch S4.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasCachedEvents) {
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            Text(
                text = "Previously cached offline events remain visible below.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(NothingRadius.sm),
            ) {
                Text(text = "Grant Access")
            }
            if (permissionDeniedOnce) {
                OutlinedButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(NothingRadius.sm),
                ) {
                    Text(text = "App Settings")
                }
            }
        }
    }
}

@Composable
private fun RangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(NothingRadius.sm),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
    ) {
        Box(
            modifier = Modifier.padding(vertical = NothingSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun SyncStatusBanner(
    lastSyncedLabel: String?,
    feedbackMessage: String?,
    filteredCount: Int,
    onOpenWatchSync: () -> Unit,
) {
    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentPadding = PaddingValues(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
        onClick = onOpenWatchSync,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NothingStatusBadge(
                label = if (lastSyncedLabel != null) "LOCAL CACHE · $filteredCount" else "NOT SYNCED",
                dotColor = if (lastSyncedLabel != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = "WATCH BRIDGE →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = feedbackMessage
                ?: (lastSyncedLabel?.let { "Synced $it · " } ?: "") +
                    "Tap to configure Watch Reminder Bridge (fallback for WearAuthV2 native Events limit).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AgendaEventCard(
    event: CalendarEventUiModel,
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
                        .background(Color(event.calendarColor)),
                )
                Text(
                    text = event.timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = event.calendarName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(NothingSpacing.xs))

        Text(
            text = event.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (!event.location.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(NothingSpacing.xxs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xxs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyAgendaCard(
    hasPermission: Boolean,
    hasCalendars: Boolean,
    selectedCalendarCount: Int,
    searchQuery: String,
    selectedRange: AgendaRange,
    onSyncNow: () -> Unit,
    onOpenFilter: () -> Unit,
) {
    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                val title = when {
                    searchQuery.isNotBlank() -> "No events match \"$searchQuery\""
                    hasCalendars && selectedCalendarCount == 0 -> "No calendars selected"
                    !hasPermission -> "No cached events yet"
                    else -> "No events in ${selectedRange.label.lowercase()} window"
                }
                val description = when {
                    searchQuery.isNotBlank() -> "Try another search keyword or switch to the 30 Days window."
                    hasCalendars && selectedCalendarCount == 0 -> "Enable at least one calendar from the filter menu."
                    !hasPermission -> "Grant READ_CALENDAR permission to fetch Google and local Android calendars."
                    else -> "Tap Sync to refresh from Android Calendar Provider, or check your selected calendars."
                }

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

                Spacer(modifier = Modifier.height(NothingSpacing.xs))

                if (hasCalendars && selectedCalendarCount == 0) {
                    OutlinedButton(
                        onClick = onOpenFilter,
                        shape = RoundedCornerShape(NothingRadius.sm),
                    ) {
                        Text(text = "Select Calendars")
                    }
                } else if (hasPermission && searchQuery.isBlank()) {
                    OutlinedButton(
                        onClick = onSyncNow,
                        shape = RoundedCornerShape(NothingRadius.sm),
                    ) {
                        Text(text = "Sync Now")
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSelectionSheetContent(
    calendars: List<CalendarSource>,
    onToggleCalendar: (Long, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NothingSpacing.md)
            .padding(bottom = NothingSpacing.xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "SELECT CALENDARS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Choose which synced Android calendars appear in NothingWear",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                )
            }
        }

        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = NothingSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            items(calendars, key = { it.id }) { calendar ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = NothingSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(calendar.color)),
                        )
                        Column {
                            Text(
                                text = calendar.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val accountLabel = buildString {
                                if (calendar.isGoogleCalendar) append("Google · ")
                                append(calendar.accountName.ifBlank { calendar.accountType })
                            }
                            Text(
                                text = accountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Switch(
                        checked = calendar.isSelected,
                        onCheckedChange = { checked ->
                            onToggleCalendar(calendar.id, checked)
                        },
                    )
                }
            }
        }
    }
}
