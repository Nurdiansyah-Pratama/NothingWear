package com.nothingwear.app.features.watchface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingStatusBadge
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingRadius
import com.nothingwear.app.core.designsystem.theme.NothingSpacing
import com.nothingwear.app.domain.model.MiFitnessCustomTemplate
import com.nothingwear.app.domain.model.WatchFaceCategory
import com.nothingwear.app.domain.model.WatchFaceColorPreset
import com.nothingwear.app.domain.model.WatchFaceComplication
import com.nothingwear.app.domain.model.WatchFaceDialStyle
import com.nothingwear.app.domain.model.WatchFaceFontStyle
import com.nothingwear.app.domain.model.WatchFacePresets
import com.nothingwear.app.domain.model.WatchFaceProject
import com.nothingwear.app.domain.model.WatchFaceRingStyle

@Composable
fun WatchFaceStudioRoute(
    modifier: Modifier = Modifier,
    viewModel: WatchFaceStudioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WatchFaceStudioScreen(
        uiState = uiState,
        onSelectTab = viewModel::onSelectTab,
        onSelectMiFitnessTemplate = viewModel::onSelectMiFitnessTemplate,
        onToggleSafeZoneGuides = viewModel::onToggleSafeZoneGuides,
        onSelectCategoryPreset = viewModel::onSelectCategoryPreset,
        onProjectNameChanged = viewModel::onProjectNameChanged,
        onBackgroundColorSelected = viewModel::onBackgroundColorSelected,
        onAccentColorSelected = viewModel::onAccentColorSelected,
        onTextColorSelected = viewModel::onTextColorSelected,
        onDialStyleSelected = viewModel::onDialStyleSelected,
        onRingStyleSelected = viewModel::onRingStyleSelected,
        onFontStyleSelected = viewModel::onFontStyleSelected,
        onToggle24Hour = viewModel::onToggle24Hour,
        onToggleShowSeconds = viewModel::onToggleShowSeconds,
        onTimeScaleChanged = viewModel::onTimeScaleChanged,
        onTopComplicationSelected = viewModel::onTopComplicationSelected,
        onBottomComplicationSelected = viewModel::onBottomComplicationSelected,
        onLeftComplicationSelected = viewModel::onLeftComplicationSelected,
        onRightComplicationSelected = viewModel::onRightComplicationSelected,
        onSaveProject = { viewModel.onSaveProject(asNewCopy = false) },
        onSaveProjectCopy = { viewModel.onSaveProject(asNewCopy = true) },
        onLoadProject = viewModel::onLoadProject,
        onDeleteProject = viewModel::onDeleteProject,
        onExportBackgroundOnly = viewModel::onExportBackgroundOnly,
        onExportPreviewAndSpec = viewModel::onExportPreviewAndSpec,
        modifier = modifier,
    )
}

