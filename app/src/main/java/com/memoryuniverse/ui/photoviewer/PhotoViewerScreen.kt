package com.memoryuniverse.ui.photoviewer

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.theme.AstralBlue
import com.memoryuniverse.core.theme.CosmicViolet
import com.memoryuniverse.core.theme.GlassBorder
import com.memoryuniverse.core.theme.SpaceBlack
import com.memoryuniverse.core.theme.StarlightMuted
import com.memoryuniverse.core.theme.StarlightSecondary
import com.memoryuniverse.core.theme.StarlightWhite
import com.memoryuniverse.core.theme.StellarAmber
import com.memoryuniverse.core.theme.SupernovaRose
import com.memoryuniverse.ui.components.CosmicCard
import java.util.Date

@Composable
fun PhotoViewerScreen(
    memoryId: Long,
    viewModel: PhotoViewerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(memoryId) {
        viewModel.loadMemory(memoryId)
    }

    Box(modifier = modifier.fillMaxSize().background(SpaceBlack)) {
        if (uiState.isLoading || uiState.memory == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CosmicViolet)
            }
        } else {
            val memory = uiState.memory!!
            val dateStr = remember(memory.timestamp) {
                DateFormat.format("MMMM d, yyyy • h:mm a", Date(memory.timestamp)).toString()
            }

            // Main Photo Display
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(memory.mediaUri)
                    .crossfade(true)
                    .build(),
                contentDescription = memory.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Top Scrim & Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xE6050508), Color.Transparent)
                        )
                    )
                    .padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(1.dp, GlassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = StarlightWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = memory.title ?: "Memory #$memoryId",
                            color = StarlightWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = dateStr,
                            color = StarlightSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .border(1.dp, GlassBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (uiState.isFavorite) SupernovaRose else StarlightWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleExifSheet() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .border(1.dp, GlassBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "EXIF",
                                tint = AstralBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Attached Notes Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                CosmicCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = StellarAmber.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Note,
                                    contentDescription = null,
                                    tint = StellarAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Attached Notes (${uiState.attachedNotes.size})",
                                    color = StarlightWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { viewModel.showAddNoteDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = StellarAmber.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = StellarAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Attach Note", color = StellarAmber, fontSize = 11.sp)
                            }
                        }

                        if (uiState.attachedNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyColumn(
                                modifier = Modifier.height(120.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.attachedNotes, key = { it.id }) { note ->
                                    AttachedNoteItem(
                                        note = note,
                                        onDelete = { viewModel.deleteAttachedNote(note.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // EXIF Info Dialog
            if (uiState.showExifSheet) {
                AlertDialog(
                    onDismissRequest = { viewModel.toggleExifSheet() },
                    title = {
                        Text("EXIF Metadata", color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Timestamp: $dateStr", color = StarlightSecondary, fontSize = 12.sp)
                            if (memory.latitude != null && memory.longitude != null) {
                                Text(
                                    "GPS: ${memory.latitude}°, ${memory.longitude}°",
                                    color = AstralBlue,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text("GPS: Not available in EXIF", color = StarlightMuted, fontSize = 12.sp)
                            }
                            Text("URI: ${memory.mediaUri}", color = StarlightMuted, fontSize = 10.sp, maxLines = 2)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.toggleExifSheet() }) {
                            Text("Close", color = CosmicViolet)
                        }
                    },
                    containerColor = Color(0xFF0F0F1A)
                )
            }

            // Add Attached Note Dialog
            if (uiState.showAddNoteDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissAddNoteDialog() },
                    title = {
                        Text("Attach Note to Photo", color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text(
                                "This note will orbit directly alongside this photograph in the 3D Universe.",
                                color = StarlightMuted,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.newNoteText,
                                onValueChange = { viewModel.onNewNoteTextChange(it) },
                                placeholder = { Text("Write your thoughts or memories...", color = StarlightMuted) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.addAttachedNote() },
                            colors = ButtonDefaults.buttonColors(containerColor = StellarAmber)
                        ) {
                            Text("Attach", color = SpaceBlack)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissAddNoteDialog() }) {
                            Text("Cancel", color = StarlightMuted)
                        }
                    },
                    containerColor = Color(0xFF0F0F1A)
                )
            }
        }
    }
}

@Composable
private fun AttachedNoteItem(
    note: MemoryEntity,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x331E293B))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.textContent ?: "",
                color = StarlightWhite,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = StarlightMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
