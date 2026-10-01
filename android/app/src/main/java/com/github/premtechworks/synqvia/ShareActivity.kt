package com.github.premtechworks.synqvia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.premtechworks.synqvia.service.ClipSyncService
import com.github.premtechworks.synqvia.ui.LiquidGlassCard
import com.github.premtechworks.synqvia.ui.theme.CyanPrimary
import com.github.premtechworks.synqvia.ui.theme.DarkNavySurface
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.StatusConnected
import kotlinx.coroutines.delay

class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        } else {
            ""
        }

        val app = application as? SynqviaApp
        val syncPreferences = app?.container?.syncPreferences ?: com.github.premtechworks.synqvia.data.SyncPreferences(this)

        if (sharedText.isNotBlank() && !syncPreferences.isUserStopped) {
            val serviceIntent = Intent(this, ClipSyncService::class.java).apply {
                action = ClipSyncService.ACTION_INJECT
                putExtra(ClipSyncService.EXTRA_TEXT, sharedText)
            }
            try {
                startService(serviceIntent)
            } catch (_: Exception) {}
        }

        setContent {
            SynqviaTheme {
                ShareOverlayDialog(
                    text = sharedText,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun ShareOverlayDialog(text: String, onDismiss: () -> Unit) {
    var sent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(600)
        sent = true
        delay(1000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x77000000))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkNavySurface,
            borderColor = if (sent) StatusConnected.copy(alpha = 0.6f) else CyanPrimary.copy(alpha = 0.6f),
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (sent) StatusConnected.copy(alpha = 0.2f) else CyanPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (sent) Icons.Default.Check else Icons.Default.Send,
                        contentDescription = null,
                        tint = if (sent) StatusConnected else CyanPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (sent) "Sent to Linux PC!" else "Broadcasting to PC...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                if (text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x18FFFFFF))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
