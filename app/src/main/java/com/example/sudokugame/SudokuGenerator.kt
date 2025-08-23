package com.example.sudokugame

enum class Difficulty { Easy, Medium, Hard }

data class Puzzle(
    val board: Array<IntArray>,
    val size: Int,
    val blockRows: Int,
    val blockCols: Int,
    val solution: Array<IntArray>
)

fun generatePuzzle(difficulty: Difficulty): Puzzle {
    // Easy: 4x4 board with 2x2 blocks
    val easy = arrayOf(
        intArrayOf(1, 0, 0, 4),
        intArrayOf(0, 4, 1, 0),
        intArrayOf(2, 1, 0, 3),
        intArrayOf(0, 3, 2, 1)
    )
    val easySolution = arrayOf(
        intArrayOf(1, 2, 3, 4),
        intArrayOf(3, 4, 1, 2),
        intArrayOf(2, 1, 4, 3),
        intArrayOf(4, 3, 2, 1)
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
    val mediumSolution = arrayOf(
        intArrayOf(1, 2, 3, 4, 5, 6),
        intArrayOf(4, 5, 6, 1, 2, 3),
        intArrayOf(2, 3, 4, 5, 6, 1),
        intArrayOf(5, 6, 1, 2, 3, 4),
        intArrayOf(3, 4, 5, 6, 1, 2),
        intArrayOf(6, 1, 2, 3, 4, 5)
    )

    // Hard: classic 9x9 board with 3x3 blocks
    val hard = arrayOf(
        intArrayOf(5, 3, 0, 0, 7, 0, 0, 0, 0),
        intArrayOf(6, 0, 0, 1, 9, 5, 0, 0, 0),
        intArrayOf(0, 9, 8, 0, 0, 0, 0, 6, 0),
        intArrayOf(8, 0, 0, 0, 6, 0, 0, 0, 3),
        intArrayOf(4, 0, 0, 8, 0, 3, 0, 0, 1),
        intArrayOf(7, 0, 0, 0, 2, 0, 0, 0, 6),
        intArrayOf(0, 6, 0, 0, 0, 0, 2, 8, 0),
        intArrayOf(0, 0, 0, 4, 1, 9, 0, 0, 5),
        intArrayOf(0, 0, 0, 0, 8, 0, 0, 7, 9)
    )
    val hardSolution = arrayOf(
        intArrayOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
        intArrayOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
        intArrayOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
        intArrayOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
        intArrayOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
        intArrayOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
        intArrayOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
        intArrayOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
        intArrayOf(3, 4, 5, 2, 8, 6, 1, 7, 9)
    )

    return when (difficulty) {
        Difficulty.Easy -> Puzzle(easy.map { it.clone() }.toTypedArray(), 4, 2, 2, easySolution)
        Difficulty.Medium -> Puzzle(medium.map { it.clone() }.toTypedArray(), 6, 2, 3, mediumSolution)
        Difficulty.Hard -> Puzzle(hard.map { it.clone() }.toTypedArray(), 9, 3, 3, hardSolution)
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
