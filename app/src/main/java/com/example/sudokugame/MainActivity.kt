package com.example.sudokugame

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.sudokugame.ui.theme.SudokuGameTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class Screen { Start, Game, Settings }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuGameTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.Start) }
    var puzzle by remember { mutableStateOf<Puzzle?>(null) }
    var time by remember { mutableStateOf(0) }
    var difficulty by remember { mutableStateOf(Difficulty.Hard) }

    Crossfade(targetState = screen, label = "screen") { s ->
        when (s) {
            Screen.Start -> StartScreen(
                hasSaved = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
                    .contains("board"),
                onStart = { diff ->
                    val p = generatePuzzle(diff)
                    puzzle = p
                    difficulty = diff
                    time = 0
                    screen = Screen.Game
                },
                onContinue = {
                    val prefs = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
                    val b = stringToBoard(prefs.getString("board", "") ?: "")
                    val size = b.size
                    val (br, bc) = blockDimensions(size)
                    val sol = b.map { it.clone() }.toTypedArray().also { solveSudoku(it, size, br, bc) }
                    puzzle = Puzzle(b, size, br, bc, sol)
                    time = prefs.getInt("time", 0)
                    difficulty = when (size) { 4 -> Difficulty.Easy; 6 -> Difficulty.Medium; else -> Difficulty.Hard }
                    screen = Screen.Game
                },
                onSettings = { screen = Screen.Settings }
            )
            Screen.Game -> puzzle?.let { p ->
                GameScreen(
                    initialBoard = p.board,
                    solutionBoard = p.solution,
                    size = p.size,
                    blockRows = p.blockRows,
                    blockCols = p.blockCols,
                    initialTime = time,
                    difficulty = difficulty,
                    onBack = {
                        puzzle = null
                        screen = Screen.Start
                    }
                )
            }
            Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Start })
        }
    }
}

