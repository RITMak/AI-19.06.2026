package com.aichat.features.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.core.domain.repository.ProfileRepository
import com.aichat.core.domain.usecase.CreateChatUseCase
import com.aichat.core.domain.usecase.DeleteChatUseCase
import com.aichat.core.domain.usecase.GetChatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val createChatUseCase: CreateChatUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                profileRepository.getAllProfiles(),
                profileRepository.getActiveProfile()
            ) { profiles, activeProfile ->
                profiles to activeProfile
            }.collect { (profiles, activeProfile) ->
                _uiState.update { current ->
                    current.copy(
                        profiles = profiles,
                        activeProfile = activeProfile,
                        isSwitchingProfile = false,
                        isInitialized = true
                    )
                }
            }
        }

        viewModelScope.launch {
            profileRepository.getActiveProfile().collect { profile ->
                val id = profile?.id ?: return@collect
                loadChats(id)
            }
        }
    }

    private fun loadChats(profileId: String) {
        viewModelScope.launch {
            getChatsUseCase(profileId).collect { chats ->
                _uiState.value = _uiState.value.copy(chats = chats)
            }
        }
    }

    fun refreshChats() {
        val profile = _uiState.value.activeProfile ?: return
        loadChats(profile.id)
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            deleteChatUseCase(chatId)
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            val created = profileRepository.createProfile(name)
            profileRepository.setActiveProfile(created.id)
        }
    }

    fun switchProfile(profileId: String) {
        _uiState.value = _uiState.value.copy(isSwitchingProfile = true)
        viewModelScope.launch {
            profileRepository.setActiveProfile(profileId)
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profileId)
        }
    }
}