package com.bookd.app

import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private val factory: Settings.Factory = SharedPreferencesSettings.Factory(BookdApplication.instance)

actual val settings: Settings = factory.create("BookdSettings")