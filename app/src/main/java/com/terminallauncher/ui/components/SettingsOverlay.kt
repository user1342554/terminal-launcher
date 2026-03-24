package com.terminallauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terminallauncher.ui.theme.Background
import com.terminallauncher.ui.theme.CopperLived
import com.terminallauncher.ui.theme.Monospace
import com.terminallauncher.ui.theme.TextDim
import com.terminallauncher.ui.theme.TextInput

@Composable
fun SettingsOverlay(
    birthYear: Int?,
    birthMonth: Int?,
    swipeLeftLabel: String?,
    swipeRightLabel: String?,
    wallpaperHome: Boolean,
    wallpaperLock: Boolean,
    onChangeBirthDate: () -> Unit,
    onToggleWallpaperHome: () -> Unit,
    onToggleWallpaperLock: () -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background.copy(alpha = 0.97f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp)
        ) {
            Text(
                text = "settings",
                color = CopperLived,
                fontFamily = Monospace,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            SettingsRow(
                label = "birth date",
                value = if (birthYear != null && birthMonth != null) "$birthYear / $birthMonth" else "not set",
                onClick = onChangeBirthDate
            )

            Spacer(Modifier.height(12.dp))

            SettingsRow(
                label = "swipe right app",
                value = swipeRightLabel ?: "not set",
                onClick = null
            )

            Spacer(Modifier.height(12.dp))

            SettingsRow(
                label = "swipe left app",
                value = swipeLeftLabel ?: "not set",
                onClick = null
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "wallpaper",
                color = CopperLived,
                fontFamily = Monospace,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ToggleRow(
                label = "home screen",
                enabled = wallpaperHome,
                onClick = onToggleWallpaperHome
            )

            Spacer(Modifier.height(8.dp))

            ToggleRow(
                label = "lock screen",
                enabled = wallpaperLock,
                onClick = onToggleWallpaperLock
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "[ reset all ]",
                color = Color(0xFFFF6B6B),
                fontFamily = Monospace,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onResetAll)
                    .padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "tap anywhere to close",
                color = TextDim,
                fontFamily = Monospace,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp)
    ) {
        Column {
            Text(text = label, color = TextInput, fontFamily = Monospace, fontSize = 14.sp)
            Text(text = value, color = TextDim, fontFamily = Monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ToggleRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = if (enabled) "[x]" else "[ ]",
            color = if (enabled) CopperLived else TextDim,
            fontFamily = Monospace,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = TextInput, fontFamily = Monospace, fontSize = 14.sp)
    }
}
