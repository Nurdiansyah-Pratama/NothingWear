package com.nothingwear.app.features.device

import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nothingwear.app.core.bluetooth.GattUuidLookup
import com.nothingwear.app.core.bluetooth.WatchMatchConfidence
import com.nothingwear.app.core.designsystem.component.MilestonePlaceholderCard
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingStatusBadge
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingRadius
import com.nothingwear.app.core.designsystem.theme.NothingSpacing
import com.nothingwear.app.domain.model.BleDeviceSummary
import com.nothingwear.app.domain.model.BluetoothAdapterStatus
import com.nothingwear.app.domain.model.DiagnosticLogEntry
import com.nothingwear.app.domain.model.DiagnosticLogLevel
import com.nothingwear.app.domain.model.GattInspectionSession
import com.nothingwear.app.domain.model.GattServiceInfo
import com.nothingwear.app.domain.model.WatchConnectionState

@Composable
fun DevicesRoute(
    modifier: Modifier = Modifier,
    viewModel: DevicesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = viewModel::onPermissionsResult,
    )

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { viewModel.onRefreshEnvironment() },
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onRefreshEnvironment()
    }

    DevicesScreen(
        uiState = uiState,
        onRequestPermissions = {
            if (uiState.requiredPermissions.isNotEmpty()) {
                permissionsLauncher.launch(uiState.requiredPermissions.toTypedArray())
            }
        },
        onOpenAppSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
            context.startActivity(intent)
        },
        onEnableBluetooth = {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            runCatching { enableBluetoothLauncher.launch(intent) }
                .onFailure {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
        },
        onRefreshEnvironment = viewModel::onRefreshEnvironment,
        onToggleScan = viewModel::onToggleScan,
        onToggleFilterXiaomiOnly = viewModel::onToggleFilterXiaomiOnly,
        onInspectDevice = viewModel::onInspectDevice,
        onDisconnectGatt = viewModel::onDisconnectGatt,
        onToggleServiceExpanded = viewModel::onToggleServiceExpanded,
        onCopyRedactedReport = {
            val report = viewModel.generateRedactedReport()
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("NothingWear BLE Diagnostics", report))
        },
        onClearLogs = viewModel::onClearLogs,
        modifier = modifier,
    )
}

