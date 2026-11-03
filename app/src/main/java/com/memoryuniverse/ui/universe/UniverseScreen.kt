package com.memoryuniverse.ui.universe

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memoryuniverse.core.theme.AstralBlue
import com.memoryuniverse.core.theme.CosmicViolet
import com.memoryuniverse.core.theme.DeepVoid
import com.memoryuniverse.core.theme.GlassBorder
import com.memoryuniverse.core.theme.SpaceBlack
import com.memoryuniverse.core.theme.StarlightMuted
import com.memoryuniverse.core.theme.StarlightSecondary
import com.memoryuniverse.core.theme.StarlightWhite
import com.memoryuniverse.core.theme.SupernovaRose
import com.memoryuniverse.ui.components.CosmicCard
import com.memoryuniverse.universe.camera.universeGestureHandler
import com.memoryuniverse.universe.renderer.UniverseCanvasRenderer
import com.memoryuniverse.universe.renderer.UniverseObject3D
import java.util.Date

@Composable
fun UniverseScreen(
    viewModel: UniverseViewModel,
    onNavigateBack: () -> Unit,
    onOpenPhotoViewer: (Long) -> Unit,
    onNavigateToCreateNote: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToClusters: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val renderer = remember { UniverseCanvasRenderer() }

    // Real-time animation clock for continuous 60 FPS celestial rendering
    var timeSeconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                timeSeconds = (now - startTime) / 1_000_000_000f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .onSizeChanged { size ->
                viewModel.onViewportSizeChanged(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        // 3D Universe Projection Canvas with Gesture Processing
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .universeGestureHandler(
                    cameraState = uiState.cameraState,
                    onZoomChange = { viewModel.onZoom(it) },
                    onPanChange = { dx, dy -> viewModel.onPan(dx, dy) },
                    onTap = { x, y -> viewModel.onScreenTap(x, y) },
                    onDoubleTap = { x, y -> viewModel.onDoubleTap(x, y) }
                )
        ) {
            val objectsToRender = if (uiState.showFavoritesOnly) {
                uiState.objects.filter { it.isFavorite }
            } else {
                uiState.objects
            }

            renderer.renderUniverse(
                drawScope = this,
                cameraState = uiState.cameraState,
                objects = objectsToRender,
                clusters = uiState.clusters,
                selectedMemoryId = uiState.selectedMemory?.memoryId,
                timeSeconds = timeSeconds
            )
        }

        // Top Cosmic HUD Bar
        TopCosmicHud(
            panX = uiState.cameraState.panX,
            panY = uiState.cameraState.panY,
            zoom = uiState.cameraState.zoomDistance,
            clusterCount = uiState.clusters.size,
            objectCount = uiState.objects.size,
            onBack = onNavigateBack,
            onSearch = onNavigateToSearch,
            onClusters = onNavigateToClusters
        )

        // Right Edge Floating Camera Navigation Controls
        RightCameraControls(
            onReset = { viewModel.resetView() },
            onZoomIn = { viewModel.zoomIn() },
            onZoomOut = { viewModel.zoomOut() },
            onAddNote = onNavigateToCreateNote,
            isFavoritesOnly = uiState.showFavoritesOnly,
            onToggleFavorites = { viewModel.toggleFavoritesFilter() }
        )

        // Bottom Selected Memory Inspector Panel
        AnimatedVisibility(
            visible = uiState.selectedMemory != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            uiState.selectedMemory?.let { memory ->
                SelectedMemorySheet(
                    memory = memory,
                    onDismiss = { viewModel.dismissSelectedMemory() },
                    onInspect = { onOpenPhotoViewer(memory.memoryId) }
                )
            }
        }
    }
}

@Composable
private fun TopCosmicHud(
    panX: Float,
    panY: Float,
    zoom: Float,
    clusterCount: Int,
    objectCount: Int,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onClusters: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Back to Gallery Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x9907070D))
                    .border(1.dp, GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = StarlightWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Search AI Button
            IconButton(
                onClick = onSearch,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x9907070D))
                    .border(1.dp, AstralBlue.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Search,
                    contentDescription = "Search",
                    tint = AstralBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Galaxy Selector Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xCC07070D))
                .border(1.dp, CosmicViolet.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .clickable { onClusters() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AstralBlue)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (clusterCount > 0) "$clusterCount Galaxies" else "Universe",
                color = StarlightWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "($objectCount)",
                color = AstralBlue,
                fontSize = 11.sp
            )
        }

        // Coordinates Readout
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x9907070D))
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Z:${zoom.toInt()}",
                color = StarlightSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun RightCameraControls(
    onReset: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onAddNote: () -> Unit,
    isFavoritesOnly: Boolean,
    onToggleFavorites: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 16.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xDD07070D))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Origin",
                    tint = AstralBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onAddNote) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "New Note",
                    tint = com.memoryuniverse.core.theme.StellarAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onZoomIn) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = StarlightWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onZoomOut) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = StarlightWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onToggleFavorites) {
                Icon(
                    imageVector = if (isFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorites",
                    tint = if (isFavoritesOnly) SupernovaRose else StarlightMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectedMemorySheet(
    memory: UniverseObject3D,
    onDismiss: () -> Unit,
    onInspect: () -> Unit
) {
    val context = LocalContext.current
    val isNote = memory.memoryType == com.memoryuniverse.core.database.entity.MemoryType.NOTE ||
                 memory.memoryType == com.memoryuniverse.core.database.entity.MemoryType.PHOTO_ATTACHED_NOTE

    val dateString = remember(memory.timestamp) {
        DateFormat.format("MMMM d, yyyy", Date(memory.timestamp)).toString()
    }

    CosmicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        borderColor = if (isNote) com.memoryuniverse.core.theme.StellarAmber.copy(alpha = 0.7f) else CosmicViolet.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isNote) {
                // Note Icon Thumbnail
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, com.memoryuniverse.core.theme.StellarAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Description,
                        contentDescription = "Note",
                        tint = com.memoryuniverse.core.theme.StellarAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                // Photo Thumbnail
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(memory.mediaUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = memory.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memory.title ?: if (isNote) "Celestial Note #${memory.memoryId}" else "Memory #${memory.memoryId}",
                    color = StarlightWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (isNote && !memory.textContent.isNullOrBlank()) {
                    Text(
                        text = memory.textContent,
                        color = StarlightSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = dateString,
                        color = StarlightSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Universe: (${memory.position.x.toInt()}, ${memory.position.y.toInt()}, ${memory.position.z.toInt()})",
                    color = if (isNote) com.memoryuniverse.core.theme.StellarAmber else CosmicViolet,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Action Buttons
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = StarlightMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (!isNote) {
                    Button(
                        onClick = onInspect,
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicViolet),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(text = "View", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
