package com.example.sudokugame

enum class Difficulty { Easy, Medium, Hard }

fun generatePuzzle(difficulty: Difficulty): Array<IntArray> {
    val easy = arrayOf(
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

    val medium = arrayOf(
        intArrayOf(0, 0, 0, 2, 6, 0, 7, 0, 1),
        intArrayOf(6, 8, 0, 0, 7, 0, 0, 9, 0),
        intArrayOf(1, 9, 0, 0, 0, 4, 5, 0, 0),
        intArrayOf(8, 2, 0, 1, 0, 0, 0, 4, 0),
        intArrayOf(0, 0, 4, 6, 0, 2, 9, 0, 0),
        intArrayOf(0, 5, 0, 0, 0, 3, 0, 2, 8),
        intArrayOf(0, 0, 9, 3, 0, 0, 0, 7, 4),
        intArrayOf(0, 4, 0, 0, 5, 0, 0, 3, 6),
        intArrayOf(7, 0, 3, 0, 1, 8, 0, 0, 0)
    )

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

    val puzzle = when (difficulty) {
        Difficulty.Easy -> easy
        Difficulty.Medium -> medium
        Difficulty.Hard -> hard
    }
    return puzzle.map { it.clone() }.toTypedArray()
}

fun generateEmptyBoard(): Array<IntArray> = Array(9) { IntArray(9) { 0 } }

fun boardToString(board: Array<IntArray>): String =
    board.joinToString(";") { row -> row.joinToString(",") }

fun stringToBoard(str: String): Array<IntArray> =
    str.split(";").map { row -> row.split(",").map { it.toInt() }.toIntArray() }.toTypedArray()
