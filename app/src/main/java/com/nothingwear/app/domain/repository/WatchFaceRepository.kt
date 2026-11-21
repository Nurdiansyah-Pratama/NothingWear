package com.nothingwear.app.domain.repository

import com.nothingwear.app.domain.model.MiFitnessCustomTemplate
import com.nothingwear.app.domain.model.WatchFaceAssetPackResult
import com.nothingwear.app.domain.model.WatchFaceExportResult
import com.nothingwear.app.domain.model.WatchFaceProject
import kotlinx.coroutines.flow.Flow

interface WatchFaceRepository {
    fun observeSavedProjects(): Flow<List<WatchFaceProject>>

    suspend fun saveProject(project: WatchFaceProject): Long

    suspend fun deleteProject(projectId: Long)

    suspend fun exportPreviewAndSpec(
        project: WatchFaceProject,
        nextEventLabel: String?,
        savePngToGallery: Boolean,
        backgroundOnly: Boolean = false,
        miFitnessTemplate: MiFitnessCustomTemplate = MiFitnessCustomTemplate.NONE,
    ): WatchFaceExportResult

    suspend fun exportLayerAssetPackZip(
        project: WatchFaceProject,
        nextEventLabel: String?,
        miFitnessTemplate: MiFitnessCustomTemplate = MiFitnessCustomTemplate.NONE,
        saveZipToDownloads: Boolean = true,
    ): WatchFaceAssetPackResult = WatchFaceAssetPackResult.Failed("Asset pack export unsupported in stub.")
}
