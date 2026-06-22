package com.aichat.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.core.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                profileRepository.getAllProfiles(),
                profileRepository.getActiveProfile()
            ) { profiles, activeProfile ->
                ProfileUiState(
                    profiles = profiles,
                    activeProfileId = activeProfile?.id,
                    isInitialized = true
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            val created = profileRepository.createProfile(name)
            profileRepository.setActiveProfile(created.id)
        }
    }

    fun switchProfile(profileId: String) {
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