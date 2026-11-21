package com.nothingwear.app.features.settings

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nothingwear.app.BuildConfig
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingRadius
import com.nothingwear.app.core.designsystem.theme.NothingSpacing
import com.nothingwear.app.core.designsystem.theme.toAccentSpec
import com.nothingwear.app.domain.model.AccentColorOption
import com.nothingwear.app.domain.model.ThemeMode

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
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
                MetadataRow(label = "Milestone", value = "M5 — Watch Face Studio")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = NothingSpacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                MetadataRow(label = "Target Wearable", value = "Xiaomi Watch S4")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = NothingSpacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                MetadataRow(label = "Permissions", value = "Calendar, BLE, Notifications")
            }
        }

        item {
            NothingSurfaceCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = "SECURITY & ENGINEERING CONSTRAINTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Text(
                    text = "NothingWear operates independently without modifying Xiaomi firmware, bypassing authentication, or interfering with official Mi Fitness pairing.",
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
