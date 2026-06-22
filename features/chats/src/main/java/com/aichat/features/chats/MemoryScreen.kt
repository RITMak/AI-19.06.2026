package com.aichat.features.chats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichat.core.domain.model.LongTermMemoryDomain
import com.aichat.core.domain.model.WorkingMemoryDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Рабочая", "Долговременная")

    // Состояния для диалогов
    var showWorkingDialog by remember { mutableStateOf(false) }
    var showLtDialog by remember { mutableStateOf(false) }
    var editingWorkingMemory by remember { mutableStateOf<WorkingMemoryDomain?>(null) }
    var editingLtMemory by remember { mutableStateOf<LongTermMemoryDomain?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧠 Память") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> WorkingMemoryTab(
                    memories = uiState.workingMemories,
                    onAdd = {
                        editingWorkingMemory = null
                        showWorkingDialog = true
                    },
                    onEdit = { memory ->
                        editingWorkingMemory = memory
                        showWorkingDialog = true
                    },
                    onDelete = { memory ->
                        viewModel.deleteWorkingMemory(memory.id)
                    }
                )
                1 -> LongTermMemoryTab(
                    memories = uiState.longTermMemories,
                    onAdd = {
                        editingLtMemory = null
                        showLtDialog = true
                    },
                    onEdit = { memory ->
                        editingLtMemory = memory
                        showLtDialog = true
                    },
                    onDelete = { memory ->
                        viewModel.deleteLongTermMemory(memory.id)
                    }
                )
            }
        }
    }

    // Диалог рабочей памяти
    if (showWorkingDialog) {
        val initialTitle = editingWorkingMemory?.title ?: ""
        WorkingMemoryDialog(
            initialTitle = initialTitle,
            onConfirm = { title ->
                showWorkingDialog = false
                if (editingWorkingMemory != null) {
                    viewModel.updateWorkingMemory(editingWorkingMemory!!, title)
                } else {
                    viewModel.addWorkingMemory(title)
                }
            },
            onDismiss = { showWorkingDialog = false }
        )
    }

    // Диалог долговременной памяти
    if (showLtDialog) {
        val initialKey = editingLtMemory?.key ?: ""
        val initialValue = editingLtMemory?.value ?: ""
        LongTermMemoryDialog(
            initialKey = initialKey,
            initialValue = initialValue,
            onConfirm = { key, value ->
                showLtDialog = false
                if (editingLtMemory != null) {
                    viewModel.updateLongTermMemory(editingLtMemory!!, key, value)
                } else {
                    viewModel.addLongTermMemory(key, value)
                }
            },
            onDismiss = { showLtDialog = false }
        )
    }
}

@Composable
private fun WorkingMemoryTab(
    memories: List<WorkingMemoryDomain>,
    onAdd: () -> Unit,
    onEdit: (WorkingMemoryDomain) -> Unit,
    onDelete: (WorkingMemoryDomain) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
            }
        }

        if (memories.isEmpty()) {
            Text("Нет задач", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(memories, key = { it.id }) { memory ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = memory.title,
                            modifier = Modifier.weight(1f),
                            fontWeight = MaterialTheme.typography.titleSmall.fontWeight
                        )
                        IconButton(onClick = { onEdit(memory) }) {
                            Text("✏️")
                        }
                        IconButton(onClick = { onDelete(memory) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LongTermMemoryTab(
    memories: List<LongTermMemoryDomain>,
    onAdd: () -> Unit,
    onEdit: (LongTermMemoryDomain) -> Unit,
    onDelete: (LongTermMemoryDomain) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Добавить запись")
            }
        }

        if (memories.isEmpty()) {
            Text("Нет записей", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(memories, key = { it.id }) { memory ->
                    val isProtected = memory.key == "profile_name"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = memory.key, fontWeight = MaterialTheme.typography.titleSmall.fontWeight)
                            Text(text = memory.value, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!isProtected) {
                            IconButton(onClick = { onEdit(memory) }) {
                                Text("\u270F\uFE0F")
                            }
                            IconButton(onClick = { onDelete(memory) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }
}

