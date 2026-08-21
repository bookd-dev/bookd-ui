package com.bookd.app

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings

private val factory: Settings.Factory = PreferencesSettings.Factory()

actual val settings: Settings = factory.create("BookdSettings")