package com.github.premtechworks.synqvia.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import com.github.premtechworks.synqvia.ui.components.ScreenScaffold
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.premtechworks.synqvia.ui.motion.shimmerHighlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premtechworks.synqvia.data.ClipEntity
import com.github.premtechworks.synqvia.data.SyncStats
import com.github.premtechworks.synqvia.ui.ClipFilter
import com.github.premtechworks.synqvia.ui.MainTab
import com.github.premtechworks.synqvia.ui.MainViewModel
import com.github.premtechworks.synqvia.ui.components.CircleIconButton
import com.github.premtechworks.synqvia.ui.components.ClipDetailSheet
import com.github.premtechworks.synqvia.ui.components.ClipHistoryItem
import com.github.premtechworks.synqvia.ui.components.SynqviaCard
import com.github.premtechworks.synqvia.ui.theme.AccentRed
import com.github.premtechworks.synqvia.ui.theme.BgBottom
import com.github.premtechworks.synqvia.ui.theme.BgTop
import com.github.premtechworks.synqvia.ui.theme.DividerDark
import com.github.premtechworks.synqvia.ui.theme.OnPrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.OutlineDark
import com.github.premtechworks.synqvia.ui.theme.PrimaryCyan
import com.github.premtechworks.synqvia.ui.theme.SurfaceDark
import com.github.premtechworks.synqvia.ui.theme.SurfaceHigh
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.theme.TextPrimary
import com.github.premtechworks.synqvia.ui.theme.TextSecondary
import com.github.premtechworks.synqvia.ui.theme.TextTertiary
import com.github.premtechworks.synqvia.ui.util.DateGroup
import com.github.premtechworks.synqvia.ui.util.groupClipsByDate
import java.time.LocalDate

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.LocalHazeState
import com.github.premtechworks.synqvia.ui.motion.RollingNumber
import com.github.premtechworks.synqvia.ui.motion.entryStagger
import com.github.premtechworks.synqvia.ui.motion.pressable

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    onNavigateTab: (MainTab) -> Unit = { viewModel.setTab(it) }
) {
    val clips by viewModel.filteredClips.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val stats by viewModel.syncStats.collectAsState()
    val isLoading by viewModel.isClipsLoading.collectAsState()
    val context = LocalContext.current
    val hazeState = LocalHazeState.current

    var localSelectedClip by remember { mutableStateOf<ClipEntity?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Fallback Detail Sheet Dialog for previews / non-haze contexts
    if (hazeState == null) {
        localSelectedClip?.let { detailClip ->
            val activeClip = clips.find { it.id == detailClip.id } ?: detailClip
            ClipDetailSheet(
                clip = activeClip,
                onDismiss = { localSelectedClip = null },
                onCopy = { text -> viewModel.copyToClipboardOnly(text) },
                onShare = { text ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share clip"))
                },
                onTogglePin = { id, pinned -> viewModel.togglePin(id, pinned) },
                onDelete = { id -> viewModel.deleteClip(id) }
            )
        }
    }

    // Clear confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Clipboard History?",
                    style = SynqviaType.Headline,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "All local synced history records will be permanently removed. This does not erase text from PC history.",
                    style = SynqviaType.Body,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear All", color = AccentRed, style = SynqviaType.Button.copy(color = AccentRed))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary, style = SynqviaType.Button)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    HistoryContent(
        clips = clips,
        searchQuery = searchQuery,
        currentFilter = currentFilter,
        totalCount = stats.totalCount,
        isLoading = isLoading,
        onRefresh = { viewModel.syncNow() },
        lazyListState = lazyListState,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onFilterChange = { viewModel.setFilter(it) },
        onBack = { onNavigateTab(MainTab.SYNC) },
        onClipClick = {
            if (hazeState != null) {
                viewModel.selectClipForDetail(it)
            } else {
                localSelectedClip = it
            }
        },
        onTogglePin = { clip -> viewModel.togglePin(clip.id, !clip.pinned) },
        onCopy = { clip -> viewModel.copyToClipboardOnly(clip.text) },
        onShare = { clip ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, clip.text)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share clip"))
        },
        onResend = { clip -> viewModel.resendClip(clip) },
        onDelete = { clip -> viewModel.deleteClip(clip.id) },
        onRequestClearHistory = { showClearConfirmDialog = true },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    clips: List<ClipEntity>,
    searchQuery: String,
    currentFilter: ClipFilter,
    totalCount: Int,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ClipFilter) -> Unit,
    onBack: () -> Unit,
    onClipClick: (ClipEntity) -> Unit,
    onTogglePin: (ClipEntity) -> Unit,
    onCopy: (ClipEntity) -> Unit,
    onShare: (ClipEntity) -> Unit,
    onResend: (ClipEntity) -> Unit,
    onDelete: (ClipEntity) -> Unit,
    onRequestClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    initialSearchMode: Boolean = false
) {
    var isSearchActive by remember { mutableStateOf(initialSearchMode || searchQuery.isNotEmpty()) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val dateGroups = remember(clips) { groupClipsByDate(clips) }

    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 8
        }
    }

    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val haptics = LocalAppHaptics.current
    val coroutineScope = rememberCoroutineScope()

    ScreenScaffold(
        modifier = modifier.testTag("history_screen"),
        isScrolled = isScrolled,
        header = {
            HistoryTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onEnterSearch = { isSearchActive = true },
                onExitSearch = {
                    isSearchActive = false
                    onSearchQueryChange("")
                    keyboardController?.hide()
                },
                onBack = onBack
            )
        }
    ) { contentPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                onRefresh()
                coroutineScope.launch {
                    delay(1200L)
                    isRefreshing = false
                    haptics.confirm()
                }
            },
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = contentPadding.calculateTopPadding()),
                    containerColor = SurfaceHigh,
                    color = PrimaryCyan
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Crossfade(
                targetState = isLoading,
                animationSpec = tween(200),
                label = "history_loading_crossfade"
            ) { loading ->
                if (loading) {
                    HistorySkeletonList(contentPadding = contentPadding)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState,
                        contentPadding = contentPadding
                    ) {
            // ==========================================
            // A) FILTER CHIPS ROW
            // ==========================================
            item(key = "history_filter_chips") {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .entryStagger(index = 0),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val countToShow = if (totalCount > 0) totalCount else clips.size

                    item {
                        HistoryFilterPill(
                            label = "All",
                            count = countToShow,
                            selected = currentFilter == ClipFilter.ALL,
                            onClick = { onFilterChange(ClipFilter.ALL) }
                        )
                    }
                    item {
                        HistoryFilterPill(
                            label = "Sent",
                            selected = currentFilter == ClipFilter.SENT,
                            onClick = { onFilterChange(ClipFilter.SENT) }
                        )
                    }
                    item {
                        HistoryFilterPill(
                            label = "Received",
                            selected = currentFilter == ClipFilter.RECEIVED,
                            onClick = { onFilterChange(ClipFilter.RECEIVED) }
                        )
                    }
                    item {
                        HistoryFilterPill(
                            label = "Pinned",
                            selected = currentFilter == ClipFilter.PINNED,
                            onClick = { onFilterChange(ClipFilter.PINNED) }
                        )
                    }
                }
            }

            // ==========================================
            // B) DATE-GROUPED LIST OR EMPTY STATE
            // ==========================================
            if (clips.isEmpty()) {
                item(key = "history_empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No clips match your search" else "No clips yet",
                                style = SynqviaType.Headline,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                dateGroups.forEachIndexed { groupIndex, group ->
                    stickyHeader(key = "header_${group.header}_${group.date}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgTop)
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = group.header,
                                style = SynqviaType.FootnoteSemiBold,
                                color = TextSecondary
                            )
                        }
                    }

                    item(key = "card_${group.header}_${group.date}") {
                        SynqviaCard(
                            shape = RoundedCornerShape(16.dp),
                            padding = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .entryStagger(index = groupIndex + 1)
                                .animateItem()
                        ) {
                            group.clips.forEachIndexed { index, clip ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(DividerDark)
                                    )
                                }
                                ClipHistoryItem(
                                    clip = clip,
                                    searchQuery = searchQuery,
                                    onClick = { onClipClick(clip) },
                                    onTogglePin = { onTogglePin(clip) },
                                    onCopy = { onCopy(clip) },
                                    onShare = { onShare(clip) },
                                    onResend = { onResend(clip) },
                                    onDelete = { onDelete(clip) }
                                )
                            }
                        }
                    }
                }

                // Centered "Clear all history" button at the bottom of the list
                item(key = "clear_all_history_footer") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clear all history",
                            style = SynqviaType.ButtonSmall.copy(color = AccentRed),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    role = Role.Button,
                                    onClick = onRequestClearHistory
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
}
}
}


