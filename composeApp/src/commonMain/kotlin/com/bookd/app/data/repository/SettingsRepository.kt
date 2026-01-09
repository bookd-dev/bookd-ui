package com.bookd.app.data.repository

import com.russhwolf.settings.Settings

/**
 * This class demonstrates common code exercising all of the functionality of the [Settings] class.
 * The majority of this functionality is delegated to [SettingConfig] subclasses for each supported type.
 */
class SettingsRepository(private val settings: Settings) {


    fun clear() = settings.clear()
}