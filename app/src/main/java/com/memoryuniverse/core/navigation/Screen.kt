package com.memoryuniverse.core.navigation

sealed class Screen(val route: String, val title: String) {
    data object Universe : Screen("universe", "Universe")
    data object Gallery : Screen("gallery", "Gallery")
    data object ExploreClusters : Screen("clusters", "Galaxies")
    data object Search : Screen("search", "Search")
    data object CreateNote : Screen("create_note", "New Note")
    data object PhotoViewer : Screen("photo_viewer/{memoryId}", "Photo") {
        fun createRoute(memoryId: Long): String = "photo_viewer/$memoryId"
    }
    data object Settings : Screen("settings", "Settings")
}
