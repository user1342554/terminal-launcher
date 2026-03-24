package com.terminallauncher.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terminallauncher.data.MediaState
import com.terminallauncher.ui.theme.Background
import com.terminallauncher.ui.theme.CopperLived
import com.terminallauncher.ui.theme.DimRemaining
import com.terminallauncher.ui.theme.Monospace
import com.terminallauncher.ui.theme.TextDim
import com.terminallauncher.ui.theme.TextInput
import kotlinx.coroutines.delay

@Composable
fun WidgetScreen() {
    val context = LocalContext.current
    val nowPlaying by MediaState.nowPlaying.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            MediaState.refresh(context)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (nowPlaying.title.isEmpty() && nowPlaying.artist.isEmpty()) {
            NothingPlaying(context)
        } else {
            val accentColor = Color(nowPlaying.colors.accent)
            MusicWidget(
                title = nowPlaying.title,
                artist = nowPlaying.artist,
                isPlaying = nowPlaying.isPlaying,
                albumArt = nowPlaying.albumArt,
                accent = accentColor,
                onPrev = { MediaState.getController()?.transportControls?.skipToPrevious() },
                onPlayPause = {
                    val ctrl = MediaState.getController()?.transportControls
                    if (nowPlaying.isPlaying) ctrl?.pause() else ctrl?.play()
                },
                onNext = { MediaState.getController()?.transportControls?.skipToNext() },
                onOpenPlayer = {
                    val pkg = MediaState.getPlayerPackage()
                    if (pkg != null) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun NothingPlaying(context: Context) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 48.dp)
    ) {
        Text(
            text = "nothing playing",
            color = TextDim,
            fontFamily = Monospace,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "play music in any app",
            color = TextDim.copy(alpha = 0.5f),
            fontFamily = Monospace,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "[ grant notification access ]",
            color = CopperLived.copy(alpha = 0.6f),
            fontFamily = Monospace,
            fontSize = 12.sp,
            modifier = Modifier.clickable {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        )
    }
}

@Composable
private fun MusicWidget(
    title: String,
    artist: String,
    isPlaying: Boolean,
    albumArt: Bitmap?,
    accent: Color = CopperLived,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.Center)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DimRemaining)
                .clickable(onClick = onOpenPlayer),
            contentAlignment = Alignment.Center
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Title
        Text(
            text = title.ifEmpty { "unknown" },
            color = TextInput,
            fontFamily = Monospace,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        // Artist
        Text(
            text = artist.ifEmpty { "" },
            color = accent.copy(alpha = 0.7f),
            fontFamily = Monospace,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(36.dp))

        // Controls
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Previous
            ControlButton(size = 52, onClick = onPrev) {
                SkipIcon(flipped = true)
            }

            Spacer(Modifier.width(28.dp))

            // Play/Pause
            ControlButton(size = 64, isAccent = true, accentColor = accent, onClick = onPlayPause) {
                if (isPlaying) PauseIcon(accent) else PlayIcon(accent)
            }

            Spacer(Modifier.width(28.dp))

            // Next
            ControlButton(size = 52, onClick = onNext) {
                SkipIcon(flipped = false)
            }
        }
    }
    }
}

@Composable
private fun ControlButton(
    size: Int,
    isAccent: Boolean = false,
    accentColor: Color = CopperLived,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (isAccent) accentColor.copy(alpha = 0.15f) else DimRemaining)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun PlayIcon(color: Color = CopperLived) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.15f)
            lineTo(size.width * 0.85f, size.height * 0.5f)
            lineTo(size.width * 0.25f, size.height * 0.85f)
            close()
        }
        drawPath(path, color, style = Fill)
    }
}

@Composable
private fun PauseIcon(color: Color = CopperLived) {
    Canvas(modifier = Modifier.size(24.dp)) {
        drawRect(color, topLeft = Offset(size.width * 0.2f, size.height * 0.15f), size = Size(size.width * 0.2f, size.height * 0.7f))
        drawRect(color, topLeft = Offset(size.width * 0.6f, size.height * 0.15f), size = Size(size.width * 0.2f, size.height * 0.7f))
    }
}

@Composable
private fun SkipIcon(flipped: Boolean) {
    val color = TextInput
    Canvas(modifier = Modifier.size(20.dp)) {
        if (flipped) {
            // <<| (previous)
            val tri = Path().apply {
                moveTo(size.width * 0.7f, size.height * 0.15f)
                lineTo(size.width * 0.15f, size.height * 0.5f)
                lineTo(size.width * 0.7f, size.height * 0.85f)
                close()
            }
            drawPath(tri, color)
            drawRect(color, topLeft = Offset(size.width * 0.78f, size.height * 0.15f), size = Size(size.width * 0.08f, size.height * 0.7f))
        } else {
            // |>> (next)
            val tri = Path().apply {
                moveTo(size.width * 0.3f, size.height * 0.15f)
                lineTo(size.width * 0.85f, size.height * 0.5f)
                lineTo(size.width * 0.3f, size.height * 0.85f)
                close()
            }
            drawPath(tri, color)
            drawRect(color, topLeft = Offset(size.width * 0.14f, size.height * 0.15f), size = Size(size.width * 0.08f, size.height * 0.7f))
        }
    }
}
