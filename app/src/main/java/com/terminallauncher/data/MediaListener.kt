package com.terminallauncher.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.service.notification.NotificationListenerService
import androidx.palette.graphics.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DynamicColors(
    val accent: Int = 0xFFC9956B.toInt(),      // default copper
    val accentLight: Int = 0xFFE8B87D.toInt(),  // default gold
    val muted: Int = 0xFF2A2A35.toInt()         // default dim
)

data class NowPlaying(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val albumArt: Bitmap? = null,
    val colors: DynamicColors = DynamicColors()
)

object MediaState {
    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying

    private var activeController: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val playing = state?.state == PlaybackState.STATE_PLAYING
            _nowPlaying.value = _nowPlaying.value.copy(isPlaying = playing)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            if (metadata != null) {
                updateFromMetadata(metadata)
            }
        }
    }

    fun refresh(context: Context) {
        lastContext = context
        try {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(context, MediaNotificationListener::class.java)
            val controllers = msm.getActiveSessions(cn)

            // Detach old
            activeController?.unregisterCallback(callback)
            activeController = null

            if (controllers.isNotEmpty()) {
                val controller = controllers[0]
                activeController = controller
                controller.registerCallback(callback)

                val metadata = controller.metadata
                val state = controller.playbackState
                val playing = state?.state == PlaybackState.STATE_PLAYING

                if (metadata != null) {
                    updateFromMetadata(metadata)
                    _nowPlaying.value = _nowPlaying.value.copy(isPlaying = playing)
                } else {
                    _nowPlaying.value = NowPlaying(isPlaying = playing)
                }
            } else {
                _nowPlaying.value = NowPlaying()
            }
        } catch (_: SecurityException) {
            // No notification listener permission
        } catch (_: Exception) {}
    }

    private var lastContext: Context? = null

    private fun updateFromMetadata(metadata: MediaMetadata) {
        var art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        // Try loading from URI if no direct bitmap
        if (art == null) {
            val uriStr = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
            if (uriStr != null && lastContext != null) {
                art = loadBitmapFromUri(lastContext!!, uriStr)
            }
        }

        val colors = extractColors(art)
        _nowPlaying.value = _nowPlaying.value.copy(
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            albumArt = art,
            colors = colors
        )
    }

    private fun loadBitmapFromUri(context: Context, uriStr: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) { null }
    }

    private fun extractColors(art: Bitmap?): DynamicColors {
        if (art != null) {
            try {
                val palette = Palette.from(art).maximumColorCount(24).generate()

                // Score each swatch: mostly area, but favor brighter colors
                val maxPop = (palette.dominantSwatch?.population ?: 1).toFloat()
                // Get colorful candidates
                val colorful = palette.swatches
                    .filter {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.rgb, hsv)
                        hsv[1] > 0.15f && hsv[2] > 0.2f
                    }

                // If no colorful swatches, it's a B&W/monochrome album — use greys directly
                val candidates = colorful.ifEmpty { palette.swatches.filter {
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(it.rgb, hsv)
                    hsv[2] > 0.2f // just not pure black
                } }
                    .sortedByDescending { swatch ->
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(swatch.rgb, hsv)
                        val areaScore = swatch.population / maxPop  // 0-1
                        val brightScore = hsv[2]                     // 0-1
                        val satScore = hsv[1]                        // 0-1
                        areaScore * 0.5f + brightScore * 0.3f + satScore * 0.2f
                    }

                val accent = if (candidates.isNotEmpty()) {
                    boostColor(candidates.first().rgb)
                } else {
                    // All colors are dark/grey — just boost the dominant
                    val dom = palette.dominantSwatch?.rgb ?: 0xFFC9956B.toInt()
                    boostColor(dom)
                }

                val light = lighten(accent)
                val muted = darken(accent)

                return DynamicColors(
                    accent = accent or 0xFF000000.toInt(),
                    accentLight = light or 0xFF000000.toInt(),
                    muted = muted or 0xFF000000.toInt()
                )
            } catch (_: Exception) {}
        }

        val title = _nowPlaying.value.title
        if (title.isNotEmpty()) return colorFromString(title)
        return DynamicColors()
    }

    private fun lighten(color: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * 0.6f).coerceAtMost(0.7f)
        hsv[2] = (hsv[2] + 0.3f).coerceAtMost(1f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun boostColor(color: Int, brighten: Boolean = false): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        // Only boost saturation if the color actually has some — don't force color onto greys
        if (hsv[1] > 0.1f) {
            hsv[1] = (hsv[1] * 1.3f).coerceAtMost(1f)
        }
        hsv[2] = if (brighten) (hsv[2] * 1.2f).coerceIn(0.7f, 1f)
                 else hsv[2].coerceIn(0.5f, 0.85f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun darken(color: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * 0.4f).coerceAtMost(0.5f)
        hsv[2] = 0.15f
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun colorFromString(text: String): DynamicColors {
        val hash = text.hashCode()
        val hue = (hash and 0xFF) * 360f / 255f
        val hsv = floatArrayOf(hue, 0.7f, 0.75f)
        val accent = android.graphics.Color.HSVToColor(hsv)
        hsv[2] = 0.9f
        val light = android.graphics.Color.HSVToColor(hsv)
        hsv[1] = 0.3f; hsv[2] = 0.15f
        val muted = android.graphics.Color.HSVToColor(hsv)
        return DynamicColors(accent = accent, accentLight = light, muted = muted)
    }

    fun getController(): MediaController? = activeController

    fun getPlayerPackage(): String? = activeController?.packageName
}

class MediaNotificationListener : NotificationListenerService()
