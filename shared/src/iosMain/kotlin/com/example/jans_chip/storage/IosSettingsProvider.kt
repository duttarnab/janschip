package com.example.jans_chip.storage

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

class IosSettingsProvider : SettingsProvider {
    override fun getSettings(): Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults())
}