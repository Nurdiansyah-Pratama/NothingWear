package com.nothingwear.app.data.watchface

import com.nothingwear.app.core.common.IoDispatcher
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.core.database.dao.WatchFaceDao
import com.nothingwear.app.core.database.entity.WatchFaceProjectEntity
import com.nothingwear.app.domain.model.MiFitnessCustomTemplate
import com.nothingwear.app.domain.model.WatchFaceAssetPackArtifact
import com.nothingwear.app.domain.model.WatchFaceAssetPackResult
import com.nothingwear.app.domain.model.WatchFaceCategory
import com.nothingwear.app.domain.model.WatchFaceComplication
import com.nothingwear.app.domain.model.WatchFaceDialStyle
import com.nothingwear.app.domain.model.WatchFaceExportArtifact
import com.nothingwear.app.domain.model.WatchFaceExportResult
import com.nothingwear.app.domain.model.WatchFaceFontStyle
import com.nothingwear.app.domain.model.WatchFaceProject
import com.nothingwear.app.domain.model.WatchFaceRingStyle
import com.nothingwear.app.domain.repository.WatchFaceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomWatchFaceRepository @Inject constructor(
    private val watchFaceDao: WatchFaceDao,
    private val bitmapRenderer: WatchFaceBitmapRenderer,
    private val timeProvider: TimeProvider,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WatchFaceRepository {

    override fun observeSavedProjects(): Flow<List<WatchFaceProject>> =
        watchFaceDao.observeAllProjects().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveProject(project: WatchFaceProject): Long = withContext(ioDispatcher) {
        val now = timeProvider.nowMillis()
        watchFaceDao.upsertProject(
            project.copy(
                name = project.name.trim().ifBlank { "Untitled Watch Face" },
                updatedAtMillis = now,
            ).toEntity(),
        )
    }

    override suspend fun deleteProject(projectId: Long) = withContext(ioDispatcher) {
        watchFaceDao.deleteProjectById(projectId)
    }

    override suspend fun exportPreviewAndSpec(
        project: WatchFaceProject,
        nextEventLabel: String?,
        savePngToGallery: Boolean,
        backgroundOnly: Boolean,
        miFitnessTemplate: MiFitnessCustomTemplate,
    ): WatchFaceExportResult = withContext(ioDispatcher) {
        try {
            val now = timeProvider.nowMillis()
            val zoneId = timeProvider.zoneId()
            val bitmap = bitmapRenderer.render466Bitmap(
                project = project,
                nowMillis = now,
                zoneId = zoneId,
                nextEventLabel = nextEventLabel,
                backgroundOnly = backgroundOnly,
                miFitnessTemplate = miFitnessTemplate,
            )
            try {
                val (pngPath, jsonPath, gallerySaved) = bitmapRenderer.exportToFiles(
                    project = project,
                    bitmap = bitmap,
                    exportedAtMillis = now,
                    saveToGallery = savePngToGallery,
                    backgroundOnly = backgroundOnly,
                    miFitnessTemplate = miFitnessTemplate,
                )
                WatchFaceExportResult.ExportedPreviewAndSpec(
                    WatchFaceExportArtifact(
                        projectName = project.name,
                        pngFilePath = pngPath,
                        jsonFilePath = jsonPath,
                        savedToGallery = gallerySaved,
                        backgroundOnly = backgroundOnly,
                        miFitnessTemplate = miFitnessTemplate,
                        exportedAtMillis = now,
                    ),
                )
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            WatchFaceExportResult.Failed(e.message ?: "Failed to export 466x466 preview and spec.")
        }
    }

    override suspend fun exportLayerAssetPackZip(
        project: WatchFaceProject,
        nextEventLabel: String?,
        miFitnessTemplate: MiFitnessCustomTemplate,
        saveZipToDownloads: Boolean,
    ): WatchFaceAssetPackResult = withContext(ioDispatcher) {
        try {
            val now = timeProvider.nowMillis()
            val zoneId = timeProvider.zoneId()
            val (zipPath, savedToDownloads, entries) = bitmapRenderer.exportLayerAssetPackZip(
                project = project,
                nowMillis = now,
                zoneId = zoneId,
                nextEventLabel = nextEventLabel,
                miFitnessTemplate = miFitnessTemplate,
                saveToDownloads = saveZipToDownloads,
            )
            WatchFaceAssetPackResult.Exported(
                WatchFaceAssetPackArtifact(
                    projectName = project.name,
                    zipFilePath = zipPath,
                    savedToDownloads = savedToDownloads,
                    includedEntries = entries,
                    miFitnessTemplate = miFitnessTemplate,
                    exportedAtMillis = now,
                ),
            )
        } catch (e: Exception) {
            WatchFaceAssetPackResult.Failed(e.message ?: "Failed to export EasyFace layer asset pack ZIP.")
        }
    }

    private fun WatchFaceProjectEntity.toDomain() = WatchFaceProject(
        id = id,
        name = name,
        category = WatchFaceCategory.entries.firstOrNull { it.name == category } ?: WatchFaceCategory.CUSTOM,
        dialStyle = WatchFaceDialStyle.entries.firstOrNull { it.name == dialStyle } ?: WatchFaceDialStyle.DIGITAL_CENTER,
        ringStyle = WatchFaceRingStyle.entries.firstOrNull { it.name == ringStyle } ?: WatchFaceRingStyle.DOTS_12,
        fontStyle = WatchFaceFontStyle.entries.firstOrNull { it.name == fontStyle } ?: WatchFaceFontStyle.DOT_INDUSTRIAL,
        backgroundColorArgb = backgroundColorArgb,
        accentColorArgb = accentColorArgb,
        textColorArgb = textColorArgb,
        is24Hour = is24Hour,
        showSeconds = showSeconds,
        timeScale = timeScale,
        topComplication = WatchFaceComplication.entries.firstOrNull { it.name == topComplication } ?: WatchFaceComplication.NONE,
        bottomComplication = WatchFaceComplication.entries.firstOrNull { it.name == bottomComplication } ?: WatchFaceComplication.NONE,
        leftComplication = WatchFaceComplication.entries.firstOrNull { it.name == leftComplication } ?: WatchFaceComplication.NONE,
        rightComplication = WatchFaceComplication.entries.firstOrNull { it.name == rightComplication } ?: WatchFaceComplication.NONE,
        updatedAtMillis = updatedAtMillis,
    )

    private fun WatchFaceProject.toEntity() = WatchFaceProjectEntity(
        id = id,
        name = name,
        category = category.name,
        dialStyle = dialStyle.name,
        ringStyle = ringStyle.name,
        fontStyle = fontStyle.name,
        backgroundColorArgb = backgroundColorArgb,
        accentColorArgb = accentColorArgb,
        textColorArgb = textColorArgb,
        is24Hour = is24Hour,
        showSeconds = showSeconds,
        timeScale = timeScale,
        topComplication = topComplication.name,
        bottomComplication = bottomComplication.name,
        leftComplication = leftComplication.name,
        rightComplication = rightComplication.name,
        updatedAtMillis = updatedAtMillis,
    )
}
