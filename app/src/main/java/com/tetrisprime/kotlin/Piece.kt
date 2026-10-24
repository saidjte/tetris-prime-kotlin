package com.tetrisprime.kotlin

/**
 * Tetromino piece types and their rotation states.
 * Based on SRS (Super Rotation System) standard.
 */
enum class PieceType(val char: Char) {
    I('I'), O('O'), T('T'), S('S'), Z('Z'), J('J'), L('L');

    companion object {
        fun fromChar(c: Char): PieceType = values().first { it.char == c }
    }
}

/**
 * A single tetromino piece with position and rotation.
 */
data class Piece(
    val type: PieceType,
    val x: Int = 4,
    val y: Int = 0,
    val rotation: Int = 0
) {
    /** Get the 4 cell coordinates of this piece in its current state. */
    fun cells(): List<Pair<Int, Int>> {
        val base = SHAPES[type] ?: return emptyList()
        val rot = base[rotation % 4]
        return rot.map { (dx, dy) -> (x + dx) to (y + dy) }
    }

    fun rotated(dir: Int): Piece = copy(rotation = (rotation + dir + 4) % 4)

    fun moved(dx: Int, dy: Int): Piece = copy(x = x + dx, y = y + dy)

    companion object {
        /** SRS shapes: [rotation 0, R, 180, L] for each piece type. */
        val SHAPES: Map<PieceType, Array<List<Pair<Int, Int>>>> = mapOf(
            PieceType.I to arrayOf(
                listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1),
                listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3),
                listOf(0 to 2, 1 to 2, 2 to 2, 3 to 2),
                listOf(1 to 0, 1 to 1, 1 to 2, 1 to 3)
            ),
            PieceType.O to arrayOf(
                listOf(1 to 0, 2 to 0, 1 to 1, 2 to 1),
                listOf(1 to 0, 2 to 0, 1 to 1, 2 to 1),
                listOf(1 to 0, 2 to 0, 1 to 1, 2 to 1),
                listOf(1 to 0, 2 to 0, 1 to 1, 2 to 1)
            ),
            PieceType.T to arrayOf(
                listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1),
                listOf(1 to 0, 1 to 1, 2 to 1, 1 to 2),
                listOf(0 to 1, 1 to 1, 2 to 1, 1 to 2),
                listOf(1 to 0, 0 to 1, 1 to 1, 1 to 2)
            ),
            PieceType.S to arrayOf(
                listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),
                listOf(1 to 0, 1 to 1, 2 to 1, 2 to 2),
                listOf(1 to 1, 2 to 1, 0 to 2, 1 to 2),
                listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2)
            ),
            PieceType.Z to arrayOf(
                listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),
                listOf(2 to 0, 1 to 1, 2 to 1, 1 to 2),
                listOf(0 to 1, 1 to 1, 1 to 2, 2 to 2),
                listOf(1 to 0, 0 to 1, 1 to 1, 0 to 2)
            ),
            PieceType.J to arrayOf(
                listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1),
                listOf(1 to 0, 2 to 0, 1 to 1, 1 to 2),
                listOf(0 to 1, 1 to 1, 2 to 1, 2 to 2),
                listOf(1 to 0, 1 to 1, 0 to 2, 1 to 2)
            ),
            PieceType.L to arrayOf(
                listOf(2 to 0, 0 to 1, 1 to 1, 2 to 1),
                listOf(1 to 0, 1 to 1, 1 to 2, 2 to 2),
                listOf(0 to 1, 1 to 1, 2 to 1, 0 to 2),
                listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2)
            )
        )

        /** SRS wall kick data. */
        val WALL_KICKS: Map<Pair<Int, Int>, List<Pair<Int, Int>>> = mapOf(
            (0 to 1) to listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
            (1 to 0) to listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
            (1 to 2) to listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
            (2 to 1) to listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
            (2 to 3) to listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),
            (3 to 2) to listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
            (3 to 0) to listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
            (0 to 3) to listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2)
        )
    }
}
