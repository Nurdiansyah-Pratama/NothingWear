package com.nothingwear.app.features.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nothingwear.app.core.designsystem.component.MilestonePlaceholderCard
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingSpacing

@Composable
fun DevicesScreen(
    modifier: Modifier = Modifier,
) {
    // TODO(M2): Implement runtime BLUETOOTH_SCAN / BLUETOOTH_CONNECT permission handling,
    //           read-only BLE discovery, Watch S4 identification, GATT service inspection,
    //           and redacted diagnostic logs without writing to unknown characteristics.
    // TODO(M4): Wire isolated protocol-adapter/xiaomi-watch-s4 after M3 research completes.
    // TODO(M6): Surface only verified battery and health data sources; never fabricate metrics.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(NothingSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        item {
            Text(
                text = "DEVICES & DIAGNOSTICS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "TARGET: XIAOMI WATCH S4 · READ-ONLY BLE DIAGNOSTICS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            MilestonePlaceholderCard(
                milestoneTag = "M2 · Planned",
                title = "BLE Discovery & GATT Inspector Pending",
                description = "Bluetooth permissions and BLE scanning are intentionally inactive in M0. M2 will add read-only discovery and GATT diagnostics while preserving existing Mi Fitness pairing.",
                plannedItems = listOf(
                    "Android-version-aware BLUETOOTH_SCAN & BLUETOOTH_CONNECT requests",
                    "Scan nearby BLE peripherals and identify Xiaomi Watch S4",
                    "Inspect GATT services and characteristics in strict read-only mode",
                    "Capture redacted diagnostic logs for troubleshooting",
                ),
                safetyNote = "SAFETY: No BLE writes, no unknown commands, no firmware tampering, and no Mi Fitness pairing disruption.",
                icon = Icons.AutoMirrored.Outlined.BluetoothSearching,
            )
        }

        item {
            NothingSectionHeader(title = "Health & Telemetry Policy")
        }

        item {
            MilestonePlaceholderCard(
                milestoneTag = "M6 · Verified Data Only",
                title = "Health & Battery Metrics Unavailable",
                description = "NothingWear never generates placeholder steps, heart rate, sleep, or battery readings. Metrics will only appear when backed by verified APIs or protocol data.",
                plannedItems = listOf(
                    "Battery level: Unavailable in M0",
                    "Steps / Heart Rate / Sleep: Unavailable in M0",
                    "Watch S4 Protocol Adapter State: PROTOCOL_UNKNOWN (M3/M4)",
                ),
                safetyNote = "Research tools live outside the Android app inside protocol-lab/.",
                icon = Icons.Outlined.FavoriteBorder,
            )
        }

        item {
            NothingSurfaceCard {
                Text(
                    text = "REFERENCE ENVIRONMENT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Text(
                    text = "Primary target phone: Infinix XOS 15",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Reference phone (M3 comparison): Xiaomi 14T",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
