package com.example.bilibiliautomation.data.settings

import android.content.Context

/**
 * Remembers whether the install-time welcome guide has been shown.
 * SharedPreferences are wiped on uninstall, so a fresh install shows it again.
 */
class FirstLaunchStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun shouldShowWelcome(): Boolean = !prefs.getBoolean(KEY_WELCOME, false)

    fun markWelcomeShown() {
        prefs.edit().putBoolean(KEY_WELCOME, true).apply()
    }

    private companion object {
        const val PREFS = "ui_first_launch"
        const val KEY_WELCOME = "welcome_guide_shown"
    }
}
