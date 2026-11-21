package com.nothingwear.app.domain.usecase

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
    ): WatchFaceExportResult = repository.exportPreviewAndSpec(
        project = project,
        nextEventLabel = nextEventLabel,
        savePngToGallery = savePngToGallery,
    )
}
