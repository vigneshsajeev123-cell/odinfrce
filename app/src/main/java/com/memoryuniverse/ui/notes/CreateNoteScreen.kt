package com.memoryuniverse.ui.notes

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memoryuniverse.core.theme.AstralBlue
import com.memoryuniverse.core.theme.CosmicViolet
import com.memoryuniverse.core.theme.GlassBorder
import com.memoryuniverse.core.theme.SpaceBlack
import com.memoryuniverse.core.theme.StarlightMuted
import com.memoryuniverse.core.theme.StarlightSecondary
import com.memoryuniverse.core.theme.StarlightWhite
import com.memoryuniverse.core.theme.StellarAmber
import com.memoryuniverse.ui.components.StarfieldBackground

@Composable
fun CreateNoteScreen(
    viewModel: CreateNoteViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(SpaceBlack)) {
        StarfieldBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
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

                Text(
                    text = if (uiState.isAttachedNote) "Attach Photo Note" else "New Celestial Note",
                    color = StarlightWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Save Button
                IconButton(
                    onClick = { viewModel.saveNote(onComplete = onNavigateBack) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(StellarAmber.copy(alpha = 0.2f))
                        .border(1.dp, StellarAmber.copy(alpha = 0.6f), CircleShape)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = StellarAmber,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = StellarAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Photo Attachment Pill if attached note
            if (uiState.isAttachedNote) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, StellarAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = StellarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Orbiting: ${uiState.parentPhotoTitle ?: "Photo #${uiState.parentPhotoId}"}",
                        color = StarlightWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Cluster Selector Chips
            if (!uiState.isAttachedNote && uiState.clusters.isNotEmpty()) {
                Text(
                    text = "Assign Galaxy Cluster",
                    color = StarlightMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.clusters) { cluster ->
                        val isSelected = uiState.selectedClusterId == cluster.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) CosmicViolet.copy(alpha = 0.3f) else Color(0x331E293B))
                                .border(
                                    1.dp,
                                    if (isSelected) CosmicViolet else GlassBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.onClusterSelected(cluster.id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cluster.name,
                                color = if (isSelected) StarlightWhite else StarlightSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Note Title Input
            BasicTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                textStyle = TextStyle(
                    color = StarlightWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(StellarAmber),
                decorationBox = { innerTextField ->
                    Box {
                        if (uiState.title.isEmpty()) {
                            Text(
                                text = "Note Title...",
                                color = StarlightMuted,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Note Content Multiline Input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x66080812))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = uiState.content,
                    onValueChange = { viewModel.onContentChange(it) },
                    textStyle = TextStyle(
                        color = StarlightSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(StellarAmber),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (uiState.content.isEmpty()) {
                                Text(
                                    text = "Record memories, reflections, or descriptions to float in the universe...",
                                    color = StarlightMuted,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer word & char count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${uiState.content.length} characters",
                    color = StarlightMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "Encrypted Local SQLite Persistence",
                    color = StarlightMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
