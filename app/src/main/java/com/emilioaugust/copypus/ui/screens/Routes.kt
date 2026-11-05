package com.emilioaugust.copypus.ui.screens

sealed class Screen(val route: String) {
    object History : Screen("history")
    object Images : Screen("images")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Snippets : Screen("snippets")
}