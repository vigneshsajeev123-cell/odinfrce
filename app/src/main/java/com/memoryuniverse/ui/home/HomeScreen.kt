package com.memoryuniverse.ui.home

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memoryuniverse.core.database.dao.MemoryWithMetadata
import com.memoryuniverse.core.permissions.PermissionManager
import com.memoryuniverse.core.theme.AstralBlue
import com.memoryuniverse.core.theme.CosmicViolet
import com.memoryuniverse.core.theme.CosmicVioletDark
import com.memoryuniverse.core.theme.DarkNebula
import com.memoryuniverse.core.theme.DeepVoid
import com.memoryuniverse.core.theme.GlassBorder
import com.memoryuniverse.core.theme.SpaceBlack
import com.memoryuniverse.core.theme.StarlightMuted
import com.memoryuniverse.core.theme.StarlightSecondary
import com.memoryuniverse.core.theme.StarlightWhite
import com.memoryuniverse.core.theme.SupernovaRose
import com.memoryuniverse.ui.components.CosmicCard
import com.memoryuniverse.ui.components.CosmicMetricChip
import com.memoryuniverse.ui.components.StarfieldBackground
import java.util.Date

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToUniverse: () -> Unit,
    onPhotoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Modern permission launcher supporting Android 14+ granular access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        viewModel.onPermissionResult(granted)
    }

    Box(modifier = modifier.fillMaxSize().background(SpaceBlack)) {
        // Deep space starfield canvas background
        StarfieldBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Cosmic Top Header
            CosmicHeader(
                onRefresh = { viewModel.syncPhotos() },
                isSyncing = uiState.isSyncing
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CosmicMetricChip(
                    label = "Memories",
                    value = uiState.totalCount.toString(),
                    icon = Icons.Default.Image,
                    accentColor = CosmicViolet
                )
                CosmicMetricChip(
                    label = "Photos",
                    value = uiState.photoCount.toString(),
                    icon = Icons.Default.Image,
                    accentColor = AstralBlue
                )
                CosmicMetricChip(
                    label = "Galaxies",
                    value = uiState.clusterCount.toString(),
                    icon = Icons.Default.ViewInAr,
                    accentColor = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status message toast
            AnimatedVisibility(
                visible = uiState.statusMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.statusMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x338B5CF6))
                            .border(1.dp, CosmicViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = msg,
                            color = StarlightWhite,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Main Content: Permission Banner OR Indexed Photos Grid
            if (!uiState.hasPermission) {
                PermissionRationaleBanner(
                    onRequestPermission = {
                        permissionLauncher.launch(PermissionManager.getRequiredPhotoPermissions())
                    }
                )
            } else {
                if (uiState.memories.isEmpty()) {
                    EmptyUniverseBanner(
                        isSyncing = uiState.isSyncing,
                        onSync = { viewModel.syncPhotos() }
                    )
                } else {
                    PhotoUniverseGrid(
                        memories = uiState.memories,
                        onPhotoClick = onPhotoClick,
                        onToggleFavorite = { id, current -> viewModel.toggleFavorite(id, current) }
                    )
                }
            }
        }

        // Floating "Enter 3D Universe" Button at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Button(
                onClick = onNavigateToUniverse,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicViolet,
                    contentColor = StarlightWhite
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, AstralBlue.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Launch 3D Universe",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun CosmicHeader(
    onRefresh: () -> Unit,
    isSyncing: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(CosmicViolet)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MEMORY UNIVERSE",
                    color = StarlightWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Text(
                text = "Offline Neural Memory Cosmos",
                color = StarlightMuted,
                fontSize = 12.sp
            )
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x221E293B))
                .border(1.dp, GlassBorder, CircleShape)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AstralBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync",
                    tint = AstralBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionRationaleBanner(
    onRequestPermission: () -> Unit
) {
    CosmicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        borderColor = CosmicViolet.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = AstralBlue,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "100% Offline & Private",
                color = StarlightWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Memory Universe analyzes your personal photos strictly on your phone. No internet access, no servers, and zero cloud uploads. Grant photo access to project your memories into 3D space.",
                color = StarlightSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicViolet,
                    contentColor = StarlightWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Grant Local Photo Access",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyUniverseBanner(
    isSyncing: Boolean,
    onSync: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Deep Space is Silent",
                color = StarlightWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No photographs have been indexed into the cosmos yet. Tap below to scan your device gallery.",
                color = StarlightMuted,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSync,
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = CosmicVioletDark)
            ) {
                Text(if (isSyncing) "Scanning MediaStore..." else "Scan Device Photos")
            }
        }
    }
}

@Composable
private fun PhotoUniverseGrid(
    memories: List<MemoryWithMetadata>,
    onPhotoClick: (Long) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(memories, key = { it.id }) { memory ->
            MemoryCard(
                memory = memory,
                onClick = { onPhotoClick(memory.id) },
                onToggleFavorite = { onToggleFavorite(memory.id, memory.isFavorite) }
            )
        }
    }
}

@Composable
private fun MemoryCard(
    memory: MemoryWithMetadata,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val dateString = remember(memory.timestamp) {
        DateFormat.format("MMM d, yyyy", Date(memory.timestamp)).toString()
    }

    CosmicCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f),
        borderColor = if (memory.isFavorite) SupernovaRose.copy(alpha = 0.5f) else GlassBorder,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Coil async thumbnail loading from local content Uri
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(memory.mediaUri)
                    .crossfade(true)
                    .build(),
                contentDescription = memory.displayName ?: "Photo memory",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Scrim gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x66000000),
                                Color(0xE6050508)
                            ),
                            startY = 120f
                        )
                    )
            )

            // Favorite Button (Top right)
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = if (memory.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (memory.isFavorite) SupernovaRose else StarlightWhite,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Location badge if EXIF coordinates present (Top left)
            if (memory.latitude != null && memory.longitude != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "GPS",
                        tint = AstralBlue,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "GPS",
                        color = StarlightWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom metadata info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = memory.title ?: memory.displayName ?: "Untitled Memory",
                    color = StarlightWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateString,
                        color = StarlightSecondary,
                        fontSize = 10.sp
                    )
                    if (memory.posX != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "3D (${memory.posX.toInt()}, ${memory.posZ?.toInt()})",
                            color = CosmicViolet,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
