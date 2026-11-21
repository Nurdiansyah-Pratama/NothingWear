package com.nothingwear.app.features.notifications

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nothingwear.app.domain.model.ReminderLeadTime

@Composable
fun EventSyncRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventSyncViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingEnableAfterGrant by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.onNotificationPermissionResult(
                granted = granted,
                enableAfterGrant = pendingEnableAfterGrant,
            )
            pendingEnableAfterGrant = false
        },
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onRefreshPermissionState()
    }

    fun requestNotificationPermission(enableAfter: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingEnableAfterGrant = enableAfter
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    }

    EventSyncScreen(
        uiState = uiState,
        onBack = onBack,
        onRequestNotificationPermission = { requestNotificationPermission(enableAfter = true) },
        onOpenAppNotificationSettings = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            runCatching { context.startActivity(intent) }
                .onFailure {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
        },
        onToggleBridgeEnabled = { desired ->
            if (desired && !uiState.hasNotificationPermission) {
                requestNotificationPermission(enableAfter = true)
            } else {
                viewModel.onToggleBridgeEnabled(desired)
            }
        },
        onSelectLeadTime = viewModel::onSelectLeadTime,
        onRescheduleNow = viewModel::onRescheduleNow,
        onSendTestWatchAlert = {
            if (!uiState.hasNotificationPermission) {
                requestNotificationPermission(enableAfter = false)
            } else {
                viewModel.onSendTestWatchAlert()
            }
        },
        onScheduleDelayedTestWatchAlert = {
            if (!uiState.hasNotificationPermission) {
                requestNotificationPermission(enableAfter = false)
            } else {
                viewModel.onScheduleDelayedTestWatchAlert()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun EventSyncScreen(
    uiState: EventSyncUiState,
    onBack: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
    onToggleBridgeEnabled: (Boolean) -> Unit,
    onSelectLeadTime: (ReminderLeadTime) -> Unit,
    onRescheduleNow: () -> Unit,
    onSendTestWatchAlert: () -> Unit,
    onScheduleDelayedTestWatchAlert: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NothingSpacing.xs, vertical = NothingSpacing.xs),
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
            Column {
                Text(
                    text = "WATCH SYNC & REMINDER BRIDGE",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "M4 PROTOCOL ADAPTER + VERIFIED FALLBACK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(NothingSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
        ) {
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

            item {
                NothingSectionHeader(
                    title = "Native Watch S4 Events Protocol",
                    trailingContent = {
                        NothingStatusBadge(label = uiState.nativeCapability.state.name)
                    },
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                NothingSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Documented M3 Technical Limitation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    Text(
                        text = uiState.nativeCapability.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    Text(
                        text = "Auth: ${uiState.nativeCapability.authenticationMechanism}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Verified Surfaces: GATT 0xFDAB · Classic SPP 0x1101 · Payloads validated: ${uiState.validatedPayloadCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                NothingSectionHeader(
                    title = "Watch Reminder Bridge (Working Fallback)",
                    trailingContent = {
                        NothingStatusBadge(
                            label = if (uiState.isBridgeEnabled) "ACTIVE" else "OFF",
                            dotColor = if (uiState.isBridgeEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                NothingSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (uiState.isBridgeEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                        ) {
                            Icon(
                                imageVector = if (uiState.isBridgeEnabled) {
                                    Icons.Outlined.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsOff
                                },
                                contentDescription = null,
                                tint = if (uiState.isBridgeEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Column {
                                Text(
                                    text = "Enable Calendar Watch Alerts",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Schedules local alerts for upcoming Room-cached events so Mi Fitness NotifySyncService mirrors them to Watch S4.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isBridgeEnabled,
                            onCheckedChange = onToggleBridgeEnabled,
                        )
                    }

                    if (!uiState.hasNotificationPermission) {
                        Spacer(modifier = Modifier.height(NothingSpacing.md))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                        ) {
                            Button(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(NothingRadius.sm),
                            ) {
                                Text(text = "Allow Notifications")
                            }
                            if (uiState.permissionDeniedOnce) {
                                OutlinedButton(
                                    onClick = onOpenAppNotificationSettings,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(NothingRadius.sm),
                                ) {
                                    Text(text = "App Settings")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))

                    Text(
                        text = "REMINDER LEAD TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                    ) {
                        ReminderLeadTime.entries.forEach { lead ->
                            val selected = uiState.selectedLeadTime == lead
                            Surface(
                                onClick = { onSelectLeadTime(lead) },
                                modifier = Modifier.weight(1f),
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
                                Box(
                                    modifier = Modifier.padding(vertical = NothingSpacing.xs),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = lead.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(NothingSpacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                    ) {
                        Button(
                            onClick = onSendTestWatchAlert,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(text = "Test Now")
                        }
                        OutlinedButton(
                            onClick = onScheduleDelayedTestWatchAlert,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) {
                            Text(text = "Test in 10s (Lock)")
                        }
                        OutlinedButton(
                            onClick = onRescheduleNow,
                            enabled = uiState.isBridgeEnabled,
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Reschedule",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    if (uiState.lastScheduledLabel != null) {
                        Spacer(modifier = Modifier.height(NothingSpacing.xs))
                        Text(
                            text = "Last scheduled: ${uiState.lastScheduledCount} alerts at ${uiState.lastScheduledLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                NothingSectionHeader(
                    title = "Next 48h Planned Watch Alerts",
                    trailingContent = {
                        Text(
                            text = "${uiState.upcomingReminders.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            if (uiState.upcomingReminders.isEmpty()) {
                item {
                    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No upcoming events in the next 48 hours",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.xxs))
                        Text(
                            text = "Sync your Google Calendar in the Calendar tab to queue upcoming alerts automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(uiState.upcomingReminders.take(8), key = { it.instanceKey }) { reminder ->
                    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "ALERT · ${reminder.triggerLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = reminder.calendarName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.xxs))
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = reminder.timeRangeLabel + (reminder.location?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "INFINIX XOS 15 + MI FITNESS SETUP",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    Text(
                        text = "1. Open Mi Fitness → Device → Notifications → App notifications → turn ON App notifications AND enable NothingWear.\n" +
                            "2. Important: Mi Fitness defaults to 'Only receive when phone is locked' — either turn that off OR tap 'Test in 10s (Lock)' above and immediately lock your phone screen.\n" +
                            "3. Confirm Watch S4 is Connected in Mi Fitness and DND / Sleep mode is OFF on the watch.\n" +
                            "4. In Infinix XOS 15 App Management → NothingWear → Battery → set Unrestricted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
