package com.memoryuniverse.ui.clusters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.MemoryType
import com.memoryuniverse.ui.theme.DarkSpaceBackground
import com.memoryuniverse.ui.theme.NeonBlue
import com.memoryuniverse.ui.theme.StarWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreClustersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMemory: (Long, MemoryType) -> Unit,
    viewModel: ClustersViewModel = viewModel()
) {
    val clusters by viewModel.clustersFlow.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showRenameDialogFor by remember { mutableStateOf<ClusterEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showMergeDialogFor by remember { mutableStateOf<ClusterEntity?>(null) }
    var selectedTargetClusterId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Galaxy Clusters",
                            color = StarWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${clusters.size} AI & Manual Communities",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StarWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runClustering(forceFullRebuild = false) },
                        enabled = !uiState.isClustering
                    ) {
                        if (uiState.isClustering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NeonBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Run AI Clustering",
                                tint = NeonBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSpaceBackground)
            )
        },
        containerColor = DarkSpaceBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats banner
            if (uiState.lastStats != null) {
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Modularity: ${"%.2f".format(uiState.lastStats!!.modularity)}",
                                color = StarWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${uiState.lastStats!!.totalMemories} memories clustered • ${uiState.lastStats!!.isolatedMemories} isolated stars",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Updated",
                            tint = Color(0xFF10B981)
                        )
                    }
                }
            }

            if (clusters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Galaxies Formed Yet",
                            color = StarWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add photos or notes, then tap the AI button above to automatically cluster related memories into cosmic galaxies.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.runClustering(forceFullRebuild = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                        ) {
                            Text("Discover Galaxies Now")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(clusters, key = { it.id }) { cluster ->
                        GalaxyClusterCard(
                            cluster = cluster,
                            onSelect = { viewModel.selectCluster(cluster) },
                            onRename = {
                                showRenameDialogFor = cluster
                                renameInputText = cluster.name
                            },
                            onMerge = {
                                showMergeDialogFor = cluster
                                selectedTargetClusterId = null
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet for Cluster Details
    if (uiState.selectedCluster != null) {
        val cluster = uiState.selectedCluster!!
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectCluster(null) },
            containerColor = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(parseColorSafe(cluster.colorHex))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = cluster.name,
                            color = StarWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (cluster.userRenamed) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Manual",
                                color = NeonBlue,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${uiState.selectedClusterMemories.size} memories orbiting in this galaxy",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedClusterMemories, key = { it.id }) { mem ->
                        ClusterMemoryRow(
                            memory = mem,
                            onOpen = { onNavigateToMemory(mem.id, mem.memoryType) },
                            onToggleLock = { viewModel.toggleLock(mem.id, mem.isLocked) },
                            onDetach = { viewModel.moveMemory(mem.id, null) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Rename Dialog
    if (showRenameDialogFor != null) {
        val cluster = showRenameDialogFor!!
        AlertDialog(
            onDismissRequest = { showRenameDialogFor = null },
            title = { Text("Rename Galaxy", color = StarWhite) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("Galaxy Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StarWhite,
                        unfocusedTextColor = StarWhite
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameCluster(cluster.id, renameInputText)
                        showRenameDialogFor = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialogFor = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Merge Dialog
    if (showMergeDialogFor != null) {
        val source = showMergeDialogFor!!
        val otherClusters = clusters.filter { it.id != source.id }

        AlertDialog(
            onDismissRequest = { showMergeDialogFor = null },
            title = { Text("Merge '${source.name}'", color = StarWhite) },
            text = {
                Column {
                    Text(
                        text = "Select a target galaxy to absorb all memories from '${source.name}':",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(otherClusters) { target ->
                            val isSelected = selectedTargetClusterId == target.id
                            Surface(
                                color = if (isSelected) Color(0xFF334155) else Color(0xFF0F172A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedTargetClusterId = target.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(parseColorSafe(target.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = target.name,
                                        color = StarWhite,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTargetClusterId != null) {
                            viewModel.mergeClusters(selectedTargetClusterId!!, source.id)
                            showMergeDialogFor = null
                        }
                    },
                    enabled = selectedTargetClusterId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialogFor = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun GalaxyClusterCard(
    cluster: ClusterEntity,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onMerge: () -> Unit
) {
    val haloColor = parseColorSafe(cluster.colorHex)

    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, haloColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(haloColor)
                )
                Text(
                    text = "${cluster.memoryCount} items",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = cluster.name,
                color = StarWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onMerge, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.CallMerge,
                        contentDescription = "Merge",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClusterMemoryRow(
    memory: MemoryEntity,
    onOpen: () -> Unit,
    onToggleLock: () -> Unit,
    onDetach: () -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when (memory.memoryType) {
                        MemoryType.PHOTO -> Icons.Default.Image
                        MemoryType.NOTE -> Icons.Default.Description
                        MemoryType.PHOTO_ATTACHED_NOTE -> Icons.Default.Attachment
                    },
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = memory.title ?: memory.textContent?.take(25) ?: "Photo #${memory.id}",
                        color = StarWhite,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (memory.isLocked) "Locked to cluster" else "AI grouped",
                        color = if (memory.isLocked) Color(0xFFF59E0B) else Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }

            Row {
                IconButton(onClick = onToggleLock, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (memory.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Toggle Lock",
                        tint = if (memory.isLocked) Color(0xFFF59E0B) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDetach, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Detach from cluster",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun parseColorSafe(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF8B5CF6)
    }
}
