package com.memoryuniverse.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.memoryuniverse.core.theme.CosmicViolet
import com.memoryuniverse.core.theme.SpaceBlack
import com.memoryuniverse.core.theme.StarlightMuted
import com.memoryuniverse.core.theme.StarlightWhite
import com.memoryuniverse.ui.components.StarfieldBackground
import com.memoryuniverse.ui.home.HomeScreen
import com.memoryuniverse.ui.home.HomeViewModel

import com.memoryuniverse.ui.universe.UniverseScreen
import com.memoryuniverse.ui.universe.UniverseViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Universe.route,
        modifier = modifier
    ) {
        composable(Screen.Universe.route) {
            val universeViewModel: UniverseViewModel = viewModel()
            UniverseScreen(
                viewModel = universeViewModel,
                onNavigateBack = {
                    navController.navigate(Screen.Gallery.route)
                },
                onOpenPhotoViewer = { memoryId ->
                    navController.navigate(Screen.PhotoViewer.createRoute(memoryId))
                },
                onNavigateToCreateNote = {
                    navController.navigate(Screen.CreateNote.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToClusters = {
                    navController.navigate(Screen.ExploreClusters.route)
                }
            )
        }

        composable(Screen.Gallery.route) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                onNavigateToUniverse = {
                    navController.popBackStack(Screen.Universe.route, false)
                },
                onPhotoClick = { memoryId ->
                    navController.navigate(Screen.PhotoViewer.createRoute(memoryId))
                }
            )
        }

        composable(Screen.ExploreClusters.route) {
            val clustersViewModel: com.memoryuniverse.ui.clusters.ClustersViewModel = viewModel()
            com.memoryuniverse.ui.clusters.ExploreClustersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMemory = { memoryId, _ ->
                    navController.navigate(Screen.PhotoViewer.createRoute(memoryId))
                },
                viewModel = clustersViewModel
            )
        }

        composable(Screen.Search.route) {
            val searchViewModel: com.memoryuniverse.ui.search.SearchViewModel = viewModel()
            com.memoryuniverse.ui.search.SearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenPhotoViewer = { memoryId ->
                    navController.navigate(Screen.PhotoViewer.createRoute(memoryId))
                }
            )
        }

        composable(Screen.CreateNote.route) {
            val noteViewModel: com.memoryuniverse.ui.notes.CreateNoteViewModel = viewModel()
            com.memoryuniverse.ui.notes.CreateNoteScreen(
                viewModel = noteViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PhotoViewer.route,
            arguments = listOf(navArgument("memoryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val memoryId = backStackEntry.arguments?.getLong("memoryId") ?: 0L
            val photoViewerViewModel: com.memoryuniverse.ui.photoviewer.PhotoViewerViewModel = viewModel()
            com.memoryuniverse.ui.photoviewer.PhotoViewerScreen(
                memoryId = memoryId,
                viewModel = photoViewerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            PlaceholderCosmicScreen(title = "Cosmic Settings", subtitle = "Local Privacy & Storage Controls")
        }
    }
}

@Composable
private fun PlaceholderCosmicScreen(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack),
        contentAlignment = Alignment.Center
    ) {
        StarfieldBackground()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = title,
                color = StarlightWhite,
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = CosmicViolet,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Memory Universe Architecture Foundation",
                color = StarlightMuted,
                fontSize = 12.sp
            )
        }
    }
}
