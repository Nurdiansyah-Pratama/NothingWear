package com.nothingwear.app.features.watchface

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.domain.model.WatchFaceCategory
import com.nothingwear.app.domain.model.WatchFaceComplication
import com.nothingwear.app.domain.model.WatchFaceDialStyle
import com.nothingwear.app.domain.model.WatchFaceExportArtifact
import com.nothingwear.app.domain.model.WatchFaceExportResult
import com.nothingwear.app.domain.model.WatchFaceFontStyle
import com.nothingwear.app.domain.model.WatchFacePresets
import com.nothingwear.app.domain.model.WatchFaceProject
import com.nothingwear.app.domain.model.WatchFaceRingStyle
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.usecase.DeleteWatchFaceProjectUseCase
import com.nothingwear.app.domain.usecase.ExportWatchFaceArtifactUseCase
import com.nothingwear.app.domain.usecase.ObserveSavedWatchFacesUseCase
import com.nothingwear.app.domain.usecase.SaveWatchFaceProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchFaceStudioViewModel @Inject constructor(
    observeSavedWatchFaces: ObserveSavedWatchFacesUseCase,
    calendarRepository: CalendarRepository,
    private val saveWatchFaceProject: SaveWatchFaceProjectUseCase,
    private val deleteWatchFaceProject: DeleteWatchFaceProjectUseCase,
    private val exportWatchFaceArtifact: ExportWatchFaceArtifactUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val editorState = MutableStateFlow(EditorControls())

    val uiState: StateFlow<WatchFaceStudioUiState> = combine(
        editorState,
        observeSavedWatchFaces(),
        calendarRepository.observeSelectedEvents(),
    ) { controls, savedProjects, events ->
        val now = timeProvider.nowMillis()
        val nextEventTitle = events
            .filter { it.endTimeMillis >= now }
            .minByOrNull { it.startTimeMillis }
            ?.title

        WatchFaceStudioUiState(
            activeTab = controls.activeTab,
            draft = controls.draft,
            savedProjects = savedProjects,
            nextCalendarEventLabel = nextEventTitle,
            isExporting = controls.isExporting,
            lastExportArtifact = controls.lastExportArtifact,
            feedbackMessage = controls.feedbackMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = WatchFaceStudioUiState(),
    )

    fun onSelectTab(tab: StudioEditorTab) {
        editorState.update { it.copy(activeTab = tab) }
    }

    fun onSelectCategoryPreset(category: WatchFaceCategory) {
        val preset = WatchFacePresets.defaultFor(category)
        editorState.update {
            it.copy(
                draft = preset.copy(id = it.draft.id),
                feedbackMessage = "Applied ${category.displayName} preset (466×466 preview).",
            )
        }
    }

    fun onProjectNameChanged(name: String) {
        editorState.update { it.copy(draft = it.draft.copy(name = name)) }
    }

    fun onBackgroundColorSelected(argb: Long) {
        editorState.update { it.copy(draft = it.draft.copy(backgroundColorArgb = argb)) }
    }

    fun onAccentColorSelected(argb: Long) {
        editorState.update { it.copy(draft = it.draft.copy(accentColorArgb = argb)) }
    }

    fun onTextColorSelected(argb: Long) {
        editorState.update { it.copy(draft = it.draft.copy(textColorArgb = argb)) }
    }

    fun onDialStyleSelected(style: WatchFaceDialStyle) {
        editorState.update { it.copy(draft = it.draft.copy(dialStyle = style)) }
    }

    fun onRingStyleSelected(style: WatchFaceRingStyle) {
        editorState.update { it.copy(draft = it.draft.copy(ringStyle = style)) }
    }

    fun onFontStyleSelected(style: WatchFaceFontStyle) {
        editorState.update { it.copy(draft = it.draft.copy(fontStyle = style)) }
    }

    fun onToggle24Hour(enabled: Boolean) {
        editorState.update { it.copy(draft = it.draft.copy(is24Hour = enabled)) }
    }

    fun onToggleShowSeconds(enabled: Boolean) {
        editorState.update { it.copy(draft = it.draft.copy(showSeconds = enabled)) }
    }

    fun onTimeScaleChanged(scale: Float) {
        editorState.update { it.copy(draft = it.draft.copy(timeScale = scale.coerceIn(0.8f, 1.25f))) }
    }

    fun onTopComplicationSelected(complication: WatchFaceComplication) {
        editorState.update { it.copy(draft = it.draft.copy(topComplication = complication)) }
    }

    fun onBottomComplicationSelected(complication: WatchFaceComplication) {
        editorState.update { it.copy(draft = it.draft.copy(bottomComplication = complication)) }
    }

    fun onLeftComplicationSelected(complication: WatchFaceComplication) {
        editorState.update { it.copy(draft = it.draft.copy(leftComplication = complication)) }
    }

    fun onRightComplicationSelected(complication: WatchFaceComplication) {
        editorState.update { it.copy(draft = it.draft.copy(rightComplication = complication)) }
    }

    fun onSaveProject(asNewCopy: Boolean = false) {
        val target = editorState.value.draft.let {
            if (asNewCopy) it.copy(id = 0L) else it
        }
        viewModelScope.launch {
            val savedId = saveWatchFaceProject(target)
            editorState.update {
                it.copy(
                    draft = target.copy(id = savedId),
                    feedbackMessage = if (asNewCopy) {
                        "Saved copy '${target.name}' to local studio library."
                    } else {
                        "Saved '${target.name}' locally in Room."
                    },
                )
            }
        }
    }

    fun onLoadProject(project: WatchFaceProject) {
        editorState.update {
            it.copy(
                draft = project,
                feedbackMessage = "Loaded '${project.name}'.",
            )
        }
    }

    fun onDeleteProject(projectId: Long) {
        viewModelScope.launch {
            deleteWatchFaceProject(projectId)
            editorState.update { state ->
                val resetDraft = if (state.draft.id == projectId) {
                    state.draft.copy(id = 0L)
                } else {
                    state.draft
                }
                state.copy(
                    draft = resetDraft,
                    feedbackMessage = "Deleted watch-face project from local library.",
                )
            }
        }
    }

    fun onExportPreviewAndSpec() {
        if (editorState.value.isExporting) return
        val draft = editorState.value.draft
        val nextEvent = uiState.value.nextCalendarEventLabel
        viewModelScope.launch {
            editorState.update { it.copy(isExporting = true, feedbackMessage = null) }
            val result = exportWatchFaceArtifact(
                project = draft,
                nextEventLabel = nextEvent,
                savePngToGallery = true,
            )
            editorState.update { state ->
                when (result) {
                    is WatchFaceExportResult.ExportedPreviewAndSpec -> state.copy(
                        isExporting = false,
                        lastExportArtifact = result.artifact,
                        feedbackMessage = if (result.artifact.savedToGallery) {
                            "Exported 466×466 PNG to Pictures/NothingWear + JSON spec (preview artwork, not a compiled Watch S4 binary)."
                        } else {
                            "Exported 466×466 PNG + JSON spec to app storage (preview artwork, not a compiled Watch S4 binary)."
                        },
                    )

                    is WatchFaceExportResult.Failed -> state.copy(
                        isExporting = false,
                        feedbackMessage = "Export failed: ${result.message}",
                    )
                }
            }
        }
    }

    private data class EditorControls(
        val activeTab: StudioEditorTab = StudioEditorTab.THEMES,
        val draft: WatchFaceProject = WatchFacePresets.defaultFor(WatchFaceCategory.NOTHING),
        val isExporting: Boolean = false,
        val lastExportArtifact: WatchFaceExportArtifact? = null,
        val feedbackMessage: String? = null,
    )
}
