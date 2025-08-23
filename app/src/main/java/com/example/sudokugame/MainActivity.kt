package com.example.sudokugame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sudokugame.ui.theme.SudokuGameTheme

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

private enum class Screen { Start, Game }

@Composable
fun App() {
    var current by remember { mutableStateOf(Screen.Start) }
    Crossfade(targetState = current, label = "Screen") { screen ->
        when (screen) {
            Screen.Start -> StartScreen { current = Screen.Game }
            Screen.Game -> GameScreen()
        }
    }
}

@Composable
fun StartScreen(onStart: () -> Unit) {
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
        Button(onClick = onStart) { Text("Start Game") }
    }
}

@Composable
fun GameScreen() {
    var board by remember { mutableStateOf(generateEmptyBoard()) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        SudokuBoard(board, selected) { row, col ->
            selected = row to col
        }
        Spacer(Modifier.height(16.dp))
        NumberPad { number ->
            selected?.let { (r, c) ->
                val newBoard = board.map { it.clone() }.toTypedArray()
                newBoard[r][c] = number
                board = newBoard
            }
        }
    }
}

@Composable
fun NumberPad(onNumberSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 1..9) {
            Button(onClick = { onNumberSelected(i) }) {
                Text(i.toString())
            }
        }
    }
}
