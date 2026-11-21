package com.nothingwear.app.data.health

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.nothingwear.app.core.common.IoDispatcher
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.domain.model.HealthConnectAvailability
import com.nothingwear.app.domain.model.HealthDashboardSnapshot
import com.nothingwear.app.domain.model.HealthMetricSummary
import com.nothingwear.app.domain.model.HostPhoneEnvironment
import com.nothingwear.app.domain.model.LastVerifiedHeartRate
import com.nothingwear.app.domain.model.MetricAvailabilityStatus
import com.nothingwear.app.domain.repository.HealthRepository
import com.nothingwear.app.widget.NothingWearWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * M6 Read-only Android Health Connect + Host Companion Environment repository.
 *
 * Strictly adheres to NothingWear's honesty policy:
 * - Reads only verified records from Android Health Connect (Steps, Heart Rate, Sleep).
 * - Never fabricates health metrics or Watch S4 battery levels.
 * - Attributes every metric to its verified source (e.g., Health Connect origin package)
 *   or reports an explicit PERMISSION_REQUIRED / NO_DATA / UNAVAILABLE state.
 */
@Singleton
class AndroidHealthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val timeProvider: TimeProvider,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : HealthRepository {

    private val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    private val _snapshot = MutableStateFlow(
        HealthDashboardSnapshot(
            requiredPermissions = requiredPermissions,
            hostPhone = readHostPhoneEnvironment(),
        ),
    )

    private val lastVerifiedHrFlow: Flow<LastVerifiedHeartRate?> = dataStore.data.map { prefs ->
        val bpm = prefs[KEY_LAST_HR_BPM]
        val ts = prefs[KEY_LAST_HR_TIMESTAMP]
        val src = prefs[KEY_LAST_HR_SOURCE]
        if (bpm != null && bpm in 1..300 && ts != null && ts > 0L && !src.isNullOrBlank()) {
            LastVerifiedHeartRate(bpm = bpm, timestampMillis = ts, sourceLabel = src)
        } else {
            null
        }
    }

    override fun observeHealthSnapshot(): Flow<HealthDashboardSnapshot> = combine(
        _snapshot,
        lastVerifiedHrFlow,
    ) { snapshot, cachedHr ->
        snapshot.copy(lastVerifiedHeartRate = cachedHr)
    }

    override fun requiredHealthConnectPermissions(): Set<String> = requiredPermissions

    override suspend fun recordVerifiedBleHeartRate(bpm: Int, timestampMillis: Long) {
        if (bpm !in 1..300 || timestampMillis <= 0L) return
        persistLastVerifiedHeartRate(
            bpm = bpm,
            timestampMillis = timestampMillis,
            sourceLabel = "Watch S4 Live BLE (0x2A37)",
        )
        runCatching { NothingWearWidgetProvider.requestSyncBroadcast(context) }
    }

