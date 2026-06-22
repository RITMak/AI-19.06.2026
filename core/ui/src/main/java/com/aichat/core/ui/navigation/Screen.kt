package com.aichat.core.ui.navigation

sealed class Screen(val route: String) {
    data object Chats : Screen("chats")
    data object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    data object CreateChat : Screen("create-chat")
    data object Settings : Screen("settings")
    data object Profiles : Screen("profiles")
    data object CreateProfile : Screen("create-profile?firstLaunch={firstLaunch}") {
        fun createRoute(firstLaunch: Boolean = false) = "create-profile?firstLaunch=$firstLaunch"
    }
    data object Memory : Screen("memory")
    data object Invariants : Screen("invariants")
}
