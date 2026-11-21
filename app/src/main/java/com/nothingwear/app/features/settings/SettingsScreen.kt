package com.nothingwear.app.features.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nothingwear.app.BuildConfig
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingStatusBadge
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingRadius
import com.nothingwear.app.core.designsystem.theme.NothingSpacing
import com.nothingwear.app.core.designsystem.theme.toAccentSpec
import com.nothingwear.app.domain.model.AccentColorOption
import com.nothingwear.app.domain.model.ThemeMode
import com.nothingwear.app.widget.NothingWearWidgetProvider

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun checkIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(PowerManager::class.java)
        return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    var isBatteryUnrestricted by remember { mutableStateOf(checkIgnoringBatteryOptimizations()) }
    var widgetPinStatus by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isBatteryUnrestricted = checkIgnoringBatteryOptimizations()
    }

    SettingsScreen(
        uiState = uiState,
        isBatteryUnrestricted = isBatteryUnrestricted,
        widgetPinStatus = widgetPinStatus,
        onPinHomeWidget = {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, NothingWearWidgetProvider::class.java)
            if (manager.isRequestPinAppWidgetSupported) {
                val requested = manager.requestPinAppWidget(provider, null, null)
                widgetPinStatus = if (requested) {
                    "Widget pin prompt sent — confirm on your Infinix home screen."
                } else {
                    "Long-press your home screen → Widgets → NothingWear Live."
                }
            } else {
                widgetPinStatus = "Long-press your home screen → Widgets → NothingWear Live (4×2)."
            }
            NothingWearWidgetProvider.requestSyncBroadcast(context)
        },
        onOpenBatteryOptimizationSettings = {
            val batteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            runCatching { context.startActivity(batteryIntent) }
                .onFailure {
                    val detailsIntent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(detailsIntent)
                }
        },
        onOpenAppDetailsSettings = {
            val detailsIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
            context.startActivity(detailsIntent)
        },
        onSelectThemeMode = viewModel::onSelectThemeMode,
        onSelectAccentColor = viewModel::onSelectAccentColor,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectAccentColor: (AccentColorOption) -> Unit,
    modifier: Modifier = Modifier,
    isBatteryUnrestricted: Boolean = false,
    widgetPinStatus: String? = null,
    onPinHomeWidget: () -> Unit = {},
    onOpenBatteryOptimizationSettings: () -> Unit = {},
    onOpenAppDetailsSettings: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(NothingSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        item {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "APPEARANCE · ARCHITECTURE · SAFETY BOUNDARIES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            NothingSectionHeader(title = "Theme Mode")
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingSurfaceCard {
                Text(
                    text = "Dark mode is the default NothingWear aesthetic. Switch anytime to Light or follow Android system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = uiState.themeMode == mode
                        ThemeModeChip(
                            label = mode.name,
                            selected = selected,
                            onClick = { onSelectThemeMode(mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            NothingSectionHeader(title = "Accent Color")
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingSurfaceCard {
                Text(
                    text = "Customize primary highlights and active states across the interface.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                    AccentColorOption.entries.chunked(2).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                        ) {
                            rowOptions.forEach { option ->
                                AccentOptionTile(
                                    option = option,
                                    selected = uiState.accentColor == option,
                                    onClick = { onSelectAccentColor(option) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            NothingSectionHeader(
                title = "Home Screen Widget (4×2)",
                trailingContent = {
                    NothingStatusBadge(
                        label = "LIVE SYNC",
                        dotColor = MaterialTheme.colorScheme.primary,
                    )
                },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingSurfaceCard {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NothingWear Live Widget",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.xxs))
                        Text(
                            text = "Displays verified Steps, Heart Rate, Sleep, and your Next Calendar Event directly on your Infinix home screen with 1-tap SYNC.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!widgetPinStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    Text(
                        text = widgetPinStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Button(
                    onClick = onPinHomeWidget,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(NothingRadius.sm),
                ) {
                    Text("Add Widget to Home Screen")
                }
            }
        }

        item {
            NothingSectionHeader(
                title = "Background & XOS 15 Battery Reliability",
                trailingContent = {
                    NothingStatusBadge(
                        label = if (isBatteryUnrestricted) "UNRESTRICTED" else "OPTIMIZED",
                        dotColor = if (isBatteryUnrestricted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingSurfaceCard {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                ) {
                    Icon(
                        imageVector = if (isBatteryUnrestricted) {
                            Icons.Outlined.VerifiedUser
                        } else {
                            Icons.Outlined.BatteryAlert
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBatteryUnrestricted) {
                                "Background Execution Unrestricted"
                            } else {
                                "Battery Optimization Active (${Build.MANUFACTURER} ${Build.MODEL})"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.xxs))
                        Text(
                            text = if (isBatteryUnrestricted) {
                                "WorkManager Watch Reminder Bridge alerts can fire reliably without OS power throttling."
                            } else {
                                "On Infinix XOS 15 (Phone Master / Power Marathon), battery optimization can delay scheduled WorkManager calendar alerts to Xiaomi Watch S4. Set NothingWear and Mi Fitness to Unrestricted."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                ) {
                    OutlinedButton(
                        onClick = onOpenBatteryOptimizationSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(NothingRadius.sm),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Battery Settings")
                    }
                    OutlinedButton(
                        onClick = onOpenAppDetailsSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(NothingRadius.sm),
                    ) {
                        Text("App Info")
                    }
                }
            }
        }

        item {
            NothingSectionHeader(title = "Application & Environment")
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingSurfaceCard {
                MetadataRow(label = "Package", value = BuildConfig.APPLICATION_ID)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = NothingSpacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                MetadataRow(label = "Version", value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = NothingSpacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                MetadataRow(label = "Release Track", value = "M7 — Integration & Release")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = NothingSpacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                MetadataRow(label = "Database", value = "Room v2 (MIGRATION_1_2)")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = NothingSpacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                MetadataRow(label = "Target Wearable", value = "Xiaomi Watch S4")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = NothingSpacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                MetadataRow(label = "Permissions", value = "Calendar, BLE, Alerts, Health")
            }
        }

        item {
            NothingSurfaceCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = "SECURITY & DOCUMENTED LIMITATIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Text(
                    text = "• Independent & Safe: NothingWear never modifies Xiaomi firmware, bypasses WearAuthV2 authentication, writes to 0xFDAB, or interferes with official Mi Fitness pairing.\n" +
                        "• Native Watch S4 Events: OEM-whitelisted & WearAuthV2-encrypted in Mi Fitness; NothingWear provides a verified WorkManager Watch Reminder Bridge.\n" +
                        "• Watch Faces: Exports 466×466 clean Background-Only PNGs aligned to Mi Fitness's 5 Kustom templates (not proprietary .bin/.face binaries).\n" +
                        "• Health & Battery: Uses live BLE Heart Rate (0x180D/0x2A37) and read-only Health Connect; Watch S4 battery (0x180F) & DIS (0x180A) remain honestly UNAVAILABLE.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThemeModeChip(
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
            MaterialTheme.colorScheme.surfaceVariant
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
private fun AccentOptionTile(
    option: AccentColorOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swatch = option.toAccentSpec().previewSwatch
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(NothingRadius.sm),
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NothingSpacing.sm, vertical = NothingSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Text(
                text = option.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
