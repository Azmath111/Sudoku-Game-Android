package com.example.sudokugame

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun SudokuBoard(
    board: Array<IntArray>,
    selected: Pair<Int, Int>?,
    onCellTapped: (Int, Int) -> Unit
) {
    val highlightAlpha by animateFloatAsState(if (selected != null) 0.5f else 0f)

    Canvas(
        modifier = Modifier
            .size(300.dp)
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellSize = size.width / 9
                    val col = (offset.x / cellSize).toInt().coerceIn(0, 8)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, 8)
                    onCellTapped(row, col)
                }
            }
    ) {
        val cellSize = size.width / 9

        // Highlight the currently selected cell
        selected?.let { (r, c) ->
            drawRect(
                color = Color.Blue.copy(alpha = highlightAlpha),
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize)
            )
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
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
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