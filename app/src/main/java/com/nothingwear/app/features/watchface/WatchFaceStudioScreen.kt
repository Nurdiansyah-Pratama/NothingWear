package com.nothingwear.app.features.watchface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nothingwear.app.core.designsystem.component.MilestonePlaceholderCard
import com.nothingwear.app.core.designsystem.component.NothingSectionHeader
import com.nothingwear.app.core.designsystem.component.NothingStatusBadge
import com.nothingwear.app.core.designsystem.component.NothingSurfaceCard
import com.nothingwear.app.core.designsystem.theme.NothingSpacing
import com.nothingwear.app.domain.model.WatchFaceCategory

@Composable
fun WatchFaceStudioScreen(
    modifier: Modifier = Modifier,
) {
    // TODO(M5): Implement interactive 466x466 watch-face editor, theme presets, typography/color
    //           controls, complication previews, project persistence, and researched export pipeline.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(NothingSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        item {
            Text(
                text = "WATCH FACE STUDIO",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "466 × 466 PX TARGET · DESIGN WORKSPACE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            MilestonePlaceholderCard(
                milestoneTag = "M5 · Planned",
                title = "Editor & Export Pipeline Pending",
                description = "The interactive watch-face studio and project storage will be built in M5, separated cleanly from any verified Xiaomi Watch S4 export/packaging format.",
                plannedItems = listOf(
                    "Interactive 466 × 466 px circular preview canvas",
                    "Color, typography, layout, and complication editor",
                    "Save and load local watch-face projects",
                    "Research real Watch S4 watch-face package format before enabling export",
                ),
                safetyNote = "IMPORTANT: A visual preview is not an installable watch face. Export/install mechanisms will only be enabled after physical verification.",
                icon = Icons.Outlined.Palette,
            )
        }

        item {
            NothingSectionHeader(
                title = "Planned Theme Categories",
                trailingContent = {
                    NothingStatusBadge(label = "${WatchFaceCategory.entries.size} Presets")
                },
            )
        }

        items(WatchFaceCategory.entries.chunked(2)) { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
            ) {
                rowCategories.forEach { category ->
                    NothingSurfaceCard(modifier = Modifier.weight(1f)) {
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
                            Text(
                                text = "M5",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.xxs))
                        Column {
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
