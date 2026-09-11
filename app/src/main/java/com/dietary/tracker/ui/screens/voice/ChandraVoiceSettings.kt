package com.dietary.tracker.ui.screens.voice

import android.content.Context

object ChandraVoiceSettings {
    private const val PREFS = "chandra_voice_settings"
    private const val ALWAYS_LISTENING = "always_listening"

    fun isAlwaysListeningEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(ALWAYS_LISTENING, false)

    fun setAlwaysListeningEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ALWAYS_LISTENING, enabled).apply()
    }
}
