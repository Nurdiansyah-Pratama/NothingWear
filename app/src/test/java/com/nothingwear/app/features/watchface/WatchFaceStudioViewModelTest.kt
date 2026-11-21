package com.nothingwear.app.features.watchface

import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.domain.model.CalendarEvent
import com.nothingwear.app.domain.model.CalendarSource
import com.nothingwear.app.domain.model.CalendarSyncMetadata
import com.nothingwear.app.domain.model.CalendarSyncResult
import com.nothingwear.app.domain.model.MiFitnessCustomTemplate
import com.nothingwear.app.domain.model.WatchFaceAssetPackArtifact
import com.nothingwear.app.domain.model.WatchFaceAssetPackResult
import com.nothingwear.app.domain.model.WatchFaceCategory
import com.nothingwear.app.domain.model.WatchFaceDialStyle
import com.nothingwear.app.domain.model.WatchFaceExportArtifact
import com.nothingwear.app.domain.model.WatchFaceExportResult
import com.nothingwear.app.domain.model.WatchFaceProject
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.repository.WatchFaceRepository
import com.nothingwear.app.domain.usecase.DeleteWatchFaceProjectUseCase
import com.nothingwear.app.domain.usecase.ExportWatchFaceArtifactUseCase
import com.nothingwear.app.domain.usecase.ExportWatchFaceAssetPackUseCase
import com.nothingwear.app.domain.usecase.ObserveSavedWatchFacesUseCase
import com.nothingwear.app.domain.usecase.SaveWatchFaceProjectUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class WatchFaceStudioViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedTime = object : TimeProvider {
        override fun nowMillis(): Long = 1_763_690_000_000L
        override fun zoneId(): ZoneId = ZoneId.of("Asia/Jakarta")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun switchesPresetsCustomizesSavesLoadsAndExports466Preview() = runTest {
        val fakeWatchFaceRepo = FakeWatchFaceRepository()
        val fakeCalendarRepo = object : CalendarRepository {
            override fun observeCalendars(): Flow<List<CalendarSource>> = flowOf(emptyList())
            override fun observeSelectedEvents(): Flow<List<CalendarEvent>> = flowOf(
                listOf(
                    CalendarEvent(
                        instanceKey = "e1",
                        eventId = 1L,
                        calendarId = 1L,
                        calendarName = "Google",
                        calendarColor = 0xFFD71921.toInt(),
                        title = "Standup Sync",
                        description = null,
                        location = null,
                        startTimeMillis = 1_763_693_600_000L,
                        endTimeMillis = 1_763_697_200_000L,
                        isAllDay = false,
                        timezone = "Asia/Jakarta",
                        lastSyncedAtMillis = 1_763_690_000_000L,
                    ),
                ),
            )
            override fun observeEventDetail(instanceKey: String): Flow<CalendarEvent?> = flowOf(null)
            override fun observeSyncMetadata(): Flow<CalendarSyncMetadata> = flowOf(CalendarSyncMetadata())
            override suspend fun syncFromDevice(): CalendarSyncResult = CalendarSyncResult.PermissionDenied
            override suspend fun setCalendarSelected(calendarId: Long, selected: Boolean) = Unit
        }

        val viewModel = WatchFaceStudioViewModel(
            observeSavedWatchFaces = ObserveSavedWatchFacesUseCase(fakeWatchFaceRepo),
            calendarRepository = fakeCalendarRepo,
            saveWatchFaceProject = SaveWatchFaceProjectUseCase(fakeWatchFaceRepo),
            deleteWatchFaceProject = DeleteWatchFaceProjectUseCase(fakeWatchFaceRepo),
            exportWatchFaceArtifact = ExportWatchFaceArtifactUseCase(fakeWatchFaceRepo),
            exportWatchFaceAssetPack = ExportWatchFaceAssetPackUseCase(fakeWatchFaceRepo),
            timeProvider = fixedTime,
        )

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals("Standup Sync", viewModel.uiState.value.nextCalendarEventLabel)

        viewModel.onSelectCategoryPreset(WatchFaceCategory.CYBERPUNK)
        viewModel.onProjectNameChanged("My Watch S4 Face")
        viewModel.onDialStyleSelected(WatchFaceDialStyle.HYBRID_DUAL)
        viewModel.onSaveProject(asNewCopy = false)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.savedProjects.size)
        assertEquals("My Watch S4 Face", viewModel.uiState.value.savedProjects.first().name)
        assertEquals(WatchFaceDialStyle.HYBRID_DUAL, viewModel.uiState.value.draft.dialStyle)

        viewModel.onExportPreviewAndSpec()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.lastExportArtifact)
        assertEquals(466, viewModel.uiState.value.lastExportArtifact?.widthPx)
        assertEquals(false, viewModel.uiState.value.lastExportArtifact?.backgroundOnly)
        assertTrue(viewModel.uiState.value.feedbackMessage?.contains("466×466") == true)

        viewModel.onSelectMiFitnessTemplate(MiFitnessCustomTemplate.CUSTOM_ANALOG)
        viewModel.onToggleSafeZoneGuides(false)
        viewModel.onExportBackgroundOnly()
        advanceUntilIdle()

        assertEquals(MiFitnessCustomTemplate.CUSTOM_ANALOG, viewModel.uiState.value.miFitnessTemplate)
        assertEquals(false, viewModel.uiState.value.showSafeZoneGuides)
        assertEquals(true, viewModel.uiState.value.lastExportArtifact?.backgroundOnly)
        assertEquals(
            MiFitnessCustomTemplate.CUSTOM_ANALOG,
            viewModel.uiState.value.lastExportArtifact?.miFitnessTemplate,
        )
        assertTrue(viewModel.uiState.value.feedbackMessage?.contains("Custom analog") == true)

        viewModel.onExportLayerAssetPack()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.lastAssetPackArtifact)
        assertEquals(6, viewModel.uiState.value.lastAssetPackArtifact?.includedEntries?.size)
        assertTrue(viewModel.uiState.value.feedbackMessage?.contains("Download/NothingWear") == true)

        job.cancel()
    }

    private class FakeWatchFaceRepository : WatchFaceRepository {
        private val projects = MutableStateFlow<List<WatchFaceProject>>(emptyList())
        private var nextId = 1L

        override fun observeSavedProjects(): Flow<List<WatchFaceProject>> = projects

        override suspend fun saveProject(project: WatchFaceProject): Long {
            val id = if (project.id > 0L) project.id else nextId++
            val stored = project.copy(id = id)
            projects.value = projects.value.filterNot { it.id == id } + stored
            return id
        }

        override suspend fun deleteProject(projectId: Long) {
            projects.value = projects.value.filterNot { it.id == projectId }
        }

        override suspend fun exportPreviewAndSpec(
            project: WatchFaceProject,
            nextEventLabel: String?,
            savePngToGallery: Boolean,
            backgroundOnly: Boolean,
            miFitnessTemplate: MiFitnessCustomTemplate,
        ): WatchFaceExportResult = WatchFaceExportResult.ExportedPreviewAndSpec(
            WatchFaceExportArtifact(
                projectName = project.name,
                pngFilePath = "/tmp/test.png",
                jsonFilePath = "/tmp/test.json",
                savedToGallery = savePngToGallery,
                backgroundOnly = backgroundOnly,
                miFitnessTemplate = miFitnessTemplate,
                exportedAtMillis = 1_763_690_000_000L,
            ),
        )

        override suspend fun exportLayerAssetPackZip(
            project: WatchFaceProject,
            nextEventLabel: String?,
            miFitnessTemplate: MiFitnessCustomTemplate,
            saveZipToDownloads: Boolean,
        ): WatchFaceAssetPackResult = WatchFaceAssetPackResult.Exported(
            WatchFaceAssetPackArtifact(
                projectName = project.name,
                zipFilePath = "/tmp/test_easyface_pack.zip",
                savedToDownloads = saveZipToDownloads,
                includedEntries = listOf(
                    "01_background.png",
                    "02_bezel_ring.png",
                    "03_nothing_motif.png",
                    "04_preview_reference.png",
                    "spec.json",
                    "README_EASYFACE.txt",
                ),
                miFitnessTemplate = miFitnessTemplate,
                exportedAtMillis = 1_763_690_000_000L,
            ),
        )
    }
}
