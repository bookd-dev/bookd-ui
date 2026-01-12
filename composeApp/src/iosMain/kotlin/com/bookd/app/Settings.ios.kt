package com.bookd.app

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings

private val factory: Settings.Factory = NSUserDefaultsSettings.Factory()

actual val settings: Settings = factory.create("BookdSettings")