package com.dietary.tracker.wearable

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class HealthConnectAvailability { AVAILABLE, NOT_INSTALLED, NOT_SUPPORTED }

data class WearableSyncResult(val steps: Long?, val weightKg: Float?, val activeCalories: Double?)

/**
 * Wraps Android's Health Connect (the OS-level health data hub). This app only *reads* from it -
 * any wearable's own companion app (boAt Wearables/ProGear, Fitbit, Galaxy Wearable, Mi Fitness,
 * etc.) is what actually writes steps/weight/heart rate/sleep/SpO2/blood pressure into Health
 * Connect. If a given watch's app doesn't write a particular record type, the read functions below
 * return null - callers (including Chandra) must say "data not available" rather than invent a
 * value. This is the single source of truth used by both the Wearable Sync screen and Chandra, so
 * both always show the same real numbers.
 */
class HealthConnectManager(private val context: Context) {

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class)
    )

    fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NOT_INSTALLED
        else -> HealthConnectAvailability.NOT_SUPPORTED
    }

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    /** Build this once in the composable with rememberLauncherForActivityResult(...). */
    fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean = try {
        client.permissionController.getGrantedPermissions().containsAll(permissions)
    } catch (e: Exception) {
        false
    }

    /** Reads today's steps, the latest logged weight, and today's active calories in one go. */
    suspend fun syncNow(): WearableSyncResult {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val now = Instant.now()

        val steps = try {
            client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRangeFilter = TimeRangeFilter.between(startOfDay, now)))
                .records.sumOf { it.count }
        } catch (e: Exception) { null }

        val weight = try {
            client.readRecords(ReadRecordsRequest(WeightRecord::class, timeRangeFilter = TimeRangeFilter.before(now)))
                .records.maxByOrNull { it.time }?.weight?.inKilograms?.toFloat()
        } catch (e: Exception) { null }

        val activeCalories = try {
            client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRangeFilter = TimeRangeFilter.between(startOfDay, now)))
                .records.sumOf { it.energy.inKilocalories }
        } catch (e: Exception) { null }

        return WearableSyncResult(steps, weight, activeCalories)
    }

    /** Latest recorded heart rate sample (bpm), or null if none exists / not available. */
    suspend fun readLatestHeartRateBpm(): Long? = try {
        val now = Instant.now()
        val since = now.minusSeconds(60L * 60L * 24L * 7L) // look back up to 7 days for the latest sample
        val records = client.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(since, now))
        ).records
        records.maxByOrNull { it.endTime }?.samples?.maxByOrNull { it.time }?.beatsPerMinute
    } catch (e: Exception) { null }

    /** Total sleep duration for the most recent sleep session, in hours (rounded to 1 decimal). */
    suspend fun readLatestSleepHours(): Double? = try {
        val now = Instant.now()
        val since = now.minusSeconds(60L * 60L * 24L * 2L) // last 2 days covers "last night"
        val records = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = TimeRangeFilter.between(since, now))
        ).records
        val latest = records.maxByOrNull { it.endTime } ?: return null
        val minutes = java.time.Duration.between(latest.startTime, latest.endTime).toMinutes()
        (minutes / 60.0 * 10.0).let { Math.round(it) / 10.0 }
    } catch (e: Exception) { null }

    /** Latest blood-oxygen saturation percentage, or null. */
    suspend fun readLatestSpO2(): Double? = try {
        val now = Instant.now()
        val since = now.minusSeconds(60L * 60L * 24L * 3L)
        val records = client.readRecords(
            ReadRecordsRequest(OxygenSaturationRecord::class, timeRangeFilter = TimeRangeFilter.between(since, now))
        ).records
        records.maxByOrNull { it.time }?.percentage?.value
    } catch (e: Exception) { null }

    data class BloodPressureReading(val systolic: Double, val diastolic: Double)

    /** Latest blood pressure reading, or null - most consumer wearables do NOT write this. */
    suspend fun readLatestBloodPressure(): BloodPressureReading? = try {
        val now = Instant.now()
        val since = now.minusSeconds(60L * 60L * 24L * 30L)
        val records = client.readRecords(
            ReadRecordsRequest(BloodPressureRecord::class, timeRangeFilter = TimeRangeFilter.between(since, now))
        ).records
        records.maxByOrNull { it.time }?.let {
            BloodPressureReading(it.systolic.inMillimetersOfMercury, it.diastolic.inMillimetersOfMercury)
        }
    } catch (e: Exception) { null }
}
