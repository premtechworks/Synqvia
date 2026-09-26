package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClipEntity
import com.example.ui.GlassPillBadge
import com.example.ui.LiquidGlassCard
import com.example.ui.theme.ConflictLoserColor
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalSendColor
import com.example.ui.theme.RemoteRecvColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClipHistoryItem(
    clip: ClipEntity,
    onCopyAndResend: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isLongText = clip.text.length > 120

    val timeFormatted = remember(clip.ts) {
        val diffSec = (System.currentTimeMillis() - clip.ts) / 1000
        when {
            diffSec < 60 -> "Just now"
            diffSec < 3600 -> "${diffSec / 60}m ago"
            diffSec < 86400 -> "${diffSec / 3600}h ago"
            else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(clip.ts))
        }
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("clip_item_${clip.id.take(8)}")
            .semantics { contentDescription = "Clip: ${clip.direction}, text: ${clip.text.take(40)}" }
            .clickable { onCopyAndResend() },
        backgroundColor = GlassSurface,
        borderColor = if (clip.conflictLoser) ConflictLoserColor.copy(alpha = 0.4f) else GlassBorderSubtle,
        elevation = 3.dp,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Header: Direction Badge + Timestamp + Conflict Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (clip.isLocal) {
                        GlassPillBadge(
                            text = "▶ Sent to PC",
                            accentColor = LocalSendColor,
                            textColor = LocalSendColor
                        )
                    } else {
                        GlassPillBadge(
                            text = "◀ From PC",
                            accentColor = RemoteRecvColor,
                            textColor = RemoteRecvColor
                        )
                    }

                    if (clip.conflictLoser) {
                        GlassPillBadge(
                            text = "⚠ Conflict Loser",
                            accentColor = ConflictLoserColor,
                            textColor = ConflictLoserColor
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clip Text Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x14000000))
                    .padding(12.dp)
            ) {
                if (clip.text.isEmpty()) {
                    Text(
                        text = "(empty clipboard)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Light
                        ),
                        color = Color(0xFF64748B)
                    )
                } else {
                    Text(
                        text = clip.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Default,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color(0xFFE2E8F0),
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Character count & Actions row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${clip.text.length} chars • src: ${clip.src.take(16)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )

                    if (isLongText) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { expanded = !expanded }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse text" else "Expand text",
                                tint = CyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Interactive action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCopyAndResend,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("resend_clip_${clip.id.take(8)}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy and resend clip to PC",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("share_clip_${clip.id.take(8)}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share clip via system dialog",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_clip_${clip.id.take(8)}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete clip from history",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