@Composable
private fun HistorySkeletonList(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date header placeholder
        Box(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 4.dp)
                .size(width = 80.dp, height = 14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceHigh)
                .shimmerHighlight()
        )

        // 3 shimmering placeholder cards
        repeat(3) {
            SynqviaCard(
                shape = RoundedCornerShape(16.dp),
                padding = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shimmerHighlight()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 18.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(SurfaceHigh)
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceHigh)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceHigh)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceHigh)
                    )
                }
            }
        }
    }
}

/**
 * Top bar handling both normal title state and inline search state.
 * Fixed 48dp height to match shared header rule.
 */
@Composable
private fun HistoryTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEnterSearch: () -> Unit,
    onExitSearch: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isSearchActive) {
            // Normal top bar: 40dp circle back button + "Clipboard History" + 40dp circle search button
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = "Navigate to Sync tab"
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Clipboard History",
                style = SynqviaType.LargeTitle,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            CircleIconButton(
                icon = Icons.Default.Search,
                onClick = onEnterSearch,
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = "Search history"
            )
        } else {
            // Search mode: 40dp back/exit button + inline search field + close (X) button
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onExitSearch,
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = "Exit search"
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceHigh)
                    .border(1.dp, OutlineDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search clipboard history…",
                        style = SynqviaType.Body,
                        color = TextSecondary
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = SynqviaType.Body.copy(color = TextPrimary),
                    cursorBrush = SolidColor(PrimaryCyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_history_input")
                )
            }
            if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                CircleIconButton(
                    icon = Icons.Default.Close,
                    onClick = { onSearchQueryChange("") },
                    size = 36.dp,
                    iconSize = 18.dp,
                    contentDescription = "Clear search query"
                )
            }
        }
    }
}

