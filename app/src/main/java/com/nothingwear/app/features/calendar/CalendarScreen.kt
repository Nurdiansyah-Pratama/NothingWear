package com.nothingwear.app.features.calendar

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarRoute(
    onOpenEventDetail: (String) -> Unit,
    onOpenWatchSync: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val readPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onPermissionResult,
    )

    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onWritePermissionResult,
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onRefreshPermissionState()
    }

    CalendarScreen(
        uiState = uiState,
        onRequestPermission = {
            readPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        },
        onOpenAppSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
            context.startActivity(intent)
        },
        onSelectViewMode = viewModel::onSelectViewMode,
        onSelectRange = viewModel::onSelectRange,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onJumpToToday = viewModel::onJumpToToday,
        onSelectDate = viewModel::onSelectDate,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSyncNow = viewModel::onSyncNow,
        onShowFilterSheet = viewModel::onShowCalendarFilterSheet,
        onToggleCalendar = viewModel::onToggleCalendar,
        onOpenCreateEventSheet = { date -> viewModel.onOpenCreateEventSheet(date) },
        onDismissCreateEventSheet = viewModel::onDismissCreateEventSheet,
        onDraftTitleChanged = viewModel::onDraftTitleChanged,
        onDraftCalendarSelected = viewModel::onDraftCalendarSelected,
        onDraftShiftDate = viewModel::onDraftShiftDate,
        onDraftToggleAllDay = viewModel::onDraftToggleAllDay,
        onDraftAdjustStartMinutes = viewModel::onDraftAdjustStartMinutes,
        onDraftAdjustDurationMinutes = viewModel::onDraftAdjustDurationMinutes,
        onDraftLocationChanged = viewModel::onDraftLocationChanged,
        onDraftDescriptionChanged = viewModel::onDraftDescriptionChanged,
        onSaveCreatedEvent = {
            if (!uiState.hasWritePermission) {
                viewModel.onPrepareSaveCreatedEventNeedsWritePermission()
                writePermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
            } else {
                viewModel.onSaveCreatedEvent()
            }
        },
        onRequestDeleteEvent = viewModel::onRequestDeleteEvent,
        onDismissDeleteEvent = viewModel::onDismissDeleteEvent,
        onConfirmDeleteEvent = {
            val pending = uiState.pendingDeleteEvent
            if (pending != null && !uiState.hasWritePermission) {
                viewModel.onPrepareDeleteNeedsWritePermission(pending)
                writePermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
            } else {
                viewModel.onConfirmDeleteEvent()
            }
        },
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
    onSelectViewMode: (CalendarViewMode) -> Unit,
    onSelectRange: (AgendaRange) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSyncNow: () -> Unit,
    onShowFilterSheet: (Boolean) -> Unit,
    onToggleCalendar: (Long, Boolean) -> Unit,
    onOpenCreateEventSheet: (LocalDate?) -> Unit,
    onDismissCreateEventSheet: () -> Unit,
    onDraftTitleChanged: (String) -> Unit,
    onDraftCalendarSelected: (Long) -> Unit,
    onDraftShiftDate: (Long) -> Unit,
    onDraftToggleAllDay: (Boolean) -> Unit,
    onDraftAdjustStartMinutes: (Int) -> Unit,
    onDraftAdjustDurationMinutes: (Int) -> Unit,
    onDraftLocationChanged: (String) -> Unit,
    onDraftDescriptionChanged: (String) -> Unit,
    onSaveCreatedEvent: () -> Unit,
    onRequestDeleteEvent: (CalendarEventUiModel) -> Unit,
    onDismissDeleteEvent: () -> Unit,
    onConfirmDeleteEvent: () -> Unit,
    onOpenEventDetail: (String) -> Unit,
    onOpenWatchSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = NothingSpacing.md,
                end = NothingSpacing.md,
                top = NothingSpacing.md,
                bottom = 88.dp,
            ),
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
                CalendarViewModeBar(
                    selectedMode = uiState.viewMode,
                    onSelectMode = onSelectViewMode,
                )
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
                SyncStatusBanner(
                    lastSyncedLabel = uiState.lastSyncedLabel,
                    feedbackMessage = uiState.syncFeedbackMessage,
                    filteredCount = uiState.totalFilteredEvents,
                    onOpenWatchSync = onOpenWatchSync,
                )
            }

            if (uiState.viewMode == CalendarViewMode.MONTH_GRID) {
                item {
                    GoogleStyleMonthGridCard(
                        monthLabel = uiState.displayedMonthLabel,
                        cells = uiState.monthCells,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onJumpToToday = onJumpToToday,
                        onSelectDate = onSelectDate,
                    )
                }

                item {
                    NothingSectionHeader(
                        title = uiState.selectedDateHeader.ifBlank { "SELECTED DAY" },
                        trailingContent = {
                            TextButton(
                                onClick = { onOpenCreateEventSheet(uiState.selectedDate) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Event", style = MaterialTheme.typography.labelMedium)
                            }
                        },
                    )
                }

                if (uiState.selectedDateEvents.isEmpty()) {
                    item {
                        NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No events on this date",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.xxs))
                            Text(
                                text = "Tap + Add Event to create a new event synced directly with Android / Google Calendar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.selectedDateEvents,
                        key = { "month_${it.instanceKey}" },
                    ) { event ->
                        AgendaEventCard(
                            event = event,
                            onClick = { onOpenEventDetail(event.instanceKey) },
                            onDeleteClick = { onRequestDeleteEvent(event) },
                        )
                    }
                }
            } else {
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
                                onDeleteClick = { onRequestDeleteEvent(event) },
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onOpenCreateEventSheet(uiState.selectedDate) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(NothingSpacing.md),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add calendar event",
            )
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

    if (uiState.showCreateEventSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissCreateEventSheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CreateEventSheetContent(
                draft = uiState.createDraft,
                calendars = uiState.calendars,
                isSaving = uiState.isMutatingEvent,
                onTitleChanged = onDraftTitleChanged,
                onCalendarSelected = onDraftCalendarSelected,
                onShiftDate = onDraftShiftDate,
                onToggleAllDay = onDraftToggleAllDay,
                onAdjustStartMinutes = onDraftAdjustStartMinutes,
                onAdjustDurationMinutes = onDraftAdjustDurationMinutes,
                onLocationChanged = onDraftLocationChanged,
                onDescriptionChanged = onDraftDescriptionChanged,
                onSave = onSaveCreatedEvent,
                onDismiss = onDismissCreateEventSheet,
            )
        }
    }

    uiState.pendingDeleteEvent?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissDeleteEvent,
            title = { Text("Delete Event?") },
            text = {
                Text(
                    "Remove \"${target.title}\" from your Android / Google Calendar? This change syncs back to your calendar provider.",
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteEvent) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteEvent) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun CalendarViewModeBar(
    selectedMode: CalendarViewMode,
    onSelectMode: (CalendarViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
    ) {
        CalendarViewMode.entries.forEach { mode ->
            val selected = mode == selectedMode
            Surface(
                onClick = { onSelectMode(mode) },
                modifier = Modifier.weight(1f),
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
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (mode == CalendarViewMode.MONTH_GRID) {
                            Icons.Outlined.CalendarViewMonth
                        } else {
                            Icons.Outlined.ViewAgenda
                        },
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (mode == CalendarViewMode.MONTH_GRID) "MONTH GRID" else "AGENDA LIST",
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
    }
}

@Composable
private fun GoogleStyleMonthGridCard(
    monthLabel: String,
    cells: List<CalendarMonthCell>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    val weekdays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(NothingSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                    )
                }
                Text(
                    text = monthLabel.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Next month",
                    )
                }
            }
            OutlinedButton(
                onClick = onJumpToToday,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(NothingRadius.sm),
            ) {
                Text("Today", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(NothingSpacing.xs))

        // Weekday row
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dayName == "SUN") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 6x7 bordered Google Calendar-style grid cells
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NothingRadius.sm))
                .border(0.75.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(NothingRadius.sm)),
        ) {
            cells.chunked(7).forEach { weekRow ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekRow.forEach { cell ->
                        MonthGridDayCell(
                            cell = cell,
                            onClick = { onSelectDate(cell.date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGridDayCell(
    cell: CalendarMonthCell,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (cell.isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val cellBg = when {
        cell.isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        !cell.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .height(72.dp)
            .background(cellBg)
            .border(if (cell.isSelected) 1.25.dp else 0.5.dp, borderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (cell.isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                fontSize = 10.sp,
                fontWeight = if (cell.isToday || cell.isSelected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    cell.isToday -> MaterialTheme.colorScheme.onPrimary
                    !cell.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Render up to 2 mini Google Calendar-style colored event pills inside each date box
        cell.events.take(2).forEach { event ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(event.calendarColor).copy(alpha = 0.88f))
                    .padding(horizontal = 2.dp, vertical = 1.dp),
            ) {
                Text(
                    text = event.title,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (cell.events.size > 2) {
            Text(
                text = "+${cell.events.size - 2}",
                fontSize = 8.sp,
                lineHeight = 9.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateEventSheetContent(
    draft: CreateEventDraft,
    calendars: List<CalendarSource>,
    isSaving: Boolean,
    onTitleChanged: (String) -> Unit,
    onCalendarSelected: (Long) -> Unit,
    onShiftDate: (Long) -> Unit,
    onToggleAllDay: (Boolean) -> Unit,
    onAdjustStartMinutes: (Int) -> Unit,
    onAdjustDurationMinutes: (Int) -> Unit,
    onLocationChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    val startTime = LocalTime.of(draft.startHour.coerceIn(0, 23), draft.startMinute.coerceIn(0, 59))
    val endTime = startTime.plusMinutes(draft.durationMinutes.toLong())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NothingSpacing.md)
            .padding(bottom = NothingSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "NEW CALENDAR EVENT",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Saves directly to Android / Google Calendar & Watch Reminder Bridge",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        OutlinedTextField(
            value = draft.title,
            onValueChange = onTitleChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Event Title *") },
            placeholder = { Text("e.g., Team Sync, Doctor Appointment") },
            shape = RoundedCornerShape(NothingRadius.sm),
        )

        if (calendars.isNotEmpty()) {
            Text(
                text = "TARGET CALENDAR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                calendars.forEach { cal ->
                    val selected = cal.id == draft.calendarId
                    Surface(
                        onClick = { onCalendarSelected(cal.id) },
                        shape = RoundedCornerShape(NothingRadius.sm),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(cal.color)),
                            )
                            Text(
                                text = cal.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // Date selector stepper
        NothingSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = NothingSpacing.sm, vertical = NothingSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "DATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onShiftDate(-1) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = "Previous day",
                        )
                    }
                    Text(
                        text = draft.date.format(dateFormatter),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    IconButton(onClick = { onShiftDate(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Next day",
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "All-day event",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(checked = draft.isAllDay, onCheckedChange = onToggleAllDay)
            }

            if (!draft.isAllDay) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "TIME: ${startTime.format(timeFormatter)} – ${endTime.format(timeFormatter)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = { onAdjustStartMinutes(-30) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) { Text("-30m", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(
                            onClick = { onAdjustStartMinutes(30) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) { Text("+30m", style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DURATION: ${draft.durationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = { onAdjustDurationMinutes(-30) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) { Text("-30m", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(
                            onClick = { onAdjustDurationMinutes(30) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) { Text("+30m", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }

        OutlinedTextField(
            value = draft.location,
            onValueChange = onLocationChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Location (optional)") },
            shape = RoundedCornerShape(NothingRadius.sm),
        )

        OutlinedTextField(
            value = draft.description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
            label = { Text("Notes / Description (optional)") },
            shape = RoundedCornerShape(NothingRadius.sm),
        )

        Spacer(modifier = Modifier.height(NothingSpacing.xs))

        Button(
            onClick = onSave,
            enabled = !isSaving && draft.title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(NothingRadius.sm),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saving Event...")
            } else {
                Text("Save Event to Calendar")
            }
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
                    "$selectedCalendars OF $totalCalendars CALENDARS SELECTED · SYNCED CACHE"
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
                text = "CALENDAR PERMISSION REQUIRED",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = "Grant READ_CALENDAR to display your Google & Android calendars offline. WRITE_CALENDAR is requested on-demand only when you add or delete an event.",
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
                    "Tap to configure Watch Reminder Bridge (forwards alerts to Xiaomi Watch S4).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AgendaEventCard(
    event: CalendarEventUiModel,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
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
                modifier = Modifier.weight(1f),
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
                Text(
                    text = "· ${event.calendarName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete event",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(NothingSpacing.xxs))

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
                    else -> "Tap Sync to refresh from Android Calendar Provider, or tap + to add an event."
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
