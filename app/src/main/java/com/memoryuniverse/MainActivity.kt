package com.memoryuniverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.memoryuniverse.core.navigation.AppNavGraph
import com.memoryuniverse.core.theme.MemoryUniverseTheme
import com.memoryuniverse.core.theme.SpaceBlack

/**
 * Main Activity of Memory Universe.
 * Initializes edge-to-edge pitch-black display and Jetpack Compose navigation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MemoryUniverseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SpaceBlack
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