    private suspend fun persistLastVerifiedHeartRate(
        bpm: Int,
        timestampMillis: Long,
        sourceLabel: String,
    ) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            val existingTs = prefs[KEY_LAST_HR_TIMESTAMP] ?: 0L
            if (timestampMillis >= existingTs) {
                prefs[KEY_LAST_HR_BPM] = bpm
                prefs[KEY_LAST_HR_TIMESTAMP] = timestampMillis
                prefs[KEY_LAST_HR_SOURCE] = sourceLabel
            }
        }
    }

    override suspend fun refreshHealthData() = withContext(ioDispatcher) {
        val nowMillis = timeProvider.nowMillis()
        val hostEnv = readHostPhoneEnvironment()
        val sdkStatus = runCatching { HealthConnectClient.getSdkStatus(context) }
            .getOrDefault(HealthConnectClient.SDK_UNAVAILABLE)

        val availability = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.UPDATE_OR_INSTALL_REQUIRED
            else -> HealthConnectAvailability.UNSUPPORTED
        }

        if (availability != HealthConnectAvailability.AVAILABLE) {
            val unavailableReason = when (availability) {
                HealthConnectAvailability.UPDATE_OR_INSTALL_REQUIRED ->
                    "Health Connect needs installation or update from Google Play to read synced wearable metrics."
                else ->
                    "Health Connect is not available on this Android build."
            }
            _snapshot.value = HealthDashboardSnapshot(
                healthConnectAvailability = availability,
                hasHealthPermissions = false,
                grantedPermissions = emptySet(),
                requiredPermissions = requiredPermissions,
                stepsToday = unavailableMetric("Steps Today", "steps", unavailableReason),
                heartRateSummary = unavailableMetric("Heart Rate", "BPM", unavailableReason),
                sleepLast24h = unavailableMetric("Sleep (24h)", "", unavailableReason),
                dataOrigins = emptyList(),
                lastRefreshedMillis = nowMillis,
                hostPhone = hostEnv,
                errorMessage = null,
            )
            return@withContext
        }

        try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            val hasStepsPerm = HealthPermission.getReadPermission(StepsRecord::class) in granted
            val hasHrPerm = HealthPermission.getReadPermission(HeartRateRecord::class) in granted
            val hasSleepPerm = HealthPermission.getReadPermission(SleepSessionRecord::class) in granted
            val hasAll = requiredPermissions.all { it in granted }

            val zoneId = timeProvider.zoneId()
            val nowInstant = Instant.ofEpochMilli(nowMillis)
            val startOfDay = nowInstant.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
            val sleepLookbackStart = nowInstant.minus(30, ChronoUnit.HOURS)

            val origins = linkedSetOf<String>()

            val stepsMetric = if (hasStepsPerm) {
                readTodaySteps(client, startOfDay, nowInstant, nowMillis, origins)
            } else {
                permissionNeededMetric(
                    title = "Steps Today",
                    unit = "steps",
                    note = "Grant Health Connect Steps permission to read Mi Fitness / wearable step counts.",
                )
            }

            val hrMetric = if (hasHrPerm) {
                readTodayHeartRate(client, startOfDay, nowInstant, origins)
            } else {
                permissionNeededMetric(
                    title = "Heart Rate",
                    unit = "BPM",
                    note = "Grant Health Connect Heart Rate permission or use live standard BLE HR (0x2A37) below.",
                )
            }

            val sleepMetric = if (hasSleepPerm) {
                readRecentSleep(client, sleepLookbackStart, nowInstant, origins)
            } else {
                permissionNeededMetric(
                    title = "Sleep (24h)",
                    unit = "",
                    note = "Grant Health Connect Sleep permission to read synced sleep duration.",
                )
            }

            _snapshot.value = HealthDashboardSnapshot(
                healthConnectAvailability = availability,
                hasHealthPermissions = hasAll,
                grantedPermissions = granted.intersect(requiredPermissions),
                requiredPermissions = requiredPermissions,
                stepsToday = stepsMetric,
                heartRateSummary = hrMetric,
                sleepLast24h = sleepMetric,
                dataOrigins = origins.toList().sorted(),
                lastRefreshedMillis = nowMillis,
                hostPhone = hostEnv,
                errorMessage = null,
            )
        } catch (e: Exception) {
            _snapshot.value = _snapshot.value.copy(
                healthConnectAvailability = availability,
                lastRefreshedMillis = nowMillis,
                hostPhone = hostEnv,
                errorMessage = e.message ?: "Failed to read Health Connect data.",
            )
        }
    }

    private suspend fun readTodaySteps(
        client: HealthConnectClient,
        startOfDay: Instant,
        now: Instant,
        nowMillis: Long,
        origins: MutableSet<String>,
    ): HealthMetricSummary {
        val aggregate = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
            ),
        )
        val totalSteps = aggregate[StepsRecord.COUNT_TOTAL]
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
            ),
        ).records
        records.forEach { origins.add(it.metadata.dataOrigin.packageName) }

        return if (totalSteps != null && totalSteps > 0L) {
            val originNote = records.lastOrNull()?.metadata?.dataOrigin?.packageName
                ?.let { "Verified via Health Connect ($it)" }
                ?: "Verified via Health Connect"
            HealthMetricSummary(
                title = "Steps Today",
                primaryValue = String.format(Locale.US, "%,d", totalSteps),
                unit = "steps",
                status = MetricAvailabilityStatus.VERIFIED,
                sourceLabel = "Health Connect",
                detailNote = originNote,
                updatedAtMillis = records.maxOfOrNull { it.endTime.toEpochMilli() } ?: nowMillis,
            )
        } else {
            HealthMetricSummary(
                title = "Steps Today",
                primaryValue = "--",
                unit = "steps",
                status = MetricAvailabilityStatus.NO_DATA,
                sourceLabel = "Health Connect",
                detailNote = "No steps synced to Health Connect today. Enable Health Connect sync in Mi Fitness → Profile → Third-party data.",
            )
        }
    }

    private suspend fun readTodayHeartRate(
        client: HealthConnectClient,
        startOfDay: Instant,
        now: Instant,
        origins: MutableSet<String>,
    ): HealthMetricSummary {
        // Query at least the last 24h so overnight / recent background Watch S4 readings stay visible
        val lookbackStart = minOf(startOfDay, now.minus(24, ChronoUnit.HOURS))
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(lookbackStart, now),
            ),
        ).records
        records.forEach { origins.add(it.metadata.dataOrigin.packageName) }

        val allSamples = records.flatMap { record ->
            record.samples.map { sample -> sample to record.metadata.dataOrigin.packageName }
        }
        val latestPair = allSamples.maxByOrNull { it.first.time }

        return if (latestPair != null) {
            val latestSample = latestPair.first
            val originPkg = latestPair.second
            val minBpm = allSamples.minOf { it.first.beatsPerMinute }
            val maxBpm = allSamples.maxOf { it.first.beatsPerMinute }
            val originLabel = formatOriginDisplayName(originPkg)
            val tsMillis = latestSample.time.toEpochMilli()

            persistLastVerifiedHeartRate(
                bpm = latestSample.beatsPerMinute.toInt(),
                timestampMillis = tsMillis,
                sourceLabel = "$originLabel via Health Connect",
            )

            HealthMetricSummary(
                title = "Heart Rate",
                primaryValue = latestSample.beatsPerMinute.toString(),
                unit = "BPM",
                status = MetricAvailabilityStatus.VERIFIED,
                sourceLabel = "$originLabel (Health Connect)",
                detailNote = "24h range: $minBpm–$maxBpm BPM (${allSamples.size} samples) · $originPkg",
                updatedAtMillis = tsMillis,
            )
        } else {
            HealthMetricSummary(
                title = "Heart Rate",
                primaryValue = "--",
                unit = "BPM",
                status = MetricAvailabilityStatus.NO_DATA,
                sourceLabel = "Health Connect",
                detailNote = "No heart rate records synced in Health Connect (last 24h). Enable Health Connect sync in Mi Fitness → Profile → Third-party data.",
            )
        }
    }

    private fun formatOriginDisplayName(packageName: String): String = when {
        packageName.contains("xiaomi.wearable", ignoreCase = true) ||
            packageName.contains("mi.health", ignoreCase = true) -> "Mi Fitness"
        packageName.contains("google.android.apps.fitness", ignoreCase = true) -> "Google Fit"
        else -> packageName.substringAfterLast('.')
    }

    private suspend fun readRecentSleep(
        client: HealthConnectClient,
        startTime: Instant,
        now: Instant,
        origins: MutableSet<String>,
    ): HealthMetricSummary {
        val sessions = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, now),
            ),
        ).records
        sessions.forEach { origins.add(it.metadata.dataOrigin.packageName) }

        val totalMinutes = sessions.sumOf { session ->
            Duration.between(session.startTime, session.endTime).toMinutes().coerceAtLeast(0L)
        }

        return if (sessions.isNotEmpty() && totalMinutes > 0L) {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            val originPkg = sessions.lastOrNull()?.metadata?.dataOrigin?.packageName
            val originSuffix = originPkg?.let { " · $it" } ?: ""
            HealthMetricSummary(
                title = "Sleep (Recent)",
                primaryValue = "${hours}h ${mins}m",
                unit = "",
                status = MetricAvailabilityStatus.VERIFIED,
                sourceLabel = "Health Connect",
                detailNote = "${sessions.size} recorded session(s) in last 30h$originSuffix",
                updatedAtMillis = sessions.maxOfOrNull { it.endTime.toEpochMilli() },
            )
        } else {
            HealthMetricSummary(
                title = "Sleep (Recent)",
                primaryValue = "--",
                unit = "",
                status = MetricAvailabilityStatus.NO_DATA,
                sourceLabel = "Health Connect",
                detailNote = "No sleep sessions synced to Health Connect in the last 30h.",
            )
        }
    }

    private fun readHostPhoneEnvironment(): HostPhoneEnvironment {
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) ((level * 100) / scale).coerceIn(0, 100) else null
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val model = listOfNotNull(
            Build.MANUFACTURER?.takeIf { it.isNotBlank() },
            Build.MODEL?.takeIf { it.isNotBlank() },
        ).distinct().joinToString(" ").ifBlank { "Android Phone" }

        return HostPhoneEnvironment(
            deviceModel = model,
            androidRelease = "Android ${Build.VERSION.RELEASE}",
            sdkInt = Build.VERSION.SDK_INT,
            batteryPercent = percent,
            isCharging = isCharging,
        )
    }

    private fun permissionNeededMetric(title: String, unit: String, note: String) = HealthMetricSummary(
        title = title,
        primaryValue = "--",
        unit = unit,
        status = MetricAvailabilityStatus.PERMISSION_REQUIRED,
        sourceLabel = "Health Connect",
        detailNote = note,
    )

    private fun unavailableMetric(title: String, unit: String, note: String) = HealthMetricSummary(
        title = title,
        primaryValue = "--",
        unit = unit,
        status = MetricAvailabilityStatus.UNAVAILABLE,
        sourceLabel = "Health Connect",
        detailNote = note,
    )

    private companion object {
        val KEY_LAST_HR_BPM = intPreferencesKey("last_verified_hr_bpm")
        val KEY_LAST_HR_TIMESTAMP = longPreferencesKey("last_verified_hr_timestamp")
        val KEY_LAST_HR_SOURCE = stringPreferencesKey("last_verified_hr_source")
    }
}
