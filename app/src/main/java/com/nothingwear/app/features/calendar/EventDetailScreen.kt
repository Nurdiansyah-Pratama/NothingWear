package com.nothingwear.app.features.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.WatchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingStatusBadge
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingSpacing

@Composable
fun EventDetailRoute(
    onBack: () -> Unit,
    onOpenWatchSync: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onWritePermissionResult,
    )

    LaunchedEffect(uiState) {
        if (uiState is EventDetailUiState.Deleted) {
            onBack()
        }
    }

    EventDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenWatchSync = onOpenWatchSync,
        onRequestDelete = { viewModel.onShowDeleteConfirm(true) },
        onDismissDelete = { viewModel.onShowDeleteConfirm(false) },
        onConfirmDelete = {
            val loaded = uiState as? EventDetailUiState.Loaded
            if (loaded != null && !loaded.hasWritePermission) {
                viewModel.onShowDeleteConfirm(false)
                writePermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
            } else {
                viewModel.onConfirmDelete()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun EventDetailScreen(
    uiState: EventDetailUiState,
    onBack: () -> Unit,
    onOpenWatchSync: () -> Unit = {},
    onRequestDelete: () -> Unit = {},
    onDismissDelete: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val loadedState = uiState as? EventDetailUiState.Loaded

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NothingSpacing.xs, vertical = NothingSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = "EVENT DETAIL",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (loadedState != null) {
                IconButton(
                    onClick = onRequestDelete,
                    enabled = !loadedState.isDeleting,
                ) {
                    if (loadedState.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete event",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when (uiState) {
            EventDetailUiState.Loading, EventDetailUiState.Deleted -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (uiState is EventDetailUiState.Deleted) "EVENT DELETED" else "LOADING EVENT...",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            EventDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(NothingSpacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    NothingSurfaceCard {
                        Text(
                            text = "Event no longer in local cache",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.xs))
                        Text(
                            text = "This event may have been removed or rescheduled during the latest calendar refresh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is EventDetailUiState.Loaded -> {
                val event = uiState.event
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(NothingSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                ) {
                    if (!uiState.feedbackMessage.isNullOrBlank()) {
                        item {
                            NothingSurfaceCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentPadding = PaddingValues(NothingSpacing.sm),
                            ) {
                                Text(
                                    text = uiState.feedbackMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }

                    item {
                        NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(event.calendarColor)),
                                )
                                Text(
                                    text = event.calendarName.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(NothingSpacing.sm))

                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            Spacer(modifier = Modifier.height(NothingSpacing.md))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(NothingSpacing.md))

                            DetailRow(
                                icon = Icons.Outlined.CalendarToday,
                                label = "DATE",
                                value = event.fullDateLabel,
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.sm))
                            DetailRow(
                                icon = Icons.Outlined.AccessTime,
                                label = "TIME",
                                value = event.timeRangeLabel,
                            )
                            if (!event.timezone.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                                DetailRow(
                                    icon = Icons.Outlined.Public,
                                    label = "TIMEZONE",
                                    value = event.timezone,
                                )
                            }
                        }
                    }

                    if (!event.location.isNullOrBlank()) {
                        item {
                            NothingSectionHeader(title = "Location")
                            Spacer(modifier = Modifier.height(NothingSpacing.xs))
                            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                DetailRow(
                                    icon = Icons.Outlined.LocationOn,
                                    label = "VENUE",
                                    value = event.location,
                                )
                            }
                        }
                    }

                    if (!event.description.isNullOrBlank()) {
                        item {
                            NothingSectionHeader(title = "Description")
                            Spacer(modifier = Modifier.height(NothingSpacing.xs))
                            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Notes,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = event.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        NothingSectionHeader(title = "Storage & Watch Alert Status")
                        Spacer(modifier = Modifier.height(NothingSpacing.xs))
                        NothingSurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = onOpenWatchSync,
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
                                        imageVector = if (event.bridgeEnabled) {
                                            Icons.Outlined.NotificationsActive
                                        } else {
                                            Icons.Outlined.WatchOff
                                        },
                                        contentDescription = null,
                                        tint = if (event.bridgeEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "Watch Reminder Bridge",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                NothingStatusBadge(
                                    label = when {
                                        event.isScheduledForWatchAlert -> "ALERT QUEUED"
                                        event.bridgeEnabled -> "BRIDGE ACTIVE"
                                        else -> "BRIDGE OFF"
                                    },
                                    dotColor = if (event.bridgeEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.height(NothingSpacing.xs))
                            Text(
                                text = buildString {
                                    append("Cached offline (${event.lastSyncedLabel}). ")
                                    when {
                                        event.isScheduledForWatchAlert ->
                                            append("Watch alert scheduled (${event.bridgeLeadLabel} before start) via Mi Fitness notifications. ")
                                        event.bridgeEnabled ->
                                            append("Reminder Bridge is active (schedules upcoming alerts within 48h). ")
                                        else ->
                                            append("Tap to enable Watch Reminder Bridge so alerts forward to Xiaomi Watch S4. ")
                                    }
                                    append("Native Watch S4 Events app sync remains UNSUPPORTED due to WearAuthV2.")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (loadedState?.showDeleteConfirm == true) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete Event?") },
            text = {
                Text(
                    "Remove \"${loadedState.event.title}\" from your Android / Google Calendar? This change syncs back to your calendar provider.",
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
