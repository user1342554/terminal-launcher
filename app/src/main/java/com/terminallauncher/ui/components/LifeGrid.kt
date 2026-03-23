package com.terminallauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.terminallauncher.ui.theme.CopperLived
import com.terminallauncher.ui.theme.DimRemaining
import com.terminallauncher.ui.theme.GoldCurrent
import java.util.Calendar
import kotlin.math.floor

private const val TOTAL_MONTHS = 960 // 80 years
private const val GAP_RATIO = 0.18f
private const val PADDING_H_RATIO = 0.05f
private const val PADDING_V_RATIO = 0.06f
private const val CORNER_RADIUS_RATIO = 0.25f

@Composable
fun LifeGrid(
    birthYear: Int,
    birthMonth: Int,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val now = remember {
        val cal = Calendar.getInstance()
        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    val livedMonths = remember(birthYear, birthMonth) {
        val months = (now.first - birthYear) * 12 + (now.second - birthMonth)
        months.coerceIn(0, TOTAL_MONTHS)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val paddingH = canvasWidth * PADDING_H_RATIO
        val paddingV = canvasHeight * PADDING_V_RATIO
        val availableWidth = canvasWidth - 2 * paddingH
        val availableHeight = canvasHeight - 2 * paddingV

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
        val offsetX = (canvasWidth - gridWidth) / 2f
        val offsetY = (canvasHeight - gridHeight) / 2f

        val cornerRadius = cellSize * CORNER_RADIUS_RATIO

        // Opacity variations for lived months
        val opacityValues = floatArrayOf(0.82f, 0.84f, 0.85f, 0.86f, 0.87f, 0.88f, 0.89f, 0.9f, 0.91f)

        for (i in 0 until TOTAL_MONTHS) {
            val col = i % bestColumns
            val row = i / bestColumns
            val x = offsetX + col * stride
            val y = offsetY + row * stride

            val color = when {
                i < livedMonths -> {
                    val opacity = opacityValues[i % opacityValues.size]
                    CopperLived.copy(alpha = opacity * alpha)
                }
                i == livedMonths -> GoldCurrent.copy(alpha = alpha)
                else -> DimRemaining.copy(alpha = alpha)
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(cellSize, cellSize),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }

        // Glow effect for current month
        if (livedMonths in 0 until TOTAL_MONTHS) {
            val col = livedMonths % bestColumns
            val row = livedMonths / bestColumns
            val x = offsetX + col * stride
            val y = offsetY + row * stride
            val glowSize = cellSize * 1.3f
            val glowOffset = (glowSize - cellSize) / 2f

            drawRoundRect(
                color = GoldCurrent.copy(alpha = 0.15f * alpha),
                topLeft = Offset(x - glowOffset, y - glowOffset),
                size = Size(glowSize, glowSize),
                cornerRadius = CornerRadius(cornerRadius * 1.3f, cornerRadius * 1.3f)
            )
        }
    }
}
