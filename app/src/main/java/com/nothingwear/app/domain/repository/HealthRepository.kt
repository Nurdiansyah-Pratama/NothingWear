package com.nothingwear.app.domain.repository

import com.nothingwear.app.domain.model.HealthDashboardSnapshot
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    fun observeHealthSnapshot(): Flow<HealthDashboardSnapshot>

    fun requiredHealthConnectPermissions(): Set<String>

    suspend fun refreshHealthData()
}
