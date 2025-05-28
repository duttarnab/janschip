package com.example.jans_chip.storage

import android.content.Context
//import com.russhwolf.settings.AndroidSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

class AndroidSettingsProvider(context: Context) : SettingsProvider {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    override fun getSettings(): Settings = SharedPreferencesSettings(prefs)
}