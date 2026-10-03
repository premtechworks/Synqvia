package com.github.premtechworks.synqvia.ui.screens

import androidx.compose.runtime.Immutable
import com.github.premtechworks.synqvia.data.ClipEntity
import java.time.LocalDate

@Immutable
data class HistoryItemUi(
    val clip: ClipEntity,
    val formattedTime: String,
    val urlHost: String? = null
)

@Immutable
data class HistoryGroupUi(
    val header: String,
    val date: LocalDate,
    val items: List<HistoryItemUi>
)

@Immutable
data class HistoryUiState(
    val groups: List<HistoryGroupUi> = emptyList(),
    val rawClips: List<ClipEntity> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val isCached: Boolean = false
)
