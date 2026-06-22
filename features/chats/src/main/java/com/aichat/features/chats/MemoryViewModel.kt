package com.aichat.features.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.core.domain.model.LongTermMemoryDomain
import com.aichat.core.domain.model.WorkingMemoryDomain
import com.aichat.core.domain.repository.LongTermMemoryRepository
import com.aichat.core.domain.repository.ProfileRepository
import com.aichat.core.domain.repository.WorkingMemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class MemoryUiState(
    val workingMemories: List<WorkingMemoryDomain> = emptyList(),
    val longTermMemories: List<LongTermMemoryDomain> = emptyList()
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val workingMemoryRepository: WorkingMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    private var currentProfileId: String? = null

    init {
        observeMemories()
    }

    private fun observeMemories() {
        viewModelScope.launch {
            profileRepository.getActiveProfile().collect { profile ->
                val pid = profile?.id ?: return@collect
                currentProfileId = pid
                observeWorkingMemories(pid)
                observeLongTermMemories(pid)
            }
        }
    }

    private fun observeWorkingMemories(profileId: String) {
        viewModelScope.launch {
            workingMemoryRepository.getByProfileId(profileId).collect { memories ->
                _uiState.update { it.copy(workingMemories = memories) }
            }
        }
    }

    private fun observeLongTermMemories(profileId: String) {
        viewModelScope.launch {
            longTermMemoryRepository.getByProfileId(profileId).collect { memories ->
                _uiState.update { it.copy(longTermMemories = memories) }
            }
        }
    }

    // --- Working Memory ---

    fun addWorkingMemory(title: String) {
        val profileId = currentProfileId ?: return
        viewModelScope.launch {
            workingMemoryRepository.upsert(
                WorkingMemoryDomain(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    title = title
                )
            )
        }
    }

    fun updateWorkingMemory(memory: WorkingMemoryDomain, title: String) {
        viewModelScope.launch {
            workingMemoryRepository.upsert(memory.copy(title = title))
        }
    }

    fun deleteWorkingMemory(memoryId: String) {
        viewModelScope.launch {
            workingMemoryRepository.delete(memoryId)
        }
    }

    // --- Long Term Memory ---

    fun addLongTermMemory(key: String, value: String) {
        val profileId = currentProfileId ?: return
        viewModelScope.launch {
            longTermMemoryRepository.upsert(
                LongTermMemoryDomain(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    key = key,
                    value = value
                )
            )
        }
    }

    fun updateLongTermMemory(memory: LongTermMemoryDomain, key: String, value: String) {
        viewModelScope.launch {
            longTermMemoryRepository.upsert(memory.copy(key = key, value = value))
        }
    }

    fun deleteLongTermMemory(memoryId: String) {
        val protectedKeys = listOf("profile_name")
        viewModelScope.launch {
            val memory = longTermMemoryRepository.getById(memoryId)
            if (memory != null && memory.key in protectedKeys) return@launch
            longTermMemoryRepository.delete(memoryId)
        }
    }
}