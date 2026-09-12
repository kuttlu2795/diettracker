package com.dietary.tracker.chandra

/**
 * Every capability Chandra supports. This is the single registry both the built-in parser and
 * the custom-command system (user aliases) key off of, and the same IDs are shown/edited in the
 * "Chandra Voice Commands" management screen.
 */
enum class ChandraActionId(val category: String, val displayName: String) {
    HEART_RATE("Health", "Heart Rate"),
    STEPS("Health", "Steps"),
    WEIGHT("Health", "Weight"),
    SLEEP("Health", "Sleep"),
    SPO2("Health", "SpO2"),
    BLOOD_PRESSURE("Health", "Blood Pressure"),

    ADD_FOOD("Nutrition", "Add Food"),
    CALORIES_STATUS("Nutrition", "Calories"),
    PROTEIN_STATUS("Nutrition", "Protein"),
    WATER_STATUS("Nutrition", "Water Status"),
    ADD_WATER("Nutrition", "Add Water"),
    OPEN_DIARY("Nutrition", "Diary"),

    ADD_EXERCISE("Fitness", "Add Exercise"),

    START_FASTING("Fasting", "Start Fasting"),
    END_FASTING("Fasting", "End Fasting"),
    FASTING_STATUS("Fasting", "Fasting Status"),

    CALORIE_GOAL("Goals", "Calorie Goal"),
    PROTEIN_GOAL("Goals", "Protein Goal"),
    WATER_GOAL("Goals", "Water Goal"),
    WEIGHT_GOAL("Goals", "Weight Goal"),

    DAILY_REPORT("Reports", "Daily Report"),
    WEEKLY_REPORT("Reports", "Weekly Report"),
    PROGRESS("Reports", "Progress"),

    WATCH_SYNC("Devices", "Watch Sync")
}

/** A single utterance parsed into an action + whatever free parameters it carried. */
data class ChandraParsedCommand(
    val actionId: ChandraActionId?,   // null = unrecognized
    val rawText: String,
    val quantity: Double? = null,     // e.g. "2" in "add 2 eggs", or ml in "add 500 ml water"
    val subject: String? = null,      // e.g. "eggs" in "add 2 eggs", exercise name, goal value text
    val durationMinutes: Int? = null  // e.g. "30" in "30 minutes walking"
)

/** What executing a parsed command produced. */
data class ChandraResult(
    val spokenText: String,
    val navigateTo: String? = null // a Screen.route to navigate to, if any
)
