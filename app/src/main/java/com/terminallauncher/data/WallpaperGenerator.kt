package com.terminallauncher.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.Calendar

class WallpaperGenerator(private val context: Context) {

    companion object {
        private const val TOTAL_MONTHS = 960
        private const val GAP_RATIO = 0.18f
        private const val PADDING_H_RATIO = 0.05f
        private const val PADDING_V_RATIO = 0.06f
        private const val CORNER_RADIUS_RATIO = 0.25f

        private const val BG_COLOR = 0xFF0A0A0F.toInt()
        private const val COPPER_COLOR = 0xFFC9956B.toInt()
        private const val GOLD_COLOR = 0xFFE8B87D.toInt()
        private const val DIM_COLOR = 0xFF16161D.toInt()
        private const val SCREENTIME_COLOR = 0xFF8B3A3A.toInt()
    }

    fun generateAndSetWallpaper(birthYear: Int, birthMonth: Int, screenTimeMonths: Int = 0) {
        val metrics = getScreenMetrics()
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        // Lock screen: extra top padding for Samsung clock
        val lockBitmap = renderGrid(width, height, birthYear, birthMonth, screenTimeMonths, lockScreen = true)
        try {
            WallpaperManager.getInstance(context).setBitmap(lockBitmap, null, true, WallpaperManager.FLAG_LOCK)
        } catch (_: Exception) {}
        lockBitmap.recycle()

        // Home screen: normal grid
        val homeBitmap = renderGrid(width, height, birthYear, birthMonth, screenTimeMonths, lockScreen = false)
        try {
            WallpaperManager.getInstance(context).setBitmap(homeBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        } catch (_: Exception) {}
        homeBitmap.recycle()
    }

    private fun getScreenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    private fun renderGrid(width: Int, height: Int, birthYear: Int, birthMonth: Int, screenTimeMonths: Int = 0, lockScreen: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(BG_COLOR)

        val cal = Calendar.getInstance()
        val nowYear = cal.get(Calendar.YEAR)
        val nowMonth = cal.get(Calendar.MONTH) + 1
        val livedMonths = ((nowYear - birthYear) * 12 + (nowMonth - birthMonth)).coerceIn(0, TOTAL_MONTHS)

        val w = width.toFloat()
        val h = height.toFloat()

        val paddingH = w * PADDING_H_RATIO
        // Same top padding on both so grid doesn't jump on unlock
        val topPadding = h * 0.22f
        val paddingV = h * PADDING_V_RATIO
        val availableWidth = w - 2 * paddingH
        val availableHeight = h - topPadding - paddingV

        // Auto-optimize column count
        var bestColumns = 24
        var bestCellSize = 0f

        for (cols in 16..40) {
            val rows = (TOTAL_MONTHS + cols - 1) / cols
            val cellW = availableWidth / (cols + (cols - 1) * GAP_RATIO)
            val cellH = availableHeight / (rows + (rows - 1) * GAP_RATIO)
            val cellSize = minOf(cellW, cellH)
            if (cellSize > bestCellSize) {
                bestCellSize = cellSize
                bestColumns = cols
            }
        }

        val cellSize = bestCellSize
        val gap = cellSize * GAP_RATIO
        val stride = cellSize + gap
        val rows = (TOTAL_MONTHS + bestColumns - 1) / bestColumns

        val gridWidth = bestColumns * cellSize + (bestColumns - 1) * gap
        val gridHeight = rows * cellSize + (rows - 1) * gap
        val offsetX = (w - gridWidth) / 2f
        val offsetY = topPadding + (availableHeight - gridHeight) / 2f

        val cornerRadius = cellSize * CORNER_RADIUS_RATIO
        val opacityValues = floatArrayOf(0.82f, 0.84f, 0.85f, 0.86f, 0.87f, 0.88f, 0.89f, 0.9f, 0.91f)

        val screenTimeStart = livedMonths + 1
        val screenTimeEnd = (screenTimeStart + screenTimeMonths).coerceAtMost(TOTAL_MONTHS)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF()

        for (i in 0 until TOTAL_MONTHS) {
            val col = i % bestColumns
            val row = i / bestColumns
            val x = offsetX + col * stride
            val y = offsetY + row * stride

            when {
                i < livedMonths -> {
                    val opacity = opacityValues[i % opacityValues.size]
                    paint.color = COPPER_COLOR
                    paint.alpha = (opacity * 255).toInt()
                }
                i == livedMonths -> {
                    paint.color = GOLD_COLOR
                    paint.alpha = 255
                }
                i in screenTimeStart until screenTimeEnd -> {
                    val opacity = opacityValues[i % opacityValues.size]
                    paint.color = SCREENTIME_COLOR
                    paint.alpha = (opacity * 0.7f * 255).toInt()
                }
                else -> {
                    paint.color = DIM_COLOR
                    paint.alpha = 255
                }
            }

            rect.set(x, y, x + cellSize, y + cellSize)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }

        // Glow for current month
        if (livedMonths in 0 until TOTAL_MONTHS) {
            val col = livedMonths % bestColumns
            val row = livedMonths / bestColumns
            val x = offsetX + col * stride
            val y = offsetY + row * stride
            val glowSize = cellSize * 1.3f
            val glowOffset = (glowSize - cellSize) / 2f

            paint.color = GOLD_COLOR
            paint.alpha = 38 // ~15%
            rect.set(x - glowOffset, y - glowOffset, x - glowOffset + glowSize, y - glowOffset + glowSize)
            canvas.drawRoundRect(rect, cornerRadius * 1.3f, cornerRadius * 1.3f, paint)
        }

        return bitmap
    }
}