@Composable
fun WatchFaceStudioScreen(
    uiState: WatchFaceStudioUiState,
    onSelectTab: (StudioEditorTab) -> Unit,
    onSelectMiFitnessTemplate: (MiFitnessCustomTemplate) -> Unit,
    onToggleSafeZoneGuides: (Boolean) -> Unit,
    onSelectCategoryPreset: (WatchFaceCategory) -> Unit,
    onProjectNameChanged: (String) -> Unit,
    onBackgroundColorSelected: (Long) -> Unit,
    onAccentColorSelected: (Long) -> Unit,
    onTextColorSelected: (Long) -> Unit,
    onDialStyleSelected: (WatchFaceDialStyle) -> Unit,
    onRingStyleSelected: (WatchFaceRingStyle) -> Unit,
    onFontStyleSelected: (WatchFaceFontStyle) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onToggleShowSeconds: (Boolean) -> Unit,
    onTimeScaleChanged: (Float) -> Unit,
    onTopComplicationSelected: (WatchFaceComplication) -> Unit,
    onBottomComplicationSelected: (WatchFaceComplication) -> Unit,
    onLeftComplicationSelected: (WatchFaceComplication) -> Unit,
    onRightComplicationSelected: (WatchFaceComplication) -> Unit,
    onSaveProject: () -> Unit,
    onSaveProjectCopy: () -> Unit,
    onLoadProject: (WatchFaceProject) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onExportBackgroundOnly: () -> Unit,
    onExportPreviewAndSpec: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WATCH FACE STUDIO",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "466 × 466 PX PREVIEW · ${uiState.draft.category.displayName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSaveProject) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = "Save watch-face project",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uiState.draft.name.ifBlank { "Untitled Watch Face" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        NothingStatusBadge(
                            label = if (uiState.miFitnessTemplate == MiFitnessCustomTemplate.NONE) {
                                "466×466 PREVIEW"
                            } else {
                                "KUSTOM: ${uiState.miFitnessTemplate.displayName.uppercase()}"
                            },
                            dotColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    WatchFaceCanvasPreview(
                        project = uiState.draft,
                        nextEventLabel = uiState.nextCalendarEventLabel,
                        diameter = 232.dp,
                        miFitnessTemplate = uiState.miFitnessTemplate,
                        showSafeZoneGuides = uiState.showSafeZoneGuides,
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    Text(
                        text = if (uiState.miFitnessTemplate == MiFitnessCustomTemplate.NONE) {
                            "Preview mode — select a Mi Fitness Kustom template below to align safe zones and export a clean Background-Only PNG."
                        } else {
                            "Cyan dashed guides show Mi Fitness's live clock/hands position (excluded from exported PNG)."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            MiFitnessTemplateCard(
                selectedTemplate = uiState.miFitnessTemplate,
                showSafeZoneGuides = uiState.showSafeZoneGuides,
                isExporting = uiState.isExporting,
                onSelectTemplate = onSelectMiFitnessTemplate,
                onToggleSafeZoneGuides = onToggleSafeZoneGuides,
                onQuickExportBackgroundOnly = onExportBackgroundOnly,
            )
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

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xxs),
            ) {
                StudioEditorTab.entries.forEach { tab ->
                    val selected = uiState.activeTab == tab
                    Surface(
                        onClick = { onSelectTab(tab) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(NothingRadius.sm),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        when (uiState.activeTab) {
            StudioEditorTab.THEMES -> {
                item {
                    NothingSectionHeader(title = "Design Categories (6 Presets)")
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                        WatchFaceCategory.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                            ) {
                                pair.forEach { category ->
                                    val selected = uiState.draft.category == category
                                    NothingSurfaceCard(
                                        modifier = Modifier.weight(1f),
                                        borderColor = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        containerColor = if (selected) {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        onClick = { onSelectCategoryPreset(category) },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = category.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(NothingSpacing.xxs))
                                        Text(
                                            text = category.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            StudioEditorTab.COLORS -> {
                item {
                    ColorPickerCard(
                        title = "Background Canvas",
                        options = WatchFacePresets.backgroundColors,
                        selectedArgb = uiState.draft.backgroundColorArgb,
                        onSelect = onBackgroundColorSelected,
                    )
                }
                item {
                    ColorPickerCard(
                        title = "Accent Highlight",
                        options = WatchFacePresets.accentColors,
                        selectedArgb = uiState.draft.accentColorArgb,
                        onSelect = onAccentColorSelected,
                    )
                }
                item {
                    ColorPickerCard(
                        title = "Primary Dial & Time Color",
                        options = WatchFacePresets.textColors,
                        selectedArgb = uiState.draft.textColorArgb,
                        onSelect = onTextColorSelected,
                    )
                }
            }

            StudioEditorTab.LAYOUT -> {
                item {
                    OptionSelectorCard(
                        title = "Dial Layout",
                        options = WatchFaceDialStyle.entries,
                        selected = uiState.draft.dialStyle,
                        labelOf = { it.label },
                        onSelect = onDialStyleSelected,
                    )
                }
                item {
                    OptionSelectorCard(
                        title = "Bezel & Index Ring",
                        options = WatchFaceRingStyle.entries,
                        selected = uiState.draft.ringStyle,
                        labelOf = { it.label },
                        onSelect = onRingStyleSelected,
                    )
                }
                item {
                    OptionSelectorCard(
                        title = "Typography Style",
                        options = WatchFaceFontStyle.entries,
                        selected = uiState.draft.fontStyle,
                        labelOf = { it.label },
                        onSelect = onFontStyleSelected,
                    )
                }
                item {
                    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        ToggleRow(
                            title = "24-Hour Clock",
                            subtitle = "Display 00–23 instead of 12-hour format",
                            checked = uiState.draft.is24Hour,
                            onCheckedChange = onToggle24Hour,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = NothingSpacing.sm),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ToggleRow(
                            title = "Show Seconds",
                            subtitle = "Enable live sweeping second hand or seconds counter",
                            checked = uiState.draft.showSeconds,
                            onCheckedChange = onToggleShowSeconds,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = NothingSpacing.sm),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            text = "TIME SCALE (${(uiState.draft.timeScale * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = uiState.draft.timeScale,
                            onValueChange = onTimeScaleChanged,
                            valueRange = 0.8f..1.25f,
                        )
                    }
                }
            }

            StudioEditorTab.WIDGETS -> {
                item {
                    OptionSelectorCard(
                        title = "Top Slot",
                        options = WatchFaceComplication.entries,
                        selected = uiState.draft.topComplication,
                        labelOf = { it.label },
                        onSelect = onTopComplicationSelected,
                    )
                }
                item {
                    OptionSelectorCard(
                        title = "Bottom Slot",
                        options = WatchFaceComplication.entries,
                        selected = uiState.draft.bottomComplication,
                        labelOf = { it.label },
                        onSelect = onBottomComplicationSelected,
                    )
                }
                item {
                    OptionSelectorCard(
                        title = "Left Slot",
                        options = WatchFaceComplication.entries,
                        selected = uiState.draft.leftComplication,
                        labelOf = { it.label },
                        onSelect = onLeftComplicationSelected,
                    )
                }
                item {
                    OptionSelectorCard(
                        title = "Right Slot",
                        options = WatchFaceComplication.entries,
                        selected = uiState.draft.rightComplication,
                        labelOf = { it.label },
                        onSelect = onRightComplicationSelected,
                    )
                }
                item {
                    NothingSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "DATA HONESTY NOTE: Date, Timezone, and Next Event use real local data from your device & Room cache. Battery Slot (BAT --%) and HR Slot (HR --) are layout placeholders only; NothingWear never fabricates health metrics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            StudioEditorTab.PROJECTS -> {
                item {
                    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SAVE & EXPORT PROJECT",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.xs))
                        OutlinedTextField(
                            value = uiState.draft.name,
                            onValueChange = onProjectNameChanged,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Project Name") },
                            shape = RoundedCornerShape(NothingRadius.sm),
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                        ) {
                            Button(
                                onClick = onSaveProject,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(NothingRadius.sm),
                            ) {
                                Text(text = if (uiState.draft.id > 0L) "Update" else "Save")
                            }
                            OutlinedButton(
                                onClick = onSaveProjectCopy,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(NothingRadius.sm),
                            ) {
                                Text(text = "Save Copy")
                            }
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.xs))
                        Button(
                            onClick = onExportBackgroundOnly,
                            enabled = !uiState.isExporting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.size(NothingSpacing.xs))
                                Text("Rendering 466×466...")
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = if (uiState.miFitnessTemplate == MiFitnessCustomTemplate.NONE) {
                                        "Export Background Only (Mi Fitness Kustom)"
                                    } else {
                                        "Export Background Only (${uiState.miFitnessTemplate.displayName})"
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.xs))
                        OutlinedButton(
                            onClick = onExportPreviewAndSpec,
                            enabled = !uiState.isExporting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(NothingRadius.sm),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Export Full Preview PNG + JSON Spec")
                        }

                        uiState.lastExportArtifact?.let { artifact ->
                            Spacer(modifier = Modifier.height(NothingSpacing.sm))
                            val modeLabel = if (artifact.backgroundOnly) {
                                if (artifact.miFitnessTemplate != MiFitnessCustomTemplate.NONE) {
                                    "Background Only · ${artifact.miFitnessTemplate.displayName}"
                                } else {
                                    "Background Only"
                                }
                            } else {
                                "Full Preview"
                            }
                            Text(
                                text = "Last export: $modeLabel (${artifact.widthPx}×${artifact.heightPx} PNG)" +
                                    if (artifact.savedToGallery) " → Pictures/NothingWear" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                item {
                    NothingSectionHeader(
                        title = "Saved Local Projects (Room)",
                        trailingContent = {
                            Text(
                                text = "${uiState.savedProjects.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }

                if (uiState.savedProjects.isEmpty()) {
                    item {
                        NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No saved projects yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Customize your watch face above and tap Save to store it offline in Room.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(uiState.savedProjects.size, key = { uiState.savedProjects[it].id }) { idx ->
                        val project = uiState.savedProjects[idx]
                        val isLoaded = project.id == uiState.draft.id
                        NothingSurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (isLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            onClick = { onLoadProject(project) },
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
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(project.backgroundColorArgb))
                                            .border(2.dp, Color(project.accentColorArgb), CircleShape),
                                    )
                                    Column {
                                        Text(
                                            text = project.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${project.category.displayName} · ${project.dialStyle.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteProject(project.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete project",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

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
                                text = "XIAOMI WATCH S4 EXPORT FORMAT STATUS",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.xs))
                        Text(
                            text = "• Recommended Watch S4 Workflow: Pick a Mi Fitness Kustom template above (Custom analog, elegance, digital, postcard, or diamond) → customize colors & bezel ring → tap Export Background Only. Then open Mi Fitness → Watch Face → Kustom → choose the matching template → pick the clean 466×466 PNG from Pictures/NothingWear. Zero hand/clock overlap!\n" +
                                "• Documented Limitation: Direct installation of compiled Xiaomi Vela watch-face binaries (.bin/.face via FaceInstallBleV2Impl) requires WearAuthV2 authenticated file transfer and Xiaomi's proprietary compiler. NothingWear keeps the editor decoupled and never claims a PNG preview is a native binary watch face.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MiFitnessTemplateCard(
    selectedTemplate: MiFitnessCustomTemplate,
    showSafeZoneGuides: Boolean,
    isExporting: Boolean,
    onSelectTemplate: (MiFitnessCustomTemplate) -> Unit,
    onToggleSafeZoneGuides: (Boolean) -> Unit,
    onQuickExportBackgroundOnly: () -> Unit,
) {
    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "MI FITNESS KUSTOM ALIGNMENT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "ZERO OVERLAP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            MiFitnessCustomTemplate.entries.forEach { template ->
                val isSelected = template == selectedTemplate
                Surface(
                    onClick = { onSelectTemplate(template) },
                    shape = RoundedCornerShape(NothingRadius.sm),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    Text(
                        text = template.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = NothingSpacing.sm, vertical = NothingSpacing.xs),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = selectedTemplate.safeZoneNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selectedTemplate != MiFitnessCustomTemplate.NONE) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = NothingSpacing.sm),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            ToggleRow(
                title = "Show Mi Fitness Safe-Zone Guide",
                subtitle = "Preview where live clock/hands sit (never exported to PNG)",
                checked = showSafeZoneGuides,
                onCheckedChange = onToggleSafeZoneGuides,
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            Button(
                onClick = onQuickExportBackgroundOnly,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NothingRadius.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Export Background Only (${selectedTemplate.displayName})")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerCard(
    title: String,
    options: List<WatchFaceColorPreset>,
    selectedArgb: Long,
    onSelect: (Long) -> Unit,
) {
    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            options.forEach { preset ->
                val selected = preset.argb == selectedArgb
                Surface(
                    onClick = { onSelect(preset.argb) },
                    shape = RoundedCornerShape(NothingRadius.sm),
                    color = if (selected) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        if (selected) 1.5.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = NothingSpacing.sm, vertical = NothingSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(preset.argb))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionSelectorCard(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            options.forEach { item ->
                val isSelected = item == selected
                Surface(
                    onClick = { onSelect(item) },
                    shape = RoundedCornerShape(NothingRadius.sm),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    Text(
                        text = labelOf(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = NothingSpacing.sm, vertical = NothingSpacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
