package com.azmath.sudoku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color(0xFF2B2B2B),
    onBackground = Color.White,
    surface = Color(0xFF2B2B2B),
    onSurface = Color.White
)

@Composable
fun SudokuGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content
    )
}
