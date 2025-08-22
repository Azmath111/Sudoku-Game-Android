package com.example.sudokugame

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SudokuBoard(board: Array<IntArray>, selected: Pair<Int, Int>?, onCellTapped: (Int, Int) -> Unit) {
    Canvas(modifier = Modifier.size(300.dp).background(Color.White)) {
        val cellSize = size.width / 9
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val x = c * cellSize
                val y = r * cellSize
                val isSelected = selected?.first == r && selected?.second == c
                val alpha by animateFloatAsState(if (isSelected) 0.5f else 0f)
                drawRect(
                    color = Color.Blue.copy(alpha = alpha),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }
        }
        for (i in 0..9) {
            val stroke = if (i % 3 == 0) 4f else 1f
            drawLine(
                Color.Black,
                start = Offset(i * cellSize, 0f),
                end = Offset(i * cellSize, size.height),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                Color.Black,
                start = Offset(0f, i * cellSize),
                end = Offset(size.width, i * cellSize),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
        board.forEachIndexed { r, row ->
            row.forEachIndexed { c, value ->
                if (value != 0) {
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            value.toString(),
                            c * cellSize + cellSize / 2,
                            r * cellSize + cellSize * 0.75f,
                            android.graphics.Paint().apply {
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = cellSize * 0.8f
                            }
                        )
                    }
                }
            }
        }
    }
}
