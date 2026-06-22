package com.aichat.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.core.domain.model.Invariant
import com.aichat.core.domain.repository.InvariantRepository
import com.aichat.core.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvariantsUiState(
    val invariants: List<Invariant> = emptyList()
)

@HiltViewModel
class InvariantsViewModel @Inject constructor(
    private val invariantRepository: InvariantRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvariantsUiState())
    val uiState: StateFlow<InvariantsUiState> = _uiState.asStateFlow()

    private var currentProfileId: String? = null

    init {
        observeInvariants()
    }

    private fun observeInvariants() {
        viewModelScope.launch {
            profileRepository.getActiveProfile().collect { profile ->
                val pid = profile?.id ?: return@collect
                currentProfileId = pid
                invariantRepository.observeByProfile(pid).collect { invariants ->
                    _uiState.update { it.copy(invariants = invariants) }
                }
            }
        }
    }

    fun addInvariant(text: String) {
        val profileId = currentProfileId ?: return
        viewModelScope.launch {
            invariantRepository.add(profileId, text)
        }
    }

    fun removeInvariant(id: String) {
        viewModelScope.launch {
            invariantRepository.remove(id)
        }
    }
}