package com.aichat.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun AiChatNavGraph(
    navController: NavHostController,
    chatsScreen: @Composable () -> Unit,
    chatScreen: @Composable (String) -> Unit,
    createChatScreen: @Composable () -> Unit,
    settingsScreen: @Composable () -> Unit,
    profilesScreen: @Composable () -> Unit,
    createProfileScreen: @Composable (Boolean) -> Unit,
    memoryScreen: @Composable () -> Unit,
    invariantsScreen: @Composable () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chats.route
    ) {
        composable(Screen.Chats.route) {
            chatsScreen()
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            chatScreen(chatId)
        }

        composable(Screen.CreateChat.route) {
            createChatScreen()
        }

        composable(Screen.Settings.route) {
            settingsScreen()
        }

        composable(Screen.Profiles.route) {
            profilesScreen()
        }

        composable(
            route = Screen.CreateProfile.route,
            arguments = listOf(
                navArgument("firstLaunch") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val firstLaunch = backStackEntry.arguments?.getBoolean("firstLaunch") ?: false
            createProfileScreen(firstLaunch)
        }

        composable(Screen.Memory.route) {
            memoryScreen()
        }

        composable(Screen.Invariants.route) {
            invariantsScreen()
        }
    }
}
