package com.nothingwear.app.domain.usecase

import com.nothingwear.app.domain.model.HealthDashboardSnapshot
import com.nothingwear.app.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveHealthSnapshotUseCase @Inject constructor(
    private val repository: HealthRepository,
) {
    operator fun invoke(): Flow<HealthDashboardSnapshot> = repository.observeHealthSnapshot()
}

class RefreshHealthDataUseCase @Inject constructor(
    private val repository: HealthRepository,
) {
    suspend operator fun invoke() = repository.refreshHealthData()
}

class GetRequiredHealthPermissionsUseCase @Inject constructor(
    private val repository: HealthRepository,
) {
    operator fun invoke(): Set<String> = repository.requiredHealthConnectPermissions()
}