@Composable
fun DevicesScreen(
    uiState: DevicesUiState,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onRefreshEnvironment: () -> Unit,
    onToggleScan: () -> Unit,
    onToggleFilterXiaomiOnly: () -> Unit,
    onInspectDevice: (String) -> Unit,
    onDisconnectGatt: () -> Unit,
    onToggleServiceExpanded: (String) -> Unit,
    onCopyRedactedReport: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(NothingSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        item {
            DevicesHeader(
                adapterStatus = uiState.adapterStatus,
                onRefresh = onRefreshEnvironment,
                onCopyReport = onCopyRedactedReport,
            )
        }

        if (!uiState.reportCopiedNotice.isNullOrBlank()) {
            item {
                NothingSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    borderColor = MaterialTheme.colorScheme.primary,
                    contentPadding = PaddingValues(NothingSpacing.sm),
                ) {
                    Text(
                        text = uiState.reportCopiedNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        if (!uiState.hasBlePermissions) {
            item {
                BlePermissionCard(
                    requiredPermissions = uiState.requiredPermissions,
                    permissionDeniedOnce = uiState.permissionDeniedOnce,
                    onRequestPermissions = onRequestPermissions,
                    onOpenAppSettings = onOpenAppSettings,
                )
            }
        } else if (uiState.adapterStatus != BluetoothAdapterStatus.ENABLED) {
            item {
                BluetoothDisabledCard(
                    adapterStatus = uiState.adapterStatus,
                    onEnableBluetooth = onEnableBluetooth,
                )
            }
        } else {
            item {
                ScanControlsBar(
                    isScanning = uiState.isScanning,
                    showXiaomiOrBondedOnly = uiState.showXiaomiOrBondedOnly,
                    shownCount = uiState.devices.size,
                    totalCount = uiState.totalDiscoveredCount,
                    onToggleScan = onToggleScan,
                    onToggleFilter = onToggleFilterXiaomiOnly,
                )
            }
        }

        val session = uiState.gattSession
        if (session.deviceAddressKey != null || session.services.isNotEmpty() || session.lastStatusNote != null) {
            item {
                NothingSectionHeader(
                    title = "Read-Only GATT Inspector",
                    trailingContent = {
                        NothingStatusBadge(
                            label = session.connectionState.name.replace('_', ' '),
                            dotColor = when (session.connectionState) {
                                WatchConnectionState.CONNECTED,
                                WatchConnectionState.PROTOCOL_UNKNOWN,
                                WatchConnectionState.READY -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                GattSessionCard(
                    session = session,
                    expandedServiceUuids = uiState.expandedServiceUuids,
                    onDisconnect = onDisconnectGatt,
                    onToggleServiceExpanded = onToggleServiceExpanded,
                )
            }
        }

        item {
            NothingSectionHeader(
                title = "Bonded & Nearby BLE Devices",
                trailingContent = {
                    Text(
                        text = "${uiState.devices.size} SHOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        if (uiState.devices.isEmpty()) {
            item {
                NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.BluetoothSearching,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xxs)) {
                            Text(
                                text = if (!uiState.hasBlePermissions) {
                                    "Bluetooth permissions not granted"
                                } else if (uiState.showXiaomiOrBondedOnly && uiState.totalDiscoveredCount > 0) {
                                    "No Xiaomi or bonded devices matched filter"
                                } else {
                                    "No BLE devices listed yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Tap Start Scan to discover nearby BLE peripherals and list bonded wearables (including Xiaomi Watch S4 paired via Mi Fitness).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            items(uiState.devices, key = { it.addressKey }) { device ->
                DiscoveredDeviceCard(
                    device = device,
                    isCurrentlyInspected = uiState.gattSession.deviceAddressKey == device.addressKey &&
                        uiState.gattSession.connectionState != WatchConnectionState.DISCONNECTED,
                    canInspect = uiState.hasBlePermissions && uiState.adapterStatus == BluetoothAdapterStatus.ENABLED,
                    onInspect = { onInspectDevice(device.addressKey) },
                )
            }
        }

        item {
            NothingSectionHeader(
                title = "Diagnostic Logs (Redacted)",
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onCopyRedactedReport) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy redacted report",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = onClearLogs,
                            enabled = uiState.logs.isNotEmpty(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Clear logs",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            DiagnosticLogsCard(logs = uiState.logs)
        }

        item {
            TroubleshootingCard()
        }

        item {
            // TODO(M6): Surface only verified health data sources; never fabricate metrics.
            MilestonePlaceholderCard(
                milestoneTag = "M6 · Verified Data Only",
                title = "Health Metrics Unavailable",
                description = "NothingWear never fabricates steps, heart rate, sleep, or unsupported battery numbers. Standard Battery Level (0x2A19) appears above only if exposed by the inspected device.",
                plannedItems = listOf(
                    "Steps / Heart Rate / Sleep: Unavailable in M2",
                    "Watch S4 Native Events Protocol: PROTOCOL_UNKNOWN (M3/M4)",
                ),
                safetyNote = "SAFETY: Strict read-only BLE diagnostics. Zero GATT writes, zero firmware modifications, official Mi Fitness pairing untouched.",
                icon = Icons.Outlined.FavoriteBorder,
            )
        }
    }
}

@Composable
private fun DevicesHeader(
    adapterStatus: BluetoothAdapterStatus,
    onRefresh: () -> Unit,
    onCopyReport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "DEVICES & BLE",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "BT: $adapterStatus · READ-ONLY GATT · MI FITNESS SAFE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCopyReport) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy redacted diagnostic report",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Refresh Bluetooth state",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun BlePermissionCard(
    requiredPermissions: List<String>,
    permissionDeniedOnce: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val shortNames = requiredPermissions.joinToString(", ") { it.substringAfterLast('.') }
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
                text = "BLUETOOTH PERMISSIONS REQUIRED",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = "Requested: $shortNames. Used strictly to discover nearby BLE wearables, list your paired Xiaomi Watch S4, and inspect GATT services in read-only mode. No location tracking and zero BLE writes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(NothingRadius.sm),
            ) {
                Text(text = "Grant Bluetooth")
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
private fun BluetoothDisabledCard(
    adapterStatus: BluetoothAdapterStatus,
    onEnableBluetooth: () -> Unit,
) {
    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.BluetoothDisabled,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (adapterStatus == BluetoothAdapterStatus.UNSUPPORTED) {
                        "BLE Unsupported"
                    } else {
                        "Bluetooth is Off"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (adapterStatus == BluetoothAdapterStatus.UNSUPPORTED) {
                        "This Android device does not expose Bluetooth Low Energy hardware."
                    } else {
                        "Turn on Bluetooth to list paired devices and scan for Xiaomi Watch S4."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (adapterStatus == BluetoothAdapterStatus.DISABLED) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            Button(
                onClick = onEnableBluetooth,
                shape = RoundedCornerShape(NothingRadius.sm),
            ) {
                Text(text = "Enable Bluetooth")
            }
        }
    }
}

@Composable
private fun ScanControlsBar(
    isScanning: Boolean,
    showXiaomiOrBondedOnly: Boolean,
    shownCount: Int,
    totalCount: Int,
    onToggleScan: () -> Unit,
    onToggleFilter: () -> Unit,
) {
    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onToggleScan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(NothingRadius.sm),
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.size(NothingSpacing.xs))
                    Text(text = "Stop Scan")
                } else {
                    Text(text = "Start BLE Scan")
                }
            }
            OutlinedButton(
                onClick = onToggleFilter,
                shape = RoundedCornerShape(NothingRadius.sm),
            ) {
                Text(
                    text = if (showXiaomiOrBondedOnly) "Xiaomi/Paired" else "All ($totalCount)",
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = "Showing $shownCount of $totalCount devices. Auto-stops after 15s to preserve battery on XOS 15.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GattSessionCard(
    session: GattInspectionSession,
    expandedServiceUuids: Set<String>,
    onDisconnect: () -> Unit,
    onToggleServiceExpanded: (String) -> Unit,
) {
    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (session.connectionState == WatchConnectionState.PROTOCOL_UNKNOWN ||
            session.connectionState == WatchConnectionState.CONNECTED
        ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.deviceName ?: "No active device",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!session.redactedAddress.isNullOrBlank()) {
                    Text(
                        text = session.redactedAddress,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (session.deviceAddressKey != null) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(NothingRadius.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LinkOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = "Close")
                }
            }
        }

        if (!session.lastStatusNote.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            Text(
                text = session.lastStatusNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val info = session.standardDeviceInfo
        val hasAnyStandardInfo = info.batteryPercent != null ||
            info.manufacturerName != null ||
            info.modelNumber != null ||
            info.firmwareRevision != null ||
            info.hardwareRevision != null
        if (hasAnyStandardInfo || session.services.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            Text(
                text = "STANDARD SIG READ-ONLY INFO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xxs))
            Text(
                text = "Battery (0x2A19): ${info.batteryPercent?.let { "$it%" } ?: "Not exposed"} · " +
                    "Model: ${info.modelNumber ?: "N/A"} · " +
                    "FW: ${info.firmwareRevision ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!info.manufacturerName.isNullOrBlank()) {
                Text(
                    text = "Manufacturer: ${info.manufacturerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (session.services.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            Text(
                text = "DISCOVERED GATT SERVICES (${session.services.size}) — READ-ONLY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                session.services.forEach { service ->
                    GattServiceRow(
                        service = service,
                        expanded = service.uuid in expandedServiceUuids,
                        onToggle = { onToggleServiceExpanded(service.uuid) },
                    )
                }
            }
        }

        if (session.sdpUuids.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            Text(
                text = "CACHED CLASSIC / SDP UUIDS (${session.sdpUuids.size}) — READ-ONLY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xxs)) {
                session.sdpUuids.forEach { uuid ->
                    Text(
                        text = "• ${GattUuidLookup.sdpUuidDisplayName(uuid)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "  $uuid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GattServiceRow(
    service: GattServiceInfo,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NothingRadius.sm),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(NothingSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${service.uuid} · ${service.characteristics.size} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                if (service.characteristics.isEmpty()) {
                    Text(
                        text = "No characteristics exposed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                        service.characteristics.forEach { ch ->
                            Column {
                                Text(
                                    text = ch.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = ch.uuid,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (ch.properties.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        ch.properties.forEach { prop ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            ) {
                                                Text(
                                                    text = prop,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceCard(
    device: BleDeviceSummary,
    isCurrentlyInspected: Boolean,
    canInspect: Boolean,
    onInspect: () -> Unit,
) {
    val isXiaomiCandidate = device.matchConfidence != WatchMatchConfidence.OTHER_PERIPHERAL
    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isXiaomiCandidate) {
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
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Watch,
                    contentDescription = null,
                    tint = if (isXiaomiCandidate) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NothingStatusBadge(
                label = device.matchConfidence.label,
                dotColor = if (isXiaomiCandidate) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Spacer(modifier = Modifier.height(NothingSpacing.xs))

        Text(
            text = "${device.redactedAddress} · ${device.transportType.label} · ${device.source.label}" +
                (device.rssi?.let { " · $it dBm" } ?: "") +
                if (device.isBonded) " · Bonded" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(NothingSpacing.xxs))

        Text(
            text = device.matchReason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(NothingSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(
                onClick = onInspect,
                enabled = canInspect,
                shape = RoundedCornerShape(NothingRadius.sm),
            ) {
                Text(text = if (isCurrentlyInspected) "Re-Inspect GATT" else "Inspect GATT (Read-Only)")
            }
        }
    }
}

@Composable
private fun DiagnosticLogsCard(
    logs: List<DiagnosticLogEntry>,
) {
    NothingSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (logs.isEmpty()) {
            Text(
                text = "No diagnostic log entries yet. Grant Bluetooth permissions and tap Start BLE Scan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                logs.take(15).forEach { entry ->
                    val levelColor = when (entry.level) {
                        DiagnosticLogLevel.ERROR -> MaterialTheme.colorScheme.error
                        DiagnosticLogLevel.WARN -> MaterialTheme.colorScheme.primary
                        DiagnosticLogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = "[${entry.timestampLabel}] ${entry.message}",
                        style = MaterialTheme.typography.labelSmall,
                        color = levelColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun TroubleshootingCard() {
    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
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
                text = "TROUBLESHOOTING & MI FITNESS COEXISTENCE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = "• Watch S4 already connected to Mi Fitness may not advertise in BLE scans — check the 'System GATT' or 'Paired / Bonded' entries listed above.\n" +
                "• GATT status 133 or immediate disconnect usually means the watch link is busy with Mi Fitness; close the inspection session cleanly rather than unpairing.\n" +
                "• All MAC addresses are masked in logs and copied diagnostic reports.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
