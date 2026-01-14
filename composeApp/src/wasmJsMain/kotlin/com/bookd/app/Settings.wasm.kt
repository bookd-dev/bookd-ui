package com.bookd.app

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

actual val settings: Settings = StorageSettings() // use localStorage by default