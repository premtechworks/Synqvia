package com.github.premtechworks.synqvia.ui.support

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Link
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.premtechworks.synqvia.R

data class FaqItem(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val lightTileBg: Color,
    val darkTileBg: Color,
    val lightTileFg: Color,
    val darkTileFg: Color,
    @ArrayRes val stepsRes: Int,
    @StringRes val actionButtonTextRes: Int? = null,
    val actionUrl: String? = null
)

object FaqData {
    val ITEMS: List<FaqItem> = listOf(
        FaqItem(
            id = "how_to_pair",
            titleRes = R.string.faq_q1_title,
            icon = Icons.Outlined.Link,
            lightTileBg = Color(0xFFE3F6EC),
            darkTileBg = Color(0xFF1A9B5A).copy(alpha = 0.14f),
            lightTileFg = Color(0xFF1A9B5A),
            darkTileFg = Color(0xFF1A9B5A),
            stepsRes = R.array.faq_q1_steps
        ),
        FaqItem(
            id = "clipboard_sync",
            titleRes = R.string.faq_q2_title,
            icon = Icons.Outlined.Keyboard,
            lightTileBg = Color(0xFFE3EEFD),
            darkTileBg = Color(0xFF007AFF).copy(alpha = 0.14f),
            lightTileFg = Color(0xFF007AFF),
            darkTileFg = Color(0xFF007AFF),
            stepsRes = R.array.faq_q2_steps
        ),
        FaqItem(
            id = "battery_optimization",
            titleRes = R.string.faq_q3_title,
            icon = Icons.Outlined.BatteryChargingFull,
            lightTileBg = Color(0xFFEDE8FD),
            darkTileBg = Color(0xFF6D4AE0).copy(alpha = 0.14f),
            lightTileFg = Color(0xFF6D4AE0),
            darkTileFg = Color(0xFF6D4AE0),
            stepsRes = R.array.faq_q3_steps
        ),
        FaqItem(
            id = "bluetooth_problems",
            titleRes = R.string.faq_q4_title,
            icon = Icons.Outlined.Bluetooth,
            lightTileBg = Color(0xFFFDECE6),
            darkTileBg = Color(0xFFE5593D).copy(alpha = 0.14f),
            lightTileFg = Color(0xFFE5593D),
            darkTileFg = Color(0xFFE5593D),
            stepsRes = R.array.faq_q4_steps
        ),
        FaqItem(
            id = "contribute",
            titleRes = R.string.faq_q5_title,
            icon = Icons.Outlined.Code,
            lightTileBg = Color(0xFFE3EEFD),
            darkTileBg = Color(0xFF007AFF).copy(alpha = 0.14f),
            lightTileFg = Color(0xFF007AFF),
            darkTileFg = Color(0xFF007AFF),
            stepsRes = R.array.faq_q5_steps,
            actionButtonTextRes = R.string.action_view_on_github,
            actionUrl = ContactLinks.GITHUB_URL
        )
    )
}
