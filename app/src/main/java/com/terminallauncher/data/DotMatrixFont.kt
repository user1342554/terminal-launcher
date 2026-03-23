package com.terminallauncher.data

/**
 * 3x5 dot matrix font for digits 0-9 and colon.
 * Each digit is represented as a list of 5 rows, each row is 3 booleans.
 */
object DotMatrixFont {

    private val DIGITS: Map<Char, List<List<Boolean>>> = mapOf(
        '0' to listOf(
            listOf(true, true, true),
            listOf(true, false, true),
            listOf(true, false, true),
            listOf(true, false, true),
            listOf(true, true, true),
        ),
        '1' to listOf(
            listOf(false, true, false),
            listOf(true, true, false),
            listOf(false, true, false),
            listOf(false, true, false),
            listOf(true, true, true),
        ),
        '2' to listOf(
            listOf(true, true, true),
            listOf(false, false, true),
            listOf(true, true, true),
            listOf(true, false, false),
            listOf(true, true, true),
        ),
        '3' to listOf(
            listOf(true, true, true),
            listOf(false, false, true),
            listOf(true, true, true),
            listOf(false, false, true),
            listOf(true, true, true),
        ),
        '4' to listOf(
            listOf(true, false, true),
            listOf(true, false, true),
            listOf(true, true, true),
            listOf(false, false, true),
            listOf(false, false, true),
        ),
        '5' to listOf(
            listOf(true, true, true),
            listOf(true, false, false),
            listOf(true, true, true),
            listOf(false, false, true),
            listOf(true, true, true),
        ),
        '6' to listOf(
            listOf(true, true, true),
            listOf(true, false, false),
            listOf(true, true, true),
            listOf(true, false, true),
            listOf(true, true, true),
        ),
        '7' to listOf(
            listOf(true, true, true),
            listOf(false, false, true),
            listOf(false, false, true),
            listOf(false, false, true),
            listOf(false, false, true),
        ),
        '8' to listOf(
            listOf(true, true, true),
            listOf(true, false, true),
            listOf(true, true, true),
            listOf(true, false, true),
            listOf(true, true, true),
        ),
        '9' to listOf(
            listOf(true, true, true),
            listOf(true, false, true),
            listOf(true, true, true),
            listOf(false, false, true),
            listOf(true, true, true),
        ),
        ':' to listOf(
            listOf(false),
            listOf(true),
            listOf(false),
            listOf(true),
            listOf(false),
        ),
        ' ' to listOf(
            listOf(false),
            listOf(false),
            listOf(false),
            listOf(false),
            listOf(false),
        ),
    )

    fun getPattern(char: Char): List<List<Boolean>>? = DIGITS[char]

    /** Width in cells for a character */
    fun charWidth(char: Char): Int = if (char == ':' || char == ' ') 1 else 3

    /** Total width in cells for a time string like "12:34", including 1-cell gaps */
    fun textWidth(text: String): Int {
        var w = 0
        text.forEachIndexed { i, c ->
            w += charWidth(c)
            if (i < text.length - 1) w += 1 // gap between chars
        }
        return w
    }

    /** Returns a set of (col, row) offsets that should be lit for the given text */
    fun getPixels(text: String): Set<Pair<Int, Int>> {
        val pixels = mutableSetOf<Pair<Int, Int>>()
        var curCol = 0

        for (char in text) {
            val pattern = getPattern(char) ?: continue
            for (row in pattern.indices) {
                for (col in pattern[row].indices) {
                    if (pattern[row][col]) {
                        pixels.add(Pair(curCol + col, row))
                    }
                }
            }
            curCol += charWidth(char) + 1
        }
        return pixels
    }
}
