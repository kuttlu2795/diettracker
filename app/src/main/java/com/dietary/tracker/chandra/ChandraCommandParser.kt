package com.dietary.tracker.chandra

import com.dietary.tracker.data.entities.ChandraCustomCommand

/**
 * Turns a raw utterance (typed or transcribed) into a [ChandraParsedCommand]. This is a
 * deliberately rule-based, on-device matcher - substring/regex keyword matching, not a trained
 * NLP/intent-classification model. It's scoped to the phrases this app actually documents (see
 * the "Chandra Voice Commands" screen) plus common variations, including romanized Tamil
 * ("Tanglish") fragments from the spec (e.g. "evlo", "sollu", "pannu", "enna", "ennaoda").
 *
 * Nothing here calls an external NLP service - all matching happens in this process, so no
 * command text (which can include health data like "heart rate") leaves the device for parsing.
 */
object ChandraCommandParser {

    private val WAKE_PREFIX = Regex("^(hi\\s+|hey\\s+)?chandra[,!.]?\\s*")

    private val SET_GOAL_PATTERN =
        Regex("(set|change)\\s+my\\s+(calorie|calories|protein|water|weight)\\s+goal\\s+to\\s+([0-9]+(?:\\.[0-9]+)?)\\s*(litre|liter|litres|liters|l|g|grams|kg|kcal)?")

    private val ADD_WATER_PATTERN =
        Regex("(add|pannu)[^0-9]*([0-9]+)\\s*(ml|millilitre|milliliter)")
    private val ADD_WATER_GLASS_PATTERN = Regex("(add|pannu).*(glass|cup)\\s+of\\s+water|(glass|cup).*water.*(add|pannu)")

    private val ADD_EXERCISE_PATTERN =
        Regex("([0-9]+)\\s*(minute|minutes|min|mins)[^a-z]*(walking|walk|running|run|jogging|cycling|biking|gym|weights|yoga|swimming|hiit|dancing|sports|[a-z]+)")

    private val ATE_PATTERN = Regex("i\\s+(ate|had)\\s+(.+)")

    private val NUMBER_WORDS = mapOf("a" to 1.0, "an" to 1.0, "one" to 1.0, "two" to 2.0, "three" to 3.0)