/**
 * Filter pill: 34dp height, fully rounded, cyan when selected, surfaceHigh when unselected.
 */
@Composable
private fun HistoryFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    count: Int? = null
) {
    val haptics = LocalAppHaptics.current

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) PrimaryCyan else SurfaceHigh)
            .border(
                width = 1.dp,
                color = if (selected) PrimaryCyan else OutlineDark,
                shape = RoundedCornerShape(17.dp)
            )
            .pressable(targetScale = 0.96f) {
                haptics.tick()
                onClick()
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        val textColor = if (selected) OnPrimaryCyan else TextSecondary
        val textStyle = if (selected) SynqviaType.ChipSelected.copy(color = textColor) else SynqviaType.Chip.copy(color = textColor)

        if (count != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$label (",
                    style = textStyle
                )
                RollingNumber(
                    value = count,
                    style = textStyle,
                    color = textColor
                )
                Text(
                    text = ")",
                    style = textStyle
                )
            }
        } else {
            Text(
                text = label,
                style = textStyle
            )
        }
    }
}

// ==========================================
// Previews
// ==========================================

@Preview(name = "History Screen - 3 Date Groups", showBackground = true)
@Composable
fun PreviewHistoryScreenList() {
    val now = System.currentTimeMillis()
    val sampleClips = listOf(
        ClipEntity(
            id = "1",
            text = "build the debug app and install it on connected device.",
            ts = now - 22 * 60 * 1000,
            src = "prem-pc",
            direction = "remote",
            pinned = true
        ),
        ClipEntity(
            id = "2",
            text = "sudo systemctl status bluetooth",
            ts = now - 2 * 60 * 60 * 1000,
            src = "android",
            direction = "local",
            pinned = false
        ),
        ClipEntity(
            id = "3",
            text = "",
            ts = now - 26 * 60 * 60 * 1000, // Yesterday
            src = "android",
            direction = "local",
            pinned = false
        ),
        ClipEntity(
            id = "4",
            text = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0...",
            ts = now - 4 * 24 * 60 * 60 * 1000, // 4 days ago
            src = "prem-pc",
            direction = "remote",
            sensitive = true
        )
    )

    SynqviaTheme {
        HistoryContent(
            clips = sampleClips,
            searchQuery = "",
            currentFilter = ClipFilter.ALL,
            totalCount = 302,
            onSearchQueryChange = {},
            onFilterChange = {},
            onBack = {},
            onClipClick = {},
            onTogglePin = {},
            onCopy = {},
            onShare = {},
            onResend = {},
            onDelete = {},
            onRequestClearHistory = {}
        )
    }
}

@Preview(name = "History Screen - Search Mode", showBackground = true)
@Composable
fun PreviewHistoryScreenSearchMode() {
    SynqviaTheme {
        HistoryContent(
            clips = emptyList(),
            searchQuery = "bluetooth",
            currentFilter = ClipFilter.ALL,
            totalCount = 302,
            onSearchQueryChange = {},
            onFilterChange = {},
            onBack = {},
            onClipClick = {},
            onTogglePin = {},
            onCopy = {},
            onShare = {},
            onResend = {},
            onDelete = {},
            onRequestClearHistory = {},
            initialSearchMode = true
        )
    }
}
