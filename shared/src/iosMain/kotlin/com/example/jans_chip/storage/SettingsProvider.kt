package com.example.jans_chip.storage

import com.russhwolf.settings.Settings

interface SettingsProvider {
    fun getSettings(): Settings
}