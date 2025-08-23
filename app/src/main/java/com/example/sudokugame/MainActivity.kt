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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sudokugame.ui.theme.SudokuGameTheme
import kotlinx.coroutines.delay

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
    var board by remember { mutableStateOf<Array<IntArray>?>(null) }
    var time by remember { mutableStateOf(0) }
    var difficulty by remember { mutableStateOf(Difficulty.Hard) }

    Crossfade(targetState = screen, label = "screen") { s ->
        when (s) {
            Screen.Start -> StartScreen(
                hasSaved = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
                    .contains("board"),
                onStart = { diff ->
                    board = generatePuzzle(diff)
                    difficulty = diff
                    time = 0
                    screen = Screen.Game
                },
                onContinue = {
                    val prefs = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
                    board = stringToBoard(prefs.getString("board", "") ?: "")
                    time = prefs.getInt("time", 0)
                    difficulty = Difficulty.Hard
                    screen = Screen.Game
                },
                onSettings = { screen = Screen.Settings }
            )
            Screen.Game -> board?.let { b ->
                GameScreen(
                    initialBoard = b,
                    initialTime = time,
                    difficulty = difficulty,
                    onBack = {
                        board = null
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
    }
}

@Composable
fun GameScreen(
    initialBoard: Array<IntArray>,
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
    var notes by remember { mutableStateOf(Array(9) { Array(9) { mutableSetOf<Int>() } }) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var noteMode by remember { mutableStateOf(false) }
    var time by remember { mutableStateOf(initialTime) }
    var solved by remember { mutableStateOf(false) }
    var conflicts by remember { mutableStateOf(findConflicts(board)) }

    val solution = remember {
        initialBoard.map { it.clone() }.toTypedArray().also { solveSudoku(it) }
    }
    val maxNumber = when (difficulty) {
        Difficulty.Easy -> 4
        Difficulty.Medium -> 6
        Difficulty.Hard -> 9
    }

    LaunchedEffect(Unit) {
        while (true) {
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

    Column(
        modifier = Modifier.fillMaxSize().animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Time: ${time}s")
            Row {
                TextButton(onClick = { noteMode = !noteMode }) {
                    Text(if (noteMode) "Notes On" else "Notes Off")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { save() }) { Text("Save") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    clearSave()
                    onBack()
                }) { Text("Exit") }
            }
        }
        SudokuBoard(
            board,
            notes.map { row -> row.map { it.toSet() }.toTypedArray() }.toTypedArray(),
            selected,
            conflicts
        ) { r, c ->
            selected = r to c
        }
        Spacer(Modifier.height(16.dp))
        AnimatedVisibility(visible = selected != null, enter = fadeIn(), exit = fadeOut()) {
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
                        conflicts = findConflicts(board)
                        if (vibrationEnabled) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                        if (soundEnabled) {
                            tone.startTone(ToneGenerator.TONE_DTMF_0, 100)
                        }
                        if (isBoardComplete(board) && conflicts.isEmpty()) {
                            solved = true
                            clearSave()
                        }
                    }
                }
            }, onClear = {
                selected?.let { (r, c) ->
                    if (initialBoard[r][c] == 0) {
                        val newBoard = board.map { it.clone() }.toTypedArray()
                        newBoard[r][c] = 0
                        board = newBoard
                        notes[r][c].clear()
                        conflicts = findConflicts(board)
                    }
                }
            }, onHint = {
                selected?.let { (r, c) ->
                    if (initialBoard[r][c] == 0 && board[r][c] == 0) {
                        val newBoard = board.map { it.clone() }.toTypedArray()
                        newBoard[r][c] = solution[r][c]
                        board = newBoard
                        conflicts = findConflicts(board)
                        if (isBoardComplete(board) && conflicts.isEmpty()) {
                            solved = true
                            clearSave()
                        }
                    }
                }
            })
        }
        AnimatedVisibility(visible = solved, enter = fadeIn(), exit = fadeOut()) {
            Text("Congratulations!", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

fun isValidMove(board: Array<IntArray>, row: Int, col: Int, value: Int): Boolean {
    if (board[row][col] != 0) return false
    for (i in 0 until 9) {
        if (board[row][i] == value || board[i][col] == value) return false
    }
    val sr = row / 3 * 3
    val sc = col / 3 * 3
    for (r in sr until sr + 3) {
        for (c in sc until sc + 3) {
            if (board[r][c] == value) return false
        }
    }
    return true
}

fun isBoardComplete(board: Array<IntArray>): Boolean =
    board.all { row -> row.all { it != 0 } }

fun findConflicts(board: Array<IntArray>): Set<Pair<Int, Int>> {
    val conflicts = mutableSetOf<Pair<Int, Int>>()
    // Rows
    for (r in 0 until 9) {
        val seen = mutableMapOf<Int, MutableList<Int>>()
        for (c in 0 until 9) {
            val v = board[r][c]
            if (v != 0) seen.getOrPut(v) { mutableListOf() }.add(c)
        }
        for (cols in seen.values) if (cols.size > 1) cols.forEach { c -> conflicts.add(r to c) }
    }
    // Columns
    for (c in 0 until 9) {
        val seen = mutableMapOf<Int, MutableList<Int>>()
        for (r in 0 until 9) {
            val v = board[r][c]
            if (v != 0) seen.getOrPut(v) { mutableListOf() }.add(r)
        }
        for (rows in seen.values) if (rows.size > 1) rows.forEach { r -> conflicts.add(r to c) }
    }
    // Blocks
    for (sr in 0 until 9 step 3) {
        for (sc in 0 until 9 step 3) {
            val seen = mutableMapOf<Int, MutableList<Pair<Int, Int>>>()
            for (r in sr until sr + 3) {
                for (c in sc until sc + 3) {
                    val v = board[r][c]
                    if (v != 0) seen.getOrPut(v) { mutableListOf() }.add(r to c)
                }
            }
            for (cells in seen.values) if (cells.size > 1) conflicts.addAll(cells)
        }
    }
    return conflicts
}

fun solveSudoku(board: Array<IntArray>): Boolean {
    for (r in 0 until 9) {
        for (c in 0 until 9) {
            if (board[r][c] == 0) {
                for (n in 1..9) {
                    if (isValidMove(board, r, c, n)) {
                        board[r][c] = n
                        if (solveSudoku(board)) return true
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
    onNumberSelected: (Int) -> Unit,
    onClear: () -> Unit,
    onHint: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val numbers = (1..maxNumber).toList()
        numbers.chunked(3).forEach { rowNums ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowNums.forEach { n ->
                    Button(onClick = { onNumberSelected(n) }) { Text(n.toString()) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onClear) { Text("Clear") }
            Button(onClick = onHint) { Text("Hint") }
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
