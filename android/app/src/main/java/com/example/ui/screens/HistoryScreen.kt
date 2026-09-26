package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.ClipFilter
import com.example.ui.LiquidGlassCard
import com.example.ui.MainViewModel
import com.example.ui.components.ClipHistoryItem
import com.example.ui.theme.ConflictLoserColor
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalSendColor
import com.example.ui.theme.RemoteRecvColor

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val clips by viewModel.filteredClips.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Clipboard History?") },
            text = { Text("All local synced history records will be permanently removed. This does not erase text from PC history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkNavySurface,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen")
            .padding(top = 16.dp)
    ) {
        // Search & Clear Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_history_input"),
                placeholder = { Text("Search clipboard history...", color = Color(0xFF64748B)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CyanPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search query",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = GlassSurface,
                    unfocusedContainerColor = GlassSurface,
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = GlassBorderSubtle,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showClearConfirmDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x18FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear all clipboard history",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = currentFilter == ClipFilter.ALL,
                    onClick = { viewModel.setFilter(ClipFilter.ALL) },
                    label = { Text("All (${clips.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyanPrimary,
                        containerColor = Color(0x0EFFFFFF),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = currentFilter == ClipFilter.ALL,
                        borderColor = GlassBorderSubtle,
                        selectedBorderColor = CyanPrimary
                    )
                )
            }

            item {
                FilterChip(
                    selected = currentFilter == ClipFilter.SENT,
                    onClick = { viewModel.setFilter(ClipFilter.SENT) },
                    label = { Text("▶ Sent") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LocalSendColor.copy(alpha = 0.2f),
                        selectedLabelColor = LocalSendColor,
                        containerColor = Color(0x0EFFFFFF),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = currentFilter == ClipFilter.SENT,
                        borderColor = GlassBorderSubtle,
                        selectedBorderColor = LocalSendColor
                    )
                )
            }

            item {
                FilterChip(
                    selected = currentFilter == ClipFilter.RECEIVED,
                    onClick = { viewModel.setFilter(ClipFilter.RECEIVED) },
                    label = { Text("◀ Received") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RemoteRecvColor.copy(alpha = 0.2f),
                        selectedLabelColor = RemoteRecvColor,
                        containerColor = Color(0x0EFFFFFF),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = currentFilter == ClipFilter.RECEIVED,
                        borderColor = GlassBorderSubtle,
                        selectedBorderColor = RemoteRecvColor
                    )
                )
            }

            item {
                FilterChip(
                    selected = currentFilter == ClipFilter.CONFLICTS,
                    onClick = { viewModel.setFilter(ClipFilter.CONFLICTS) },
                    label = { Text("⚠ Conflicts") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ConflictLoserColor.copy(alpha = 0.2f),
                        selectedLabelColor = ConflictLoserColor,
                        containerColor = Color(0x0EFFFFFF),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = currentFilter == ClipFilter.CONFLICTS,
                        borderColor = GlassBorderSubtle,
                        selectedBorderColor = ConflictLoserColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Clip Items List
        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = GlassSurface,
                    borderColor = GlassBorderSubtle
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No clips match \"$searchQuery\"" else "No clips recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copied text will automatically show up here",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(clips, key = { it.id }) { clip ->
                    ClipHistoryItem(
                        clip = clip,
                        onCopyAndResend = { viewModel.resendClip(clip) },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, clip.text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share clip"))
                        },
                        onDelete = { viewModel.deleteClip(clip.id) }
                    )
                }
            }
        }
    }
}
