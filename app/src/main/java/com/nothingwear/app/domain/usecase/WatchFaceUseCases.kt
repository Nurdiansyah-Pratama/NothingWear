package com.nothingwear.app.domain.usecase

import com.nothingwear.app.domain.model.MiFitnessCustomTemplate
import com.nothingwear.app.domain.model.WatchFaceAssetPackResult
import com.nothingwear.app.domain.model.WatchFaceExportResult
import com.nothingwear.app.domain.model.WatchFaceProject
import com.nothingwear.app.domain.repository.WatchFaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSavedWatchFacesUseCase @Inject constructor(
    private val repository: WatchFaceRepository,
) {
    operator fun invoke(): Flow<List<WatchFaceProject>> = repository.observeSavedProjects()
}

class SaveWatchFaceProjectUseCase @Inject constructor(
    private val repository: WatchFaceRepository,
) {
    suspend operator fun invoke(project: WatchFaceProject): Long = repository.saveProject(project)
}

class DeleteWatchFaceProjectUseCase @Inject constructor(
    private val repository: WatchFaceRepository,
) {
    suspend operator fun invoke(projectId: Long) = repository.deleteProject(projectId)
}

class ExportWatchFaceArtifactUseCase @Inject constructor(
    private val repository: WatchFaceRepository,
) {
    suspend operator fun invoke(
        project: WatchFaceProject,
        nextEventLabel: String?,
        savePngToGallery: Boolean = true,
        backgroundOnly: Boolean = false,
        miFitnessTemplate: MiFitnessCustomTemplate = MiFitnessCustomTemplate.NONE,
    ): WatchFaceExportResult = repository.exportPreviewAndSpec(
        project = project,
        nextEventLabel = nextEventLabel,
        savePngToGallery = savePngToGallery,
        backgroundOnly = backgroundOnly,
        miFitnessTemplate = miFitnessTemplate,
    )
}

class ExportWatchFaceAssetPackUseCase @Inject constructor(
    private val repository: WatchFaceRepository,
) {
    suspend operator fun invoke(
        project: WatchFaceProject,
        nextEventLabel: String?,
        miFitnessTemplate: MiFitnessCustomTemplate = MiFitnessCustomTemplate.NONE,
        saveZipToDownloads: Boolean = true,
    ): WatchFaceAssetPackResult = repository.exportLayerAssetPackZip(
        project = project,
        nextEventLabel = nextEventLabel,
        miFitnessTemplate = miFitnessTemplate,
        saveZipToDownloads = saveZipToDownloads,
    )
}
