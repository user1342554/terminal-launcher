package com.terminallauncher.ui.components

import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terminallauncher.data.MediaState
import com.terminallauncher.ui.theme.Background
import com.terminallauncher.ui.theme.CopperLived
import com.terminallauncher.ui.theme.DimRemaining
import com.terminallauncher.ui.theme.GoldCurrent
import com.terminallauncher.ui.theme.Monospace
import com.terminallauncher.ui.theme.TextDim
import com.terminallauncher.ui.theme.TextInput
import com.terminallauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun MusicPlayerOverlay(onClose: () -> Unit) {
    val media by MediaState.nowPlaying.collectAsState()
    val controller = MediaState.getController()

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(controller) {
        while (true) {
            controller?.let { ctrl ->
                val state = ctrl.playbackState
                positionMs = state?.position ?: 0L
                durationMs = ctrl.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                if (state?.state == PlaybackState.STATE_PLAYING && state.lastPositionUpdateTime > 0) {
                    val elapsed = android.os.SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
                    positionMs += (elapsed * state.playbackSpeed).toLong()
                }
                if (durationMs > 0) positionMs = positionMs.coerceIn(0, durationMs)
            }
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (media.title.isEmpty()) {
                Ln("================================", CopperLived)
                Ln("      n o w   p l a y i n g     ", CopperLived)
                Ln("================================", CopperLived)
                Spacer(Modifier.height(24.dp))
                Ln("     nothing is playing...       ", TextSecondary)
                Spacer(Modifier.height(8.dp))
                Ln("   open a music app first        ", TextDim)
                Spacer(Modifier.height(32.dp))
                Ln("================================", CopperLived)
                Spacer(Modifier.height(24.dp))
                EscButton(onClose)
            } else {
                Ln("================================", CopperLived)
                Ln("      n o w   p l a y i n g     ", CopperLived)
                Ln("================================", CopperLived)

                Spacer(Modifier.height(20.dp))

                // Equalizer — real Compose bars
                EqualizerBars(isPlaying = media.isPlaying)

                Spacer(Modifier.height(20.dp))

                Ln("--------------------------------", TextDim)

                Spacer(Modifier.height(12.dp))

                // Title
                Text(
                    text = media.title,
                    color = TextInput,
                    fontFamily = Monospace,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(2.dp))

                // Artist
                Text(
                    text = media.artist.lowercase(),
                    color = CopperLived,
                    fontFamily = Monospace,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // ASCII progress bar
                val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                val barW = 28
                val filled = (progress * barW).toInt()
                val empty = barW - filled
                val bar = "[" + "#".repeat(filled) + "-".repeat(empty) + "]"

                Ln(bar, CopperLived)

                Spacer(Modifier.height(4.dp))

                // Timestamps
                val posStr = formatTime(positionMs)
                val durStr = formatTime(durationMs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(posStr, color = TextSecondary, fontFamily = Monospace, fontSize = 12.sp)
                    Text(durStr, color = TextSecondary, fontFamily = Monospace, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))

                Ln("--------------------------------", TextDim)

                Spacer(Modifier.height(16.dp))

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev
                    Text(
                        text = "|<<",
                        color = CopperLived,
                        fontFamily = Monospace,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { controller?.transportControls?.skipToPrevious() }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    Spacer(Modifier.width(16.dp))

                    // Play/Pause
                    val icon = if (media.isPlaying) " || " else " >> "
                    Text(
                        text = icon,
                        color = GoldCurrent,
                        fontFamily = Monospace,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DimRemaining)
                            .clickable {
                                if (media.isPlaying) controller?.transportControls?.pause()
                                else controller?.transportControls?.play()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )

                    Spacer(Modifier.width(16.dp))

                    // Next
                    Text(
                        text = ">>|",
                        color = CopperLived,
                        fontFamily = Monospace,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { controller?.transportControls?.skipToNext() }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Ln("================================", CopperLived)

                Spacer(Modifier.height(20.dp))

                EscButton(onClose)
            }
        }
    }
}

@Composable
private fun EqualizerBars(isPlaying: Boolean) {
    val barCount = 16
    val transition = rememberInfiniteTransition(label = "eq")

    val phases = (0 until barCount).map { i ->
        val state by transition.animateFloat(
            initialValue = 0f,
            targetValue = 6.2832f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1200 + (i * 131 % 800),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "bar$i"
        )
        state
    }

    val barColor = if (isPlaying) CopperLived else TextDim

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val fraction = if (isPlaying) {
                val w1 = sin(phases[i].toDouble() + i * 0.7)
                ((w1 + 1.0) / 2.0).toFloat().coerceIn(0.08f, 0.85f)
            } else {
                0.10f
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((48 * fraction).dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(barColor.copy(alpha = 0.4f + fraction * 0.5f))
            )
        }
    }
}

@Composable
private fun Ln(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        color = color,
        fontFamily = Monospace,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EscButton(onClose: () -> Unit) {
    Text(
        text = "[ esc ]",
        color = TextDim,
        fontFamily = Monospace,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClose)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}
