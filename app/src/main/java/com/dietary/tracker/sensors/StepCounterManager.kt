package com.dietary.tracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.dietary.tracker.data.Repository
import com.dietary.tracker.data.entities.StepEntry
import com.dietary.tracker.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Reads the device's built-in cumulative TYPE_STEP_COUNTER sensor (steps since last
 * reboot) and converts it into "steps today" by remembering the sensor value that
 * was current at the start of the day.
 */
class StepCounterManager(
    private val context: Context,
    private val repository: Repository,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun start() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun isAvailable(): Boolean = stepSensor != null

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val cumulative = event.values[0].toInt()
        val today = DateUtils.todayKey()

        scope.launch {
            val existing = repository.getStepsForDate(today)
            if (existing == null) {
                // First reading of the day: this cumulative value becomes our baseline (0 steps today)
                repository.upsertSteps(StepEntry(date = today, steps = 0, baseStepsAtBoot = cumulative))
            } else {
                val stepsToday = (cumulative - existing.baseStepsAtBoot).coerceAtLeast(0)
                if (stepsToday != existing.steps) {
                    repository.upsertSteps(existing.copy(steps = stepsToday))
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
