package com.dietary.tracker.chandra

/**
 * Example phrases shown to the user in "Chandra Voice Commands" for each action. These are
 * illustrative of what [ChandraCommandParser] already understands (it matches keywords/patterns,
 * not this exact list verbatim) - kept here only so the management screen has something concrete
 * to display per feature, per the spec's "Default Commands" list requirement.
 */
object ChandraDefaultPhrases {

    val byAction: Map<ChandraActionId, List<String>> = mapOf(
        ChandraActionId.HEART_RATE to listOf(
            "What's my heart rate?", "Tell me my heart rate", "Chandra pulse sollu", "Ennaoda heart rate enna?"
        ),
        ChandraActionId.STEPS to listOf(
            "How many steps did I walk today?", "What's my step count?", "Steps evlo?", "Innaiku steps evlo?"
        ),
        ChandraActionId.WEIGHT to listOf(
            "What's my weight?", "En weight enna?"
        ),
        ChandraActionId.SLEEP to listOf(
            "How many hours did I sleep?", "Sleep evlo?"
        ),
        ChandraActionId.SPO2 to listOf(
            "What's my oxygen level?", "What's my SpO2?"
        ),
        ChandraActionId.BLOOD_PRESSURE to listOf(
            "What's my blood pressure?", "BP enna?"
        ),
        ChandraActionId.ADD_FOOD to listOf(
            "Add 2 eggs", "Add one banana", "I ate chicken rice", "2 idli add pannu"
        ),
        ChandraActionId.CALORIES_STATUS to listOf(
            "How many calories did I eat?", "How many calories are remaining?", "Calories sollu"
        ),
        ChandraActionId.PROTEIN_STATUS to listOf(
            "How much protein did I eat?", "Protein evlo?"
        ),
        ChandraActionId.WATER_STATUS to listOf(
            "How much water did I drink?", "How much water is remaining?", "Water evlo kudichiruken?"
        ),
        ChandraActionId.ADD_WATER to listOf(
            "Add 500 ml water", "Add one glass of water", "Water add pannu"
        ),
        ChandraActionId.OPEN_DIARY to listOf(
            "Open my diary", "Show today's food", "What did I eat today?"
        ),
        ChandraActionId.ADD_EXERCISE to listOf(
            "Add 30 minutes walking", "Add 45 minutes gym", "30 mins walking add pannu"
        ),
        ChandraActionId.START_FASTING to listOf("Start fasting", "Start my fasting"),
        ChandraActionId.END_FASTING to listOf("Stop fasting", "End fasting"),
        ChandraActionId.FASTING_STATUS to listOf("How long have I been fasting?", "When does my fasting end?"),
        ChandraActionId.CALORIE_GOAL to listOf("What's my calorie goal?", "Set my calorie goal to 2000"),
        ChandraActionId.PROTEIN_GOAL to listOf("What's my protein goal?", "Change my protein goal to 120 grams"),
        ChandraActionId.WATER_GOAL to listOf("What's my water goal?", "Set my water goal to 3 litres"),
        ChandraActionId.WEIGHT_GOAL to listOf("What's my weight goal?"),
        ChandraActionId.DAILY_REPORT to listOf("Show today's report"),
        ChandraActionId.WEEKLY_REPORT to listOf("Show my weekly report", "How was my progress this week?"),
        ChandraActionId.PROGRESS to listOf("Show my weight progress"),
        ChandraActionId.WATCH_SYNC to listOf("Sync my watch", "Sync my health data", "Sync Health Connect")
    )
}
