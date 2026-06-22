package com.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.aichat.core.ui.navigation.AiChatNavGraph
import com.aichat.core.ui.navigation.Screen
import com.aichat.core.ui.theme.AiChatTheme
import com.aichat.features.chat.ChatScreen
import com.aichat.features.chatcreate.CreateChatScreen
import com.aichat.features.chats.ChatsScreen
import com.aichat.features.chats.MemoryScreen
import com.aichat.features.profile.ProfileCreateScreen
import com.aichat.features.profile.ProfileScreen
import com.aichat.features.settings.InvariantsScreen
import com.aichat.features.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AiChatApp()
                }
            }
        }
    }
}

@Composable
fun AiChatApp() {
    val navController = rememberNavController()

    AiChatNavGraph(
        navController = navController,
        chatsScreen = {
            ChatsScreen(
                onChatClick = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onNavigateToCreateChat = {
                    navController.navigate(Screen.CreateChat.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToProfiles = {
                    navController.navigate(Screen.Profiles.route)
                },
                onNavigateToCreateProfile = {
                    navController.navigate(Screen.CreateProfile.createRoute(firstLaunch = true)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToMemory = {
                    navController.navigate(Screen.Memory.route)
                },
                onNavigateToInvariants = {
                    navController.navigate(Screen.Invariants.route)
                }
            )
        },
        chatScreen = { chatId ->
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        },
        createChatScreen = {
            CreateChatScreen(
                onBack = { navController.popBackStack() },
                onModelSelected = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId)) {
                        popUpTo(Screen.Chats.route)
                    }
                }
            )
        },
        settingsScreen = {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onClearHistory = {
                    navController.popBackStack(Screen.Chats.route, inclusive = false)
                }
            )
        },
        profilesScreen = {
            ProfileScreen(
                onSelectProfile = {
                    navController.navigate(Screen.Chats.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onShowCreateProfile = { navController.navigate(Screen.CreateProfile.createRoute(firstLaunch = false)) },
                onBack = { navController.popBackStack() }
            )
        },
        memoryScreen = {
            MemoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        },
        invariantsScreen = {
            InvariantsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        },
        createProfileScreen = { firstLaunch ->
            ProfileCreateScreen(
                onProfileCreated = {
                    if (firstLaunch) {
                        navController.navigate(Screen.Chats.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack = {
                    if (firstLaunch) {
                        navController.navigate(Screen.Chats.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
    )
}
