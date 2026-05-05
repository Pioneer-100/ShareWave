package com.swiftshare.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "swiftshare_prefs")

data class UserPreferences(
    val isDarkTheme: Boolean = true,           // dark-first default
    val defaultSavePath: String = "",           // empty = Downloads folder
    val aiCompressionEnabled: Boolean = true,
    val aiDeduplicationEnabled: Boolean = true,
    val deviceDisplayName: String = "",         // user-set peer name
    val onboardingCompleted: Boolean = false,
    val transferHistoryRetentionDays: Int = 30,
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val DEFAULT_SAVE_PATH = stringPreferencesKey("default_save_path")
        val AI_COMPRESSION = booleanPreferencesKey("ai_compression_enabled")
        val AI_DEDUP = booleanPreferencesKey("ai_dedup_enabled")
        val DEVICE_NAME = stringPreferencesKey("device_display_name")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val HISTORY_RETENTION = intPreferencesKey("history_retention_days")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            UserPreferences(
                isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
                defaultSavePath = prefs[Keys.DEFAULT_SAVE_PATH] ?: "",
                aiCompressionEnabled = prefs[Keys.AI_COMPRESSION] ?: true,
                aiDeduplicationEnabled = prefs[Keys.AI_DEDUP] ?: true,
                deviceDisplayName = prefs[Keys.DEVICE_NAME] ?: "",
                onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
                transferHistoryRetentionDays = prefs[Keys.HISTORY_RETENTION] ?: 30,
            )
        }

    suspend fun setDarkTheme(enabled: Boolean) =
        context.dataStore.edit { it[Keys.IS_DARK_THEME] = enabled }

    suspend fun setDefaultSavePath(path: String) =
        context.dataStore.edit { it[Keys.DEFAULT_SAVE_PATH] = path }

    suspend fun setAiCompression(enabled: Boolean) =
        context.dataStore.edit { it[Keys.AI_COMPRESSION] = enabled }

    suspend fun setAiDedup(enabled: Boolean) =
        context.dataStore.edit { it[Keys.AI_DEDUP] = enabled }

    suspend fun setDeviceName(name: String) =
        context.dataStore.edit { it[Keys.DEVICE_NAME] = name }

    suspend fun setOnboardingCompleted() =
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }

    suspend fun setHistoryRetention(days: Int) =
        context.dataStore.edit { it[Keys.HISTORY_RETENTION] = days }
}
