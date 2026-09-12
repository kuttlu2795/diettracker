package com.dietary.tracker.chandra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the phrases explicitly listed in the Chandra spec's test-cases section, plus a few
 * parser edge cases (unknown command, custom alias override). This exercises
 * [ChandraCommandParser] only - it's pure text-in/action-out logic with no Android framework
 * dependency, so it runs as a fast local JVM unit test (no device/emulator needed).
 *
 * Not covered here (they need instrumented/manual testing on a device, since they depend on
 * Android framework behavior this parser doesn't touch): microphone permission denial, Health
 * Connect permission denial, no health data available, phone locked/screen off/app backgrounded,
 * and Always Listening on/off. Those are exercised by ChandraVoiceRepository's "I don't have
 * your..." fallback strings (see its own doc comments) and by ChandraWakeService's documented
 * foreground-service behavior respectively.
 */
class ChandraCommandParserTest {

    private fun parse(text: String) = ChandraCommandParser.parse(text)

    @Test fun wakeWordAlonesYieldNoAction() {
        assertNull(parse("Hi Chandra").actionId)
        assertNull(parse("Chandra").actionId)
    }

    @Test fun heartRateEnglish() {
        assertEquals(ChandraActionId.HEART_RATE, parse("What's my heart rate?").actionId)
    }

    @Test fun heartRateTanglish() {
        assertEquals(ChandraActionId.HEART_RATE, parse("Chandra, heart rate sollu").actionId)
    }

    @Test fun stepsEvlo() {
        assertEquals(ChandraActionId.STEPS, parse("Steps evlo?").actionId)
    }

    @Test fun stepsHowMany() {
        assertEquals(ChandraActionId.STEPS, parse("How many steps today?").actionId)
    }

    @Test fun weight() {
        assertEquals(ChandraActionId.WEIGHT, parse("What's my weight?").actionId)
    }

    @Test fun sleepEvlo() {
        assertEquals(ChandraActionId.SLEEP, parse("Sleep evlo?").actionId)
    }

    @Test fun spo2() {
        assertEquals(ChandraActionId.SPO2, parse("What's my SpO2?").actionId)
    }

    @Test fun bloodPressure() {
        assertEquals(ChandraActionId.BLOOD_PRESSURE, parse("What's my blood pressure?").actionId)
    }

    @Test fun addWaterMl() {
        val result = parse("Add 500 ml water")
        assertEquals(ChandraActionId.ADD_WATER, result.actionId)
        assertEquals(500.0, result.quantity)
    }

    @Test fun addEggsWithQuantity() {
        val result = parse("Add 2 eggs")
        assertEquals(ChandraActionId.ADD_FOOD, result.actionId)
        assertEquals(2.0, result.quantity)
        assertEquals("eggs", result.subject)
    }

    @Test fun addExerciseWithDuration() {
        val result = parse("Add 30 minutes walking")
        assertEquals(ChandraActionId.ADD_EXERCISE, result.actionId)
        assertEquals(30, result.durationMinutes)
    }

    @Test fun startFasting() {
        assertEquals(ChandraActionId.START_FASTING, parse("Start fasting").actionId)
    }

    @Test fun endFasting() {
        assertEquals(ChandraActionId.END_FASTING, parse("End fasting").actionId)
    }

    @Test fun calorieGoalQuery() {
        assertEquals(ChandraActionId.CALORIE_GOAL, parse("What's my calorie goal?").actionId)
    }

    @Test fun calorieGoalSet() {
        val result = parse("Set my calorie goal to 2000")
        assertEquals(ChandraActionId.CALORIE_GOAL, result.actionId)
        assertEquals(2000.0, result.quantity)
    }

    @Test fun openDiary() {
        assertEquals(ChandraActionId.OPEN_DIARY, parse("Open my diary").actionId)
    }

    @Test fun weeklyReport() {
        assertEquals(ChandraActionId.WEEKLY_REPORT, parse("Show weekly report").actionId)
    }

    @Test fun watchSync() {
        assertEquals(ChandraActionId.WATCH_SYNC, parse("Sync my watch").actionId)
    }

    @Test fun unknownCommandYieldsNullAction() {
        assertNull(parse("Tell me a joke about spreadsheets").actionId)
    }

    @Test fun customAliasOverridesToItsMappedAction() {
        val aliases = listOf(
            com.dietary.tracker.data.entities.ChandraCustomCommand(actionId = "HEART_RATE", phrase = "my pulse")
        )
        assertEquals(ChandraActionId.HEART_RATE, ChandraCommandParser.parse("my pulse", aliases).actionId)
    }
}
