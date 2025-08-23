package com.azmath.sudoku

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun SudokuBoard(
    board: Array<IntArray>,
    initial: Array<IntArray>,
    notes: Array<Array<Set<Int>>>,
    selected: Pair<Int, Int>?,
    conflicts: Set<Pair<Int, Int>>,
    dimension: Int,
    blockRows: Int,
    blockCols: Int,
    onCellTapped: (Int, Int) -> Unit
) {
    val highlightAlpha by animateFloatAsState(if (selected != null) 0.3f else 0f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color(0xFF2B2B2B))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellSize = this.size.width / dimension
                    val col = (offset.x / cellSize).toInt().coerceIn(0, dimension - 1)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, dimension - 1)
                    onCellTapped(row, col)
                }
            }
    ) {
        val cellSize = this.size.width / dimension

        // Highlight conflicting cells
        conflicts.forEach { (r, c) ->
            drawRect(
                color = Color.Red.copy(alpha = 0.5f),
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize)
            )
        }

        // Highlight the currently selected cell
        selected?.let { (r, c) ->
            drawRect(
                color = Color.Gray.copy(alpha = highlightAlpha),
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize)
            )
        }

        for (i in 0..dimension) {
            val vStroke = if (i % blockCols == 0) 4f else 1f
            drawLine(
                color = if (i % blockCols == 0) Color.White else Color(0xFF555555),
                start = Offset(i * cellSize, 0f),
                end = Offset(i * cellSize, this.size.height),
                strokeWidth = vStroke,
                cap = StrokeCap.Round
            )
            val hStroke = if (i % blockRows == 0) 4f else 1f
            drawLine(
                color = if (i % blockRows == 0) Color.White else Color(0xFF555555),
                start = Offset(0f, i * cellSize),
                end = Offset(this.size.width, i * cellSize),
                strokeWidth = hStroke,
                cap = StrokeCap.Round
            )
        }
        board.forEachIndexed { r, row ->
            row.forEachIndexed { c, value ->
                if (value != 0) {
                    val given = initial[r][c] != 0
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            value.toString(),
                            c * cellSize + cellSize / 2,
                            r * cellSize + cellSize * 0.75f,
                            android.graphics.Paint().apply {
                                color = if (given) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = cellSize * 0.8f
                            }
                        )
                    }
                } else if (notes[r][c].isNotEmpty()) {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            notes[r][c].sorted().joinToString(""),
                            c * cellSize + cellSize / 2,
                            r * cellSize + cellSize * 0.4f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = cellSize * 0.3f
                            }
                        )
                    }
                }
            }
        }
    }
}