    fun parse(rawText: String, customCommands: List<ChandraCustomCommand> = emptyList()): ChandraParsedCommand {
        val normalized = normalize(rawText)
        val text = normalized.replace(WAKE_PREFIX, "").trim()
        if (text.isBlank()) return ChandraParsedCommand(null, rawText)

        // 1. Explicit "set/change my X goal to N" - checked first so it never falls through to
        //    a plain status query.
        SET_GOAL_PATTERN.find(text)?.let { m ->
            val metric = m.groupValues[2]
            var value = m.groupValues[3].toDoubleOrNull()
            val unit = m.groupValues[4]
            if (metric == "water" && (unit == "litre" || unit == "liter" || unit == "litres" || unit == "liters" || unit == "l")) {
                value = value?.times(1000)
            }
            val action = when {
                metric.startsWith("calorie") -> ChandraActionId.CALORIE_GOAL
                metric == "protein" -> ChandraActionId.PROTEIN_GOAL
                metric == "water" -> ChandraActionId.WATER_GOAL
                metric == "weight" -> ChandraActionId.WEIGHT_GOAL
                else -> null
            }
            return ChandraParsedCommand(action, rawText, quantity = value)
        }

        // 2. Goal *queries* ("what's my calorie goal") - must be checked before the plain
        //    calories/protein/water status keywords below.
        if (text.contains("goal")) {
            when {
                text.contains("calorie") -> return ChandraParsedCommand(ChandraActionId.CALORIE_GOAL, rawText)
                text.contains("protein") -> return ChandraParsedCommand(ChandraActionId.PROTEIN_GOAL, rawText)
                text.contains("water") -> return ChandraParsedCommand(ChandraActionId.WATER_GOAL, rawText)
                text.contains("weight") -> return ChandraParsedCommand(ChandraActionId.WEIGHT_GOAL, rawText)
            }
        }

        // 3. Add water (needs an explicit add-trigger + quantity/glass, so plain "how much water
        //    did I drink" doesn't get misread as an add command).
        ADD_WATER_PATTERN.find(text)?.let { m ->
            return ChandraParsedCommand(ChandraActionId.ADD_WATER, rawText, quantity = m.groupValues[2].toDoubleOrNull())
        }
        if (ADD_WATER_GLASS_PATTERN.containsMatchIn(text)) {
            return ChandraParsedCommand(ChandraActionId.ADD_WATER, rawText, quantity = 250.0)
        }

        // 4. Add exercise ("30 minutes walking", "45 mins gym add pannu").
        ADD_EXERCISE_PATTERN.find(text)?.let { m ->
            val minutes = m.groupValues[1].toIntOrNull()
            val exercise = m.groupValues[3].removeSuffix(" add").removeSuffix(" pannu").trim()
            return ChandraParsedCommand(ChandraActionId.ADD_EXERCISE, rawText, durationMinutes = minutes, subject = exercise)
        }

        // 5. Fasting.
        if (text.contains("fasting") || text.contains("fast")) {
            return when {
                text.contains("start") -> ChandraParsedCommand(ChandraActionId.START_FASTING, rawText)
                text.contains("stop") || text.contains("end") -> ChandraParsedCommand(ChandraActionId.END_FASTING, rawText)
                else -> ChandraParsedCommand(ChandraActionId.FASTING_STATUS, rawText)
            }
        }

        // 6. Watch / Health Connect sync.
        if (text.contains("sync")) {
            return ChandraParsedCommand(ChandraActionId.WATCH_SYNC, rawText)
        }

        // 7. Diary / reports navigation.
        if (text.contains("diary") || text.contains("what did i eat") || text.contains("today's meals") ||
            text.contains("todays meals") || text.contains("today's food") || text.contains("todays food")
        ) {
            return ChandraParsedCommand(ChandraActionId.OPEN_DIARY, rawText)
        }
        if (text.contains("weekly report") || text.contains("week's report") || text.contains("weeks report") ||
            (text.contains("progress") && text.contains("week"))
        ) {
            return ChandraParsedCommand(ChandraActionId.WEEKLY_REPORT, rawText)
        }
        if (text.contains("daily report") || text.contains("today's report") || text.contains("todays report")) {
            return ChandraParsedCommand(ChandraActionId.DAILY_REPORT, rawText)
        }
        if (text.contains("progress") || text.contains("weight progress")) {
            return ChandraParsedCommand(ChandraActionId.PROGRESS, rawText)
        }

        // 8. Add food - "add 2 eggs", "2 idli add pannu", "I ate chicken rice".
        ATE_PATTERN.find(text)?.let { m ->
            return ChandraParsedCommand(ChandraActionId.ADD_FOOD, rawText, subject = m.groupValues[2].trim())
        }
        if ((text.startsWith("add ") || text.endsWith(" pannu") || text.contains(" add pannu")) &&
            !text.contains("water") && !text.contains("minute") && !text.contains("min ")
        ) {
            val stripped = text
                .removePrefix("add ")
                .removeSuffix(" add pannu")
                .removeSuffix(" pannu")
                .removeSuffix(" to my diary")
                .trim()
            if (stripped.isNotBlank()) {
                val firstWord = stripped.substringBefore(" ")
                val qty = firstWord.toDoubleOrNull() ?: NUMBER_WORDS[firstWord]
                val subject = if (qty != null) stripped.substringAfter(" ").trim() else stripped
                if (subject.isNotBlank()) {
                    return ChandraParsedCommand(ChandraActionId.ADD_FOOD, rawText, quantity = qty, subject = subject)
                }
            }
        }

        // 9. Custom user-defined aliases (checked before the generic keyword fallback so a
        //    user's own phrase always wins over an approximate built-in match).
        val customMatch = customCommands.firstOrNull { c ->
            val phrase = normalize(c.phrase).replace(WAKE_PREFIX, "").trim()
            phrase.isNotBlank() && (text.contains(phrase) || phrase.contains(text))
        }
        if (customMatch != null) {
            val actionId = runCatching { ChandraActionId.valueOf(customMatch.actionId) }.getOrNull()
            if (actionId != null) return ChandraParsedCommand(actionId, rawText)
        }

        // 10. Built-in status-query keyword scoring (health + nutrition status reads).
        val scored = STATUS_KEYWORDS.entries
            .map { (action, fragments) -> action to fragments.count { text.contains(it) } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }

        return ChandraParsedCommand(scored?.first, rawText)
    }

    private val STATUS_KEYWORDS: Map<ChandraActionId, List<String>> = mapOf(
        ChandraActionId.HEART_RATE to listOf("heart rate", "heartrate", "pulse", "heartbeat"),
        ChandraActionId.STEPS to listOf("step"),
        ChandraActionId.WEIGHT to listOf("weight"),
        ChandraActionId.SLEEP to listOf("sleep"),
        ChandraActionId.SPO2 to listOf("spo2", "sp o2", "oxygen level", "oxygen"),
        ChandraActionId.BLOOD_PRESSURE to listOf("blood pressure", " bp ", "bp enna"),
        ChandraActionId.CALORIES_STATUS to listOf("calorie", "calories"),
        ChandraActionId.PROTEIN_STATUS to listOf("protein"),
        ChandraActionId.WATER_STATUS to listOf("water")
    )

    private fun normalize(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()
}
