package com.aichat.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        private val KEY_DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")
    }

    val openRouterApiKey: Flow<String?> = context.dataStore.data.map { it[KEY_OPENROUTER_API_KEY] }
    val deepSeekApiKey: Flow<String?> = context.dataStore.data.map { it[KEY_DEEPSEEK_API_KEY] }

    suspend fun saveOpenRouterApiKey(key: String) {
        context.dataStore.edit { it[KEY_OPENROUTER_API_KEY] = key }
    }

    suspend fun saveDeepSeekApiKey(key: String) {
        context.dataStore.edit { it[KEY_DEEPSEEK_API_KEY] = key }
    }
}