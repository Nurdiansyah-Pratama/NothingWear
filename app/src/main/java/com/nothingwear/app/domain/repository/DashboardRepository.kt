package com.nothingwear.app.domain.repository

import com.nothingwear.app.domain.model.DashboardOverview
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeDashboardOverview(): Flow<DashboardOverview>
}
