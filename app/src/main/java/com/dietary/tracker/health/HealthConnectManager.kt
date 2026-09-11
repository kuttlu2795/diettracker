package com.dietary.tracker.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId

class HealthConnectManager(context: Context) {
    private val client: HealthConnectClient? = try { HealthConnectClient.getOrCreate(context.applicationContext) } catch (_: Exception) { null }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun requestPermissions(): Set<String>? {
        val c = client ?: return null
        return c.permissionController.requestPermissions(permissions)
    }

    suspend fun todaySteps(): Long {
        val c = client ?: return 0L
        val start = java.time.ZonedDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
        return try {
            c.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(start, Instant.now())))[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (_: Exception) { 0L }
    }

    suspend fun latestWeightKg(): Double? {
        val c = client ?: return null
        return try {
            c.readRecords(ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.before(Instant.now()), pageSize = 20))
                .records.maxByOrNull { it.time }?.weight?.inKilograms
        } catch (_: Exception) { null }
    }

    suspend fun latestHeartRate(): Long? {
        val c = client ?: return null
        return try {
            c.readRecords(ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.before(Instant.now()), pageSize = 20))
                .records.asSequence().flatMap { it.samples.asSequence() }.maxByOrNull { it.time }?.beatsPerMinute
        } catch (_: Exception) { null }
    }

    suspend fun latestBloodPressure(): Pair<Double, Double>? {
        val c = client ?: return null
        return try {
            c.readRecords(ReadRecordsRequest(BloodPressureRecord::class, TimeRangeFilter.before(Instant.now()), pageSize = 20))
                .records.maxByOrNull { it.time }?.let {
                    it.systolic.inMillimetersOfMercury to it.diastolic.inMillimetersOfMercury
                }
        } catch (_: Exception) { null }
    }

    suspend fun latestOxygenSaturation(): Double? {
        val c = client ?: return null
        return try {
            c.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.before(Instant.now()), pageSize = 20))
                .records.maxByOrNull { it.time }?.percentage?.value
        } catch (_: Exception) { null }
    }

    suspend fun latestSleepHours(): Double? {
        val c = client ?: return null
        return try {
            c.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.before(Instant.now()), pageSize = 20))
                .records.maxByOrNull { it.endTime }?.let { (it.endTime.toEpochMilli() - it.startTime.toEpochMilli()) / 3_600_000.0 }
        } catch (_: Exception) { null }
    }
}
