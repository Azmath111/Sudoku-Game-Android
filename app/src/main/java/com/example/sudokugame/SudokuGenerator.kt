package com.example.sudokugame

enum class Difficulty { Easy, Medium, Hard }

data class Puzzle(
    val board: Array<IntArray>,
    val size: Int,
    val blockRows: Int,
    val blockCols: Int
)

fun generatePuzzle(difficulty: Difficulty): Puzzle {
    // Easy: 4x4 board with 2x2 blocks
    val easy = arrayOf(
        intArrayOf(1, 0, 0, 4),
        intArrayOf(0, 4, 1, 0),
        intArrayOf(2, 1, 0, 3),
        intArrayOf(0, 3, 2, 1)
    )

    // Medium: 6x6 board with 2x3 blocks
    val medium = arrayOf(
        intArrayOf(0, 2, 0, 4, 5, 0),
        intArrayOf(4, 0, 6, 1, 0, 3),
        intArrayOf(2, 3, 0, 0, 6, 1),
        intArrayOf(5, 6, 1, 0, 0, 4),
        intArrayOf(0, 4, 5, 6, 1, 0),
        intArrayOf(6, 1, 2, 3, 0, 5)
    )

    // Hard: classic 9x9 board with 3x3 blocks
    val hard = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 1, 2),
        intArrayOf(0, 0, 0, 0, 0, 3, 0, 8, 5),
        intArrayOf(0, 0, 1, 0, 2, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 5, 0, 7, 0, 0, 0),
        intArrayOf(0, 0, 4, 0, 0, 0, 1, 0, 0),
        intArrayOf(0, 9, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(5, 0, 0, 0, 0, 0, 0, 7, 3),
        intArrayOf(0, 0, 2, 0, 1, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 4, 0, 0, 0, 9)
    )

    return when (difficulty) {
        Difficulty.Easy -> Puzzle(easy.map { it.clone() }.toTypedArray(), 4, 2, 2)
        Difficulty.Medium -> Puzzle(medium.map { it.clone() }.toTypedArray(), 6, 2, 3)
        Difficulty.Hard -> Puzzle(hard.map { it.clone() }.toTypedArray(), 9, 3, 3)
    }
}

fun boardToString(board: Array<IntArray>): String =
    board.joinToString(";") { row -> row.joinToString(",") }

fun stringToBoard(str: String): Array<IntArray> =
    str.split(";").map { row -> row.split(",").map { it.toInt() }.toIntArray() }.toTypedArray()

fun blockDimensions(size: Int): Pair<Int, Int> = when (size) {
    4 -> 2 to 2
    6 -> 2 to 3
    else -> 3 to 3
}
