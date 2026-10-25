package com.tetrisprime.kotlin

/**
 * The game board (10 columns x 22 rows).
 * Rows 0-1 are spawn area (above visible playfield).
 * Stores which cells are filled and with which marker character.
 *
 * Markers:
 * - I, O, T, S, Z, J, L = piece types
 * - G = garbage row
 * - C = cheese cell (cheese mode)
 */
class Board(val width: Int = 10, val height: Int = 22) {
    private val grid: Array<Char?> = arrayOfNulls(width * height)

    /** Visible playfield height (rows 2..height-1 are visible). */
    val visibleHeight: Int get() = height - 2

    operator fun get(x: Int, y: Int): Char? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return grid[y * width + x]
    }

    operator fun set(x: Int, y: Int, value: Char?) {
        if (x in 0 until width && y in 0 until height) {
            grid[y * width + x] = value
        }
    }

    /** Check if a piece position is valid (no collision, in bounds). */
    fun isValid(piece: Piece): Boolean {
        return piece.cells().all { (x, y) ->
            x in 0 until width && y < height && (y < 0 || this[x, y] == null)
        }
    }

    /** Lock a piece into the board. */
    fun lock(piece: Piece) {
        val typeChar = piece.type.char
        piece.cells().forEach { (x, y) ->
            if (y in 0 until height && x in 0 until width) {
                this[x, y] = typeChar
            }
        }
    }

    /** Clear completed lines. Returns the number of lines cleared and their indices. */
    fun clearLines(): ClearResult {
        val clearedRows = mutableListOf<Int>()
        for (y in height - 1 downTo 0) {
            var full = true
            for (x in 0 until width) {
                val cell = this[x, y]
                // Don't count cheese cells as "filled" for line clearing
                if (cell == null || cell == 'C') {
                    full = false
                    break
                }
            }
            if (full) {
                clearedRows.add(y)
            }
        }
        // Phase 8 fix — sort clearedRows DESCENDING before removal.
        // The previous code iterated in insertion order (also descending due to
        // the downTo loop, but only by coincidence). Sorting explicitly makes
        // the intent clear and prevents index corruption when multiple rows
        // are cleared in one frame.
        // When we remove the lowest cleared row first, the rows above shift
        // down by one. The next cleared row's index (which was higher up)
        // is now one less than originally recorded — but since we process
        // from lowest to highest, the higher rows haven't shifted yet at
        // the time we process the lower ones, so their indices are still
        // valid at that point. Wait — that's backwards. Let me reconsider.
        //
        // Actually: if we clear rows [10, 12, 15] (clearedRows list, ascending),
        // and we process them in ascending order:
        //   - clear row 10: rows 0-9 shift down → row 0 becomes null, row 10 = old row 9
        //   - clear row 12: but row 12 is now what used to be row 13 (because row 10
        //     clearing shifted everything above row 10 down by 1)
        // So we'd be clearing the WRONG row.
        //
        // FIX: process in DESCENDING order (highest row first). When we clear row 15,
        // rows 0-14 shift down → row 0 becomes null, rows 1-15 = old rows 0-14.
        // Row 12 (originally) is now at index 13 — but we haven't processed it yet,
        // and we'll process it next at its ORIGINAL index 12, which is now WRONG.
        //
        // The truly correct approach: rebuild the board by copying non-cleared rows.
        val clearedSet = clearedRows.toSet()
        val newGrid = Array<CharArray?>(height) { null }
        for (i in 0 until height) newGrid[i] = CharArray(width) { ' ' }
        var writeY = height - 1
        for (y in height - 1 downTo 0) {
            if (y in clearedSet) continue
            for (x in 0 until width) {
                newGrid[writeY]!![x] = this[x, y] ?: ' '
            }
            writeY--
        }
        // Fill remaining top rows with null (empty)
        for (y in 0..writeY) {
            for (x in 0 until width) {
                newGrid[y]!![x] = ' '
            }
        }
        // Copy back
        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = newGrid[y]!![x]
                this[x, y] = if (c == ' ') null else c
            }
        }
        return ClearResult(clearedRows.size, clearedRows)
    }

    /** Check if the board is in game-over state (piece spawned colliding). */
    fun isGameOver(piece: Piece): Boolean = !isValid(piece)

    /** Get the ghost piece position (where piece would land). */
    fun ghostDrop(piece: Piece): Piece {
        var p = piece
        while (isValid(p.moved(0, 1))) {
            p = p.moved(0, 1)
        }
        return p
    }

    /** Get the highest filled row index (for danger detection). Returns -1 if empty. */
    fun highestFilledRow(): Int {
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (this[x, y] != null) return y
            }
        }
        return -1
    }

    /**
     * Clear the top N filled rows (used by the Continue system).
     * Returns the number of rows actually cleared.
     */
    fun clearTopRows(n: Int): Int {
        var cleared = 0
        // Iterate from the top, find filled rows, clear them, until we've cleared n.
        for (y in 0 until height) {
            if (cleared >= n) break
            var rowFilled = false
            for (x in 0 until width) {
                if (this[x, y] != null) { rowFilled = true; break }
            }
            if (rowFilled) {
                for (x in 0 until width) this[x, y] = null
                cleared++
            }
        }
        return cleared
    }

    /** Check if the board is completely empty (for Perfect Clear detection). */
    fun isEmpty(): Boolean {
        for (i in grid.indices) {
            if (grid[i] != null) return false
        }
        return true
    }

    /** Check T-Spin using the 3-corner rule.
     * @param piece The T-piece to check
     * @return TSpinType: NONE, MINI, or FULL
     */
    fun checkTSpin(piece: Piece): TSpinType {
        if (piece.type != PieceType.T) return TSpinType.NONE

        // T-piece center is at (x+1, y+1)
        val cx = piece.x + 1
        val cy = piece.y + 1

        // Check 4 corners
        val isFilled = { x: Int, y: Int ->
            x < 0 || x >= width || y >= height || (y >= 0 && this[x, y] != null)
        }

        val topLeft = isFilled(cx - 1, cy - 1)
        val topRight = isFilled(cx + 1, cy - 1)
        val bottomLeft = isFilled(cx - 1, cy + 1)
        val bottomRight = isFilled(cx + 1, cy + 1)

        // Count filled corners based on rotation
        // For T-Spin: at least 3 of 4 corners filled
        val corners = listOf(topLeft, topRight, bottomLeft, bottomRight)
        val filledCount = corners.count { it }

        if (filledCount < 3) return TSpinType.NONE

        // Determine if it's a full T-Spin or T-Spin Mini
        // Mini: the two corners facing the flat side of T are filled
        // Full: at least one of the "back" corners is filled
        return when (piece.rotation) {
            0 -> if (bottomLeft && bottomRight) TSpinType.FULL else TSpinType.MINI // T faces down
            1 -> if (topRight && bottomRight) TSpinType.FULL else TSpinType.MINI  // T faces left
            2 -> if (topLeft && topRight) TSpinType.FULL else TSpinType.MINI      // T faces up
            3 -> if (topLeft && bottomLeft) TSpinType.FULL else TSpinType.MINI    // T faces right
            else -> TSpinType.NONE
        }
    }

    /** Insert a garbage row at the bottom with a hole at the specified column. */
    fun addGarbageRow(holeCol: Int, marker: Char = 'G') {
        // Shift everything up by 1
        for (y in 0 until height - 1) {
            for (x in 0 until width) {
                this[x, y] = this[x, y + 1]
            }
        }
        // Fill bottom row with garbage, except the hole
        for (x in 0 until width) {
            this[x, height - 1] = if (x == holeCol) null else marker
        }
    }

    /** Count cheese cells (for cheese mode). */
    fun countCheeseCells(): Int {
        var count = 0
        for (i in grid.indices) {
            if (grid[i] == 'C') count++
        }
        return count
    }

    /** Fill bottom N rows with cheese cells, each with a random hole. */
    fun fillCheese(rows: Int, rng: kotlin.random.Random) {
        for (r in 0 until rows) {
            val y = height - 1 - r
            val hole = rng.nextInt(width)
            for (x in 0 until width) {
                this[x, y] = if (x == hole) null else 'C'
            }
        }
    }

    /** Deep copy the board. */
    fun copy(): Board {
        val newBoard = Board(width, height)
        for (i in grid.indices) {
            newBoard.grid[i] = grid[i]
        }
        return newBoard
    }
}

/** Result of clearing lines. */
data class ClearResult(val linesCleared: Int, val clearedRows: List<Int>)

/** T-Spin types. */
enum class TSpinType {
    NONE,   // Not a T-Spin
    MINI,   // T-Spin Mini
    FULL    // Full T-Spin
}
