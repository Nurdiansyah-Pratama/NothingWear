package com.nothingwear.app.features.watchface

import com.nothingwear.app.domain.model.WatchFaceCategory
import com.nothingwear.app.domain.model.WatchFaceExportArtifact
import com.nothingwear.app.domain.model.WatchFacePresets
import com.nothingwear.app.domain.model.WatchFaceProject

enum class StudioEditorTab(val label: String) {
    THEMES("Themes"),
    COLORS("Colors"),
    LAYOUT("Layout"),
    WIDGETS("Widgets"),
    PROJECTS("Export"),
}

data class WatchFaceStudioUiState(
    val activeTab: StudioEditorTab = StudioEditorTab.THEMES,
    val draft: WatchFaceProject = WatchFacePresets.defaultFor(WatchFaceCategory.NOTHING),
    val savedProjects: List<WatchFaceProject> = emptyList(),
    val nextCalendarEventLabel: String? = null,
    val isExporting: Boolean = false,
    val lastExportArtifact: WatchFaceExportArtifact? = null,
    val feedbackMessage: String? = null,
)
