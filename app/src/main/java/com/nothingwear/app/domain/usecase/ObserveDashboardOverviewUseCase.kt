package com.nothingwear.app.domain.usecase

import com.nothingwear.app.domain.model.DashboardOverview
import com.nothingwear.app.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDashboardOverviewUseCase @Inject constructor(
    private val repository: DashboardRepository,
) {
    operator fun invoke(): Flow<DashboardOverview> = repository.observeDashboardOverview()
}