@Composable
fun StartScreen(
    hasSaved: Boolean,
    onStart: (Difficulty) -> Unit,
    onContinue: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
    val scores = prefs.getString("scores", "")
        ?.split(";")
        ?.mapNotNull { entry ->
            val parts = entry.split("|")
            val score = parts.getOrNull(0)?.toIntOrNull()
            val time = parts.getOrNull(1)?.toIntOrNull()
            if (score != null && time != null) score to time else null
        } ?: emptyList()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sudoku",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))
        Row {
            Button(onClick = { onStart(Difficulty.Easy) }) { Text("Easy") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onStart(Difficulty.Medium) }) { Text("Medium") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onStart(Difficulty.Hard) }) { Text("Hard") }
        }
        if (hasSaved) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinue) { Text("Continue") }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSettings) { Text("Settings") }
        if (scores.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("High Scores:")
                scores.forEachIndexed { index, (s, t) ->
                    Text("${index + 1}. ${s} pts - ${formatTime(t)}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    initialBoard: Array<IntArray>,
    solutionBoard: Array<IntArray>,
    size: Int,
    blockRows: Int,
    blockCols: Int,
    initialTime: Int,
    difficulty: Difficulty,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
    val haptic = LocalHapticFeedback.current
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 50) }
    val soundEnabled = prefs.getBoolean("sound", true)
    val vibrationEnabled = prefs.getBoolean("vibration", true)
    var board by remember { mutableStateOf(initialBoard.map { it.clone() }.toTypedArray()) }
    var notes by remember { mutableStateOf(Array(size) { Array(size) { mutableSetOf<Int>() } }) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var noteMode by remember { mutableStateOf(false) }
    var time by remember { mutableStateOf(initialTime) }
    var solved by remember { mutableStateOf(false) }
    var conflicts by remember { mutableStateOf(findConflicts(board, size, blockRows, blockCols)) }
    var hintsUsed by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }

    val solution = solutionBoard
    val maxNumber = size

    LaunchedEffect(solved) {
        while (!solved) {
            delay(1000)
            time++
        }
    }

    fun save() {
        prefs.edit()
            .putString("board", boardToString(board))
            .putInt("time", time)
            .apply()
    }

    fun clearSave() {
        prefs.edit().remove("board").remove("time").apply()
    }

    fun computeScore(): Int {
        val base = size * size * 100
        return (base - time * 5 - hintsUsed * 100).coerceAtLeast(0)
    }

    fun saveHighScore(value: Int, time: Int) {
        val list = prefs.getString("scores", "")
            ?.split(";")
            ?.mapNotNull { entry ->
                val parts = entry.split("|")
                val score = parts.getOrNull(0)?.toIntOrNull()
                val t = parts.getOrNull(1)?.toIntOrNull()
                if (score != null && t != null) score to t else null
            }
            ?.toMutableList() ?: mutableListOf()
        list.add(value to time)
        val top = list.sortedByDescending { it.first }.take(5)
        val str = top.joinToString(";") { "${it.first}|${it.second}" }
        prefs.edit().putString("scores", str).apply()
    }

    fun restart() {
        board = initialBoard.map { it.clone() }.toTypedArray()
        notes = Array(size) { Array(size) { mutableSetOf<Int>() } }
        time = 0
        hintsUsed = 0
        conflicts = findConflicts(board, size, blockRows, blockCols)
        solved = false
        showDialog = false
        score = 0
    }

    Column(
        modifier = Modifier.fillMaxSize().animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF3C3C3C)
            ),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mini Sudoku")
                    Text(
                        "by Deepak Kumar",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    clearSave()
                    onBack()
                }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatTime(time))
            Text("Difficulty: ${difficulty.name}", fontWeight = FontWeight.Medium)
            TextButton(onClick = { restart() }) { Text("Reset") }
        }
        SudokuBoard(
            board = board,
            initial = initialBoard,
            notes = notes.map { row -> row.map { it.toSet() }.toTypedArray() }.toTypedArray(),
            selected = selected,
            conflicts = conflicts,
            dimension = size,
            blockRows = blockRows,
            blockCols = blockCols
        ) { r, c ->
            selected = r to c
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    selected?.let { (r, c) ->
                        if (initialBoard[r][c] == 0) {
                            val newBoard = board.map { it.clone() }.toTypedArray()
                            if (board[r][c] != 0 && board[r][c] == solution[r][c]) {
                                val empties = mutableListOf<Pair<Int, Int>>()
                                for (i in 0 until size) {
                                    for (j in 0 until size) if (board[i][j] == 0) empties.add(i to j)
                                }
                                val nearest = empties.minByOrNull { (er, ec) -> abs(er - r) + abs(ec - c) }
                                nearest?.let { (nr, nc) -> newBoard[nr][nc] = solution[nr][nc] }
                            } else {
                                newBoard[r][c] = solution[r][c]
                            }
                            board = newBoard
                            hintsUsed++
                            conflicts = findConflicts(board, size, blockRows, blockCols)
                            if (isBoardComplete(board) && conflicts.isEmpty()) {
                                solved = true
                                score = computeScore()
                                saveHighScore(score, time)
                                clearSave()
                                showDialog = true
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3C3C3C),
                    contentColor = Color.White
                )
            ) { Text("Hint") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notes")
                Switch(checked = noteMode, onCheckedChange = { noteMode = it })
            }
            Button(
                onClick = {
                    selected?.let { (r, c) ->
                        if (initialBoard[r][c] == 0) {
                            val newBoard = board.map { it.clone() }.toTypedArray()
                            newBoard[r][c] = 0
                            board = newBoard
                            notes[r][c].clear()
                            conflicts = findConflicts(board, size, blockRows, blockCols)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3C3C3C),
                    contentColor = Color.White
                )
            ) { Text("Erase") }
        }
        Spacer(Modifier.height(8.dp))
        NumberPad(maxNumber, onNumberSelected = { number ->
            selected?.let { (r, c) ->
                if (noteMode) {
                    val cellNotes = notes[r][c]
                    if (cellNotes.contains(number)) cellNotes.remove(number) else cellNotes.add(number)
                    notes = notes.copyOf()
                } else {
                    val newBoard = board.map { it.clone() }.toTypedArray()
                    newBoard[r][c] = number
                    board = newBoard
                    notes[r][c].clear()
                    conflicts = findConflicts(board, size, blockRows, blockCols)
                    if (vibrationEnabled) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    if (soundEnabled) {
                        tone.startTone(ToneGenerator.TONE_DTMF_0, 100)
                    }
                    if (isBoardComplete(board) && conflicts.isEmpty()) {
                        solved = true
                        score = computeScore()
                        saveHighScore(score, time)
                        clearSave()
                        showDialog = true
                    }
                }
            }
        })
        AnimatedVisibility(visible = showDialog, enter = scaleIn(), exit = scaleOut()) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Puzzle Complete") },
                text = { Text("Time: ${time}s\nScore: ${score}") },
                confirmButton = { TextButton(onClick = { restart() }) { Text("Restart") } },
                dismissButton = { TextButton(onClick = { onBack() }) { Text("Main Menu") } }
            )
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

