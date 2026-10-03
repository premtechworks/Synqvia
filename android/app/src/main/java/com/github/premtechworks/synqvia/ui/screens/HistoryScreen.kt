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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.key
import com.github.premtechworks.synqvia.ui.motion.DecelerateEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import com.github.premtechworks.synqvia.ui.util.buildHistoryGroups
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
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
    val uiState by viewModel.historyUiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val context = LocalContext.current
    val hazeState = LocalHazeState.current

    var localSelectedClip by remember { mutableStateOf<ClipEntity?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Fallback Detail Sheet Dialog for previews / non-haze contexts
    if (hazeState == null) {
        localSelectedClip?.let { detailClip ->
            val activeClip = uiState.rawClips.find { it.id == detailClip.id } ?: detailClip
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
        val colors = SynqviaTheme.colors
        val clipCount = if (uiState.totalCount > 0) uiState.totalCount else uiState.rawClips.size
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear all history?",
                    style = SynqviaType.Headline,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This removes $clipCount clips from this phone. Clips on your PC are not affected.",
                    style = SynqviaType.Body,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear", color = colors.red, style = SynqviaType.Button.copy(color = colors.red))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary, style = SynqviaType.Button)
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary
        )
    }

    HistoryContent(
        clips = uiState.rawClips,
        groups = uiState.groups,
        searchQuery = searchQuery,
        currentFilter = currentFilter,
        totalCount = uiState.totalCount,
        isLoading = uiState.isLoading,
        isCached = uiState.isCached,
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
    groups: List<HistoryGroupUi> = emptyList(),
    isLoading: Boolean = false,
    isCached: Boolean = false,
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
    val activeGroups = if (groups.isNotEmpty() || clips.isEmpty()) {
        groups
    } else {
        remember(clips) { buildHistoryGroups(clips) }
    }

    var hasVisitedHistory by rememberSaveable { mutableStateOf(false) }
    val shouldStagger = !hasVisitedHistory
    LaunchedEffect(Unit) {
        hasVisitedHistory = true
    }

    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 8
        }
    }

    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val haptics = LocalAppHaptics.current
    val coroutineScope = rememberCoroutineScope()
    val colors = SynqviaTheme.colors

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
                onBack = onBack,
                onClearAll = onRequestClearHistory
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
                    containerColor = colors.surfaceHigh,
                    color = colors.primary
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading && !isCached) {
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
                                tint = colors.textTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No clips match your search" else "No clips yet",
                                style = SynqviaType.Headline,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                activeGroups.forEachIndexed { groupIndex, group ->
                    stickyHeader(key = "header_${group.header}_${group.date}", contentType = "date_header") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.bgTop)
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = group.header,
                                style = SynqviaType.FootnoteSemiBold,
                                color = colors.dateHeaderText
                            )
                        }
                    }

                    item(key = "card_${group.header}_${group.date}", contentType = "date_card") {
                        SynqviaCard(
                            shape = RoundedCornerShape(16.dp),
                            padding = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .entryStagger(index = groupIndex + 1, trigger = shouldStagger && groupIndex < 5)
                                .animateItem()
                        ) {
                            group.items.forEachIndexed { index, item ->
                                key(item.clip.id) {
                                    var isDismissed by remember(item.clip.id) { mutableStateOf(false) }

                                    AnimatedVisibility(
                                        visible = !isDismissed,
                                        enter = expandVertically(animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)),
                                        exit = shrinkVertically(
                                            animationSpec = tween(durationMillis = 220, easing = DecelerateEasing)
                                        ) + fadeOut(animationSpec = tween(durationMillis = 150))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            if (index > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                        .background(colors.divider)
                                                )
                                            }
                                            ClipHistoryItem(
                                                clip = item.clip,
                                                searchQuery = searchQuery,
                                                precomputedTime = item.formattedTime,
                                                precomputedHost = item.urlHost,
                                                onClick = { onClipClick(item.clip) },
                                                onTogglePin = {
                                                    if (currentFilter == ClipFilter.PINNED) {
                                                        isDismissed = true
                                                        coroutineScope.launch {
                                                            delay(220L)
                                                            onTogglePin(item.clip)
                                                        }
                                                    } else {
                                                        onTogglePin(item.clip)
                                                    }
                                                },
                                                onCopy = { onCopy(item.clip) },
                                                onShare = { onShare(item.clip) },
                                                onResend = { onResend(item.clip) },
                                                onDelete = {
                                                    isDismissed = true
                                                    coroutineScope.launch {
                                                        delay(220L)
                                                        onDelete(item.clip)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
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
                            style = SynqviaType.ButtonSmall.copy(color = colors.redText),
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


@Composable
private fun HistorySkeletonList(contentPadding: PaddingValues) {
    val colors = SynqviaTheme.colors
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
                .background(colors.surfaceHigh)
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
                                .background(colors.surfaceHigh)
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceHigh)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceHigh)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceHigh)
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
    onBack: () -> Unit,
    onClearAll: () -> Unit = {}
) {
    val colors = SynqviaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isSearchActive) {
            // Normal top bar: 40dp circle back button | "History" | 40dp circle search button | 40dp circle clear-all button
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = "Navigate to Sync tab"
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "History",
                style = SynqviaType.LargeTitle,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            CircleIconButton(
                icon = Icons.Default.Search,
                onClick = onEnterSearch,
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = "Search history"
            )
            Spacer(modifier = Modifier.width(8.dp))
            CircleIconButton(
                icon = Icons.Default.DeleteSweep,
                onClick = onClearAll,
                size = 40.dp,
                iconSize = 20.dp,
                tint = colors.red,
                contentDescription = "Clear all history"
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
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search clipboard history…",
                        style = SynqviaType.Body,
                        color = colors.textSecondary
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = SynqviaType.Body.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.primary),
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
 * Filter pill: 34dp height, fully rounded, primary when selected, chipUnselectedBg when unselected.
 */
@Composable
private fun HistoryFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    count: Int? = null
) {
    val haptics = LocalAppHaptics.current
    val colors = SynqviaTheme.colors
    val isDark = SynqviaTheme.isDark

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) colors.primary else colors.chipUnselectedBg)
            .border(
                width = 1.dp,
                color = if (isDark) {
                    if (selected) colors.primary else colors.outline
                } else {
                    if (selected) colors.primary else Color.Transparent
                },
                shape = RoundedCornerShape(17.dp)
            )
            .pressable(targetScale = 0.96f) {
                haptics.tick()
                onClick()
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        val textColor = if (selected) colors.onPrimary else colors.chipUnselectedText
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
