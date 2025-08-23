package com.example.sudokugame

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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

    Crossfade(targetState = screen, label = "screen") { s ->
        when (s) {
            Screen.Start -> StartScreen(
                hasSaved = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
                    .contains("board"),
                onStart = { difficulty ->
                    board = generatePuzzle(difficulty)
                    time = 0
                    screen = Screen.Game
                },
                onContinue = {
                    val prefs = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
                    board = stringToBoard(prefs.getString("board", "") ?: "")
                    time = prefs.getInt("time", 0)
                    screen = Screen.Game
                },
                onSettings = { screen = Screen.Settings }
            )
            Screen.Game -> board?.let { b ->
                GameScreen(
                    initialBoard = b,
                    initialTime = time,
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
        modifier = Modifier.fillMaxSize(),
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
        SudokuBoard(board, notes.map { row -> row.map { it.toSet() }.toTypedArray() }.toTypedArray(), selected) { r, c ->
            selected = r to c
        }
        Spacer(Modifier.height(16.dp))
        NumberPad { number ->
            selected?.let { (r, c) ->
                if (noteMode) {
                    val cellNotes = notes[r][c]
                    if (cellNotes.contains(number)) cellNotes.remove(number) else cellNotes.add(number)
                    notes = notes.copyOf()
                } else if (isValidMove(board, r, c, number)) {
                    val newBoard = board.map { it.clone() }.toTypedArray()
                    newBoard[r][c] = number
                    board = newBoard
                    notes[r][c].clear()
                    if (vibrationEnabled) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    if (soundEnabled) {
                        tone.startTone(ToneGenerator.TONE_DTMF_0, 100)
                    }
                    if (isBoardComplete(board)) {
                        solved = true
                        clearSave()
                    }
                }
            }
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

@Composable
fun NumberPad(onNumberSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (n in row) {
                    Button(onClick = { onNumberSelected(n) }) { Text(n.toString()) }
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
