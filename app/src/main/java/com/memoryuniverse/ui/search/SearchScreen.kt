package com.memoryuniverse.ui.search

import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sparkles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memoryuniverse.core.database.entity.MemoryType
import com.memoryuniverse.core.theme.AstralBlue
import com.memoryuniverse.core.theme.CosmicViolet
import com.memoryuniverse.core.theme.DarkNebula
import com.memoryuniverse.core.theme.GlassBorder
import com.memoryuniverse.core.theme.SpaceBlack
import com.memoryuniverse.core.theme.StarlightMuted
import com.memoryuniverse.core.theme.StarlightSecondary
import com.memoryuniverse.core.theme.StarlightWhite
import com.memoryuniverse.core.theme.StellarAmber
import com.memoryuniverse.ui.components.CosmicCard
import com.memoryuniverse.ui.components.StarfieldBackground
import java.util.Date

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onOpenPhotoViewer: (Long) -> Unit,
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

            // Search Bar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.width(10.dp))

                // Search Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC0D0D18))
                        .border(1.dp, CosmicViolet.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AstralBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = uiState.query,
                            onValueChange = { viewModel.onQueryChange(it) },
                            textStyle = TextStyle(
                                color = StarlightWhite,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(AstralBlue),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.weight(1f)) {
                                    if (uiState.query.isEmpty()) {
                                        Text(
                                            text = "Search by concepts, OCR, or notes...",
                                            color = StarlightMuted,
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.query.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onQueryChange("") },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = StarlightMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Status Bar & Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = AstralBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "512-dim On-Device Vector Space",
                        color = StarlightMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        label = "All",
                        isSelected = uiState.filter == SearchFilter.ALL,
                        onClick = { viewModel.onFilterChange(SearchFilter.ALL) }
                    )
                    FilterChip(
                        label = "Photos",
                        isSelected = uiState.filter == SearchFilter.PHOTOS,
                        onClick = { viewModel.onFilterChange(SearchFilter.PHOTOS) }
                    )
                    FilterChip(
                        label = "Notes",
                        isSelected = uiState.filter == SearchFilter.NOTES,
                        onClick = { viewModel.onFilterChange(SearchFilter.NOTES) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results List or Initial Suggestions
            if (uiState.isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AstralBlue, modifier = Modifier.size(32.dp))
                }
            } else if (uiState.query.isEmpty()) {
                SearchEmptyState(onSuggestionClick = { viewModel.onQueryChange(it) })
            } else if (uiState.results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No semantic memories found for \"${uiState.query}\"",
                        color = StarlightMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.results, key = { it.memory.id }) { result ->
                        SearchResultCard(
                            result = result,
                            onClick = {
                                if (result.memory.memoryType == MemoryType.PHOTO) {
                                    onOpenPhotoViewer(result.memory.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CosmicViolet.copy(alpha = 0.35f) else Color(0x331E293B))
            .border(1.dp, if (isSelected) CosmicViolet else GlassBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) StarlightWhite else StarlightMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResultItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val memory = result.memory
    val isPhoto = memory.memoryType == MemoryType.PHOTO
    val matchPct = (result.similarityScore * 100).toInt()

    val matchColor = when {
        matchPct >= 80 -> Color(0xFF10B981) // Emerald
        matchPct >= 50 -> AstralBlue
        else -> CosmicViolet
    }

    val dateStr = remember(memory.timestamp) {
        DateFormat.format("MMM d, yyyy", Date(memory.timestamp)).toString()
    }

    CosmicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        borderColor = if (matchPct >= 75) matchColor.copy(alpha = 0.5f) else GlassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Note Icon
            if (isPhoto) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
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
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, StellarAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = StellarAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Information Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = memory.title ?: (if (isPhoto) "Photo #${memory.id}" else "Note #${memory.id}"),
                        color = StarlightWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    // Semantic Match Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(matchColor.copy(alpha = 0.15f))
                            .border(1.dp, matchColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$matchPct% Match",
                            color = matchColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (!result.ocrSnippet.isNullOrBlank()) {
                    Text(
                        text = "OCR: \"${result.ocrSnippet}\"",
                        color = StarlightSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                } else if (!memory.textContent.isNullOrBlank()) {
                    Text(
                        text = memory.textContent,
                        color = StarlightSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        color = StarlightMuted,
                        fontSize = 10.sp
                    )
                    if (result.keywords.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${result.keywords.take(2).joinToString(", ")}",
                            color = CosmicViolet,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Sparkles,
            contentDescription = null,
            tint = AstralBlue,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "On-Device Multimodal Search",
            color = StarlightWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Search your photographs and notes semantically using local AI embeddings and on-device OCR.",
            color = StarlightMuted,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TRY SEARCHING",
            color = StarlightMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        val suggestions = listOf("Sunrise", "Mountains", "Campfire", "Night sky", "Trail", "Receipt", "Meeting", "Forest")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x331E293B))
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = suggestion, color = AstralBlue, fontSize = 11.sp)
                }
            }
        }
    }
}
