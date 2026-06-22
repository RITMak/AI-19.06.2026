package com.aichat.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.core.data.datastore.SettingsDataStore
import com.aichat.core.database.dao.ChatDao
import com.aichat.core.database.dao.MessageDao
import com.aichat.core.database.dao.ModelDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val modelDao: ModelDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val openRouterKey = settingsDataStore.openRouterApiKey.first() ?: ""
            val deepSeekKey = settingsDataStore.deepSeekApiKey.first() ?: ""
            _uiState.value = _uiState.value.copy(
                openRouterKey = openRouterKey,
                deepSeekKey = deepSeekKey
            )
        }
    }

    fun saveOpenRouterKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.saveOpenRouterApiKey(key)
            _uiState.value = _uiState.value.copy(openRouterKey = key)
        }
    }

    fun saveDeepSeekKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.saveDeepSeekApiKey(key)
            _uiState.value = _uiState.value.copy(deepSeekKey = key)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatDao.deleteAllChats()
            modelDao.deleteAllModels()
        }
    }
}