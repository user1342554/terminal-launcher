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
                controller.registerCallback(callback, android.os.Handler(android.os.Looper.getMainLooper()))

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
        val bmpArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        val bmpArt2 = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val uriArt = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
        val uriArt2 = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
        android.util.Log.d("MediaListener", "bmpArt=${bmpArt != null} bmpArt2=${bmpArt2 != null} uriArt=$uriArt uriArt2=$uriArt2")

        var art = bmpArt ?: bmpArt2

        if (art == null) {
            val uriStr = uriArt ?: uriArt2
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
        android.util.Log.d("MediaListener", "Loading art from URI: $uriStr")
        // Try content:// URI
        try {
            val uri = Uri.parse(uriStr)
            val bmp = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
            if (bmp != null) {
                android.util.Log.d("MediaListener", "Loaded from content URI: ${bmp.width}x${bmp.height}")
                return bmp
            }
        } catch (e: Exception) {
            android.util.Log.d("MediaListener", "Content URI failed: ${e.message}")
        }
        // Try HTTP/HTTPS URL in a blocking way (called from background thread via refresh)
        if (uriStr.startsWith("http")) {
            try {
                val conn = java.net.URL(uriStr).openConnection()
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val bmp = BitmapFactory.decodeStream(conn.getInputStream())
                if (bmp != null) {
                    android.util.Log.d("MediaListener", "Loaded from HTTP: ${bmp.width}x${bmp.height}")
                    return bmp
                }
            } catch (e: Exception) {
                android.util.Log.d("MediaListener", "HTTP failed: ${e.message}")
            }
        }
        return null
    }

    private fun extractColors(art: Bitmap?): DynamicColors {
        if (art != null) {
            try {
                val palette = Palette.from(art).maximumColorCount(24).generate()

                // Score each swatch: mostly area, but favor brighter colors
                val maxPop = (palette.dominantSwatch?.population ?: 1).toFloat()
                // Check if the image is mostly B&W by looking at the dominant color
                val domHsv = FloatArray(3)
                val domColor = palette.dominantSwatch?.rgb ?: 0xFF888888.toInt()
                android.graphics.Color.colorToHSV(domColor, domHsv)
                val isMonochrome = domHsv[1] < 0.20f // dominant color is grey = B&W image

                val candidates = if (isMonochrome) {
                    // B&W image — only use grey swatches, ignore any slight color casts
                    palette.swatches.filter {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.rgb, hsv)
                        hsv[2] > 0.25f
                    }
                } else {
                    // Colorful image — pick colorful swatches
                    val colorful = palette.swatches.filter {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.rgb, hsv)
                        hsv[1] > 0.25f && hsv[2] > 0.25f
                    }
                    colorful.ifEmpty { palette.swatches.filter {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(it.rgb, hsv)
                        hsv[2] > 0.25f
                    } }
                }
                    .sortedByDescending { swatch ->
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(swatch.rgb, hsv)
                        val areaScore = swatch.population / maxPop  // 0-1
                        val brightScore = hsv[2]                     // 0-1
                        val satScore = hsv[1]                        // 0-1
                        areaScore * 0.5f + brightScore * 0.3f + satScore * 0.2f
                    }

                val accent: Int
                val light: Int
                val muted: Int

                if (candidates.isEmpty()) {
                    val dom = palette.dominantSwatch?.rgb ?: 0xFF888888.toInt()
                    accent = boostColor(dom)
                    light = lighten(accent)
                    muted = darken(accent)
                } else {
                    // Pick 3 distinct colors by maximizing hue distance
                    val sorted = candidates.sortedByDescending { swatch ->
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(swatch.rgb, hsv)
                        val areaScore = swatch.population / maxPop
                        val brightScore = hsv[2]
                        val satScore = hsv[1]
                        areaScore * 0.5f + brightScore * 0.3f + satScore * 0.2f
                    }

                    accent = boostColor(sorted[0].rgb)

                    // Find a second color with different hue
                    val accentHue = hueOf(accent)
                    val second = sorted.drop(1).firstOrNull { swatch ->
                        hueDist(hueOf(swatch.rgb), accentHue) > 30f
                    }
                    light = if (second != null) boostColor(second.rgb, brighten = true)
                            else lighten(accent)

                    // Find a third color different from both
                    val lightHue = hueOf(light)
                    val third = sorted.drop(1).firstOrNull { swatch ->
                        hueDist(hueOf(swatch.rgb), accentHue) > 30f &&
                        hueDist(hueOf(swatch.rgb), lightHue) > 30f
                    }
                    muted = if (third != null) darken(third.rgb)
                            else darken(accent)
                }

                return DynamicColors(
                    accent = accent or 0xFF000000.toInt(),
                    accentLight = light or 0xFF000000.toInt(),
                    muted = muted or 0xFF000000.toInt()
                )
            } catch (_: Exception) {}
        }
        // No art at all — use default colors, don't generate random hues
        return DynamicColors()
    }

    private fun lighten(color: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * 0.6f).coerceAtMost(0.7f)
        hsv[2] = (hsv[2] + 0.3f).coerceAtMost(1f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun hueOf(color: Int): Float {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        return hsv[0]
    }

    private fun hueDist(h1: Float, h2: Float): Float {
        val d = kotlin.math.abs(h1 - h2)
        return if (d > 180f) 360f - d else d
    }

    private fun boostColor(color: Int, brighten: Boolean = false): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        // Only boost saturation if clearly colorful — never force color onto greys
        if (hsv[1] > 0.25f) {
            hsv[1] = (hsv[1] * 1.2f).coerceAtMost(1f)
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
