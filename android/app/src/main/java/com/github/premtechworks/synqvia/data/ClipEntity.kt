package com.github.premtechworks.synqvia.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clips",
    indices = [Index(value = ["ts"], orders = [Index.Order.DESC], name = "idx_clips_ts")]
)
data class ClipEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "ts")
    val ts: Long,

    @ColumnInfo(name = "src")
    val src: String,

    @ColumnInfo(name = "direction")
    val direction: String, // "local" or "remote"

    @ColumnInfo(name = "conflict_loser")
    val conflictLoser: Boolean = false,

    @ColumnInfo(name = "pinned", defaultValue = "0")
    val pinned: Boolean = false,

    @ColumnInfo(name = "sensitive", defaultValue = "0")
    val sensitive: Boolean = false
) {
    val isLocal: Boolean get() = direction == "local"
    val isRemote: Boolean get() = direction == "remote"
}