fun isValidMove(board: Array<IntArray>, row: Int, col: Int, value: Int, size: Int, blockRows: Int, blockCols: Int): Boolean {
    if (board[row][col] != 0) return false
    for (i in 0 until size) {
        if (board[row][i] == value || board[i][col] == value) return false
    }
    val sr = row / blockRows * blockRows
    val sc = col / blockCols * blockCols
    for (r in sr until sr + blockRows) {
        for (c in sc until sc + blockCols) {
            if (board[r][c] == value) return false
        }
    }
    return true
}

fun isBoardComplete(board: Array<IntArray>): Boolean =
    board.all { row -> row.all { it != 0 } }

fun findConflicts(board: Array<IntArray>, size: Int, blockRows: Int, blockCols: Int): Set<Pair<Int, Int>> {
    val conflicts = mutableSetOf<Pair<Int, Int>>()
    // Rows
    for (r in 0 until size) {
        val seen = mutableMapOf<Int, MutableList<Int>>()
        for (c in 0 until size) {
            val v = board[r][c]
            if (v != 0) seen.getOrPut(v) { mutableListOf() }.add(c)
        }
        for (cols in seen.values) if (cols.size > 1) cols.forEach { c -> conflicts.add(r to c) }
    }
    // Columns
    for (c in 0 until size) {
        val seen = mutableMapOf<Int, MutableList<Int>>()
        for (r in 0 until size) {
            val v = board[r][c]
            if (v != 0) seen.getOrPut(v) { mutableListOf() }.add(r)
        }
        for (rows in seen.values) if (rows.size > 1) rows.forEach { r -> conflicts.add(r to c) }
    }
    // Blocks
    for (sr in 0 until size step blockRows) {
        for (sc in 0 until size step blockCols) {
            val seen = mutableMapOf<Int, MutableList<Pair<Int, Int>>>()
            for (r in sr until sr + blockRows) {
                for (c in sc until sc + blockCols) {
                    val v = board[r][c]
                    if (v != 0) seen.getOrPut(v) { mutableListOf() }.add(r to c)
                }
            }
            for (cells in seen.values) if (cells.size > 1) conflicts.addAll(cells)
        }
    }
    return conflicts
}

fun solveSudoku(board: Array<IntArray>, size: Int, blockRows: Int, blockCols: Int): Boolean {
    for (r in 0 until size) {
        for (c in 0 until size) {
            if (board[r][c] == 0) {
                for (n in 1..size) {
                    if (isValidMove(board, r, c, n, size, blockRows, blockCols)) {
                        board[r][c] = n
                        if (solveSudoku(board, size, blockRows, blockCols)) return true
                        board[r][c] = 0
                    }
                }
                return false
            }
        }
    }
    return true
}

@Composable
fun NumberPad(
    maxNumber: Int,
    onNumberSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val numbers = (1..maxNumber).toList()
        numbers.chunked(3).forEach { rowNums ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowNums.forEach { n ->
                    Button(
                        onClick = { onNumberSelected(n) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3C3C3C),
                            contentColor = Color.White
                        )
                    ) {
                        Text(n.toString(), fontSize = 24.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
    var sound by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var vibration by remember { mutableStateOf(prefs.getBoolean("vibration", true)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sound", modifier = Modifier.weight(1f))
            Switch(checked = sound, onCheckedChange = {
                sound = it
                prefs.edit().putBoolean("sound", it).apply()
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vibration", modifier = Modifier.weight(1f))
            Switch(checked = vibration, onCheckedChange = {
                vibration = it
                prefs.edit().putBoolean("vibration", it).apply()
            })
        }
        Button(onClick = onBack) { Text("Back") }
    }
}