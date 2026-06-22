package com.aichat.features.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichat.core.domain.model.ChatDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onChatClick: (String) -> Unit,
    onNavigateToCreateChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToCreateProfile: () -> Unit = onNavigateToProfiles,
    onNavigateToMemory: () -> Unit = {},
    onNavigateToInvariants: () -> Unit = {},
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Gate: нет профиля — редирект на создание профиля
    LaunchedEffect(uiState.isInitialized) {
        if (uiState.isInitialized && uiState.activeProfile == null) {
            onNavigateToCreateProfile()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "AiChat",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                // Блок профиля
                Text(
                    text = "Профиль: ${uiState.activeProfile?.name ?: "Не выбран"}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.SwitchAccount, contentDescription = null) },
                    label = { Text("Сменить профиль") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToProfiles()
                    }
                )
                NavigationDrawerItem(
                    icon = { Text("🧠", style = MaterialTheme.typography.titleMedium) },
                    label = { Text("Память") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToMemory()
                    }
                )
                NavigationDrawerItem(
                    icon = { Text("📜", style = MaterialTheme.typography.titleMedium) },
                    label = { Text("Инварианты") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToInvariants()
                    }
                )
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                    label = { Text("Чаты") },
                    selected = true,
                    onClick = {
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Чаты") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToCreateChat() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New chat")
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.chats, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        onClick = { onChatClick(chat.id) },
                        onDelete = { viewModel.deleteChat(chat.id) }
                    )
                }
            }
        }
    }


}

@Composable
fun ChatListItem(
    chat: ChatDomain,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chat.provider.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}