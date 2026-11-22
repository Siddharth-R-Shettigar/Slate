package com.example.data.preferences

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val Context.slateDataStore: DataStore<Preferences> by preferencesDataStore(name = "slate_settings")

enum class AppColorTheme(val label: String, val previewHex: Long) {
  WALLPAPER("Wallpaper", 0xFF6750A4L),
  BLUE("Blue", 0xFF2563EBL),
  CYAN("Cyan", 0xFF06B6D4L),
  EMERALD("Emerald", 0xFF10B981L),
  SAGE("Forest", 0xFF4D7C0FL),
  PURPLE("Purple", 0xFF8B5CF6L),
  ORANGE("Orange", 0xFFF97316L),
  ROSE("Rose", 0xFFE11D48L),
}

@Immutable
data class SlateSettings(
  val openDirectlyIntoActiveList: Boolean = true,
  val useTodaysCalendarAsLaunchList: Boolean = false,
  val targetSyncCalendarId: Long? = null,
  val backdropBlurEnabled: Boolean = false,
  val defaultLaunchListId: Long = 1L,
  val oledDarkModeEnabled: Boolean = false,
  val appColorTheme: AppColorTheme = AppColorTheme.WALLPAPER,
)

class SettingsRepository(private val context: Context) {

  private object Keys {
    val OPEN_DIRECTLY_INTO_ACTIVE_LIST = booleanPreferencesKey("open_directly_into_active_list")
    val USE_TODAYS_CALENDAR_AS_LAUNCH_LIST = booleanPreferencesKey("use_todays_calendar_as_launch_list")
    val TARGET_SYNC_CALENDAR_ID = longPreferencesKey("target_sync_calendar_id")
    val BACKDROP_BLUR_ENABLED = booleanPreferencesKey("backdrop_blur_enabled")
    val DEFAULT_LAUNCH_LIST_ID = longPreferencesKey("default_launch_list_id")
    val OLED_DARK_MODE_ENABLED = booleanPreferencesKey("oled_dark_mode_enabled")
    val APP_COLOR_THEME = stringPreferencesKey("app_color_theme")
  }

  val settingsFlow: Flow<SlateSettings> = context.slateDataStore.data.map { prefs ->
    val themeName = prefs[Keys.APP_COLOR_THEME]
    val resolvedTheme = AppColorTheme.entries.firstOrNull { it.name == themeName }
      ?: AppColorTheme.WALLPAPER

    SlateSettings(
      openDirectlyIntoActiveList = prefs[Keys.OPEN_DIRECTLY_INTO_ACTIVE_LIST] ?: true,
      useTodaysCalendarAsLaunchList = prefs[Keys.USE_TODAYS_CALENDAR_AS_LAUNCH_LIST] ?: false,
      targetSyncCalendarId = prefs[Keys.TARGET_SYNC_CALENDAR_ID],
      backdropBlurEnabled = prefs[Keys.BACKDROP_BLUR_ENABLED] ?: false,
      defaultLaunchListId = prefs[Keys.DEFAULT_LAUNCH_LIST_ID] ?: 1L,
      oledDarkModeEnabled = prefs[Keys.OLED_DARK_MODE_ENABLED] ?: false,
      appColorTheme = resolvedTheme,
    )
  }.flowOn(Dispatchers.IO)

  suspend fun setOpenDirectlyIntoActiveList(enabled: Boolean) {
    context.slateDataStore.edit { prefs ->
      prefs[Keys.OPEN_DIRECTLY_INTO_ACTIVE_LIST] = enabled
    }
  }

  suspend fun setUseTodaysCalendarAsLaunchList(enabled: Boolean) {
    context.slateDataStore.edit { prefs ->
      prefs[Keys.USE_TODAYS_CALENDAR_AS_LAUNCH_LIST] = enabled
    }
  }

  suspend fun setTargetSyncCalendarId(calendarId: Long?) {
    context.slateDataStore.edit { prefs ->
      if (calendarId == null) {
        prefs.remove(Keys.TARGET_SYNC_CALENDAR_ID)
      } else {
        prefs[Keys.TARGET_SYNC_CALENDAR_ID] = calendarId
      }
    }
  }

  suspend fun setBackdropBlurEnabled(enabled: Boolean) {
    context.slateDataStore.edit { prefs ->
      prefs[Keys.BACKDROP_BLUR_ENABLED] = enabled
    }
  }

  suspend fun setDefaultLaunchListId(listId: Long) {
    context.slateDataStore.edit { prefs ->
      prefs[Keys.DEFAULT_LAUNCH_LIST_ID] = listId
    }
  }

  suspend fun setOledDarkModeEnabled(enabled: Boolean) {
    context.slateDataStore.edit { prefs ->
      prefs[Keys.OLED_DARK_MODE_ENABLED] = enabled
    }
  }

  suspend fun setAppColorTheme(theme: AppColorTheme) {
    context.slateDataStore.edit { prefs ->
      prefs[Keys.APP_COLOR_THEME] = theme.name
    }
  }
}
