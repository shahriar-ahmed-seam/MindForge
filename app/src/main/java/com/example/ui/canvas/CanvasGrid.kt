package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.example.ui.theme.HighDensityGridDot

@Composable
fun CanvasGrid(
    panX: Float,
    panY: Float,
    zoom: Float,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val dotRadius = (1.2f * zoom).coerceIn(0.8f, 2.5f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val baseGridSize = 24f * zoom
        if (baseGridSize < 8f) return@Canvas

        val width = size.width
        val height = size.height

        val startX = (panX % baseGridSize) - baseGridSize
        val startY = (panY % baseGridSize) - baseGridSize

        var x = startX
        while (x < width + baseGridSize) {
            var y = startY
            while (y < height + baseGridSize) {
                drawCircle(
                    color = gridColor,
                    radius = dotRadius,
                    center = Offset(x, y)
                )
                y += baseGridSize
            }
            x += baseGridSize
        }
    }
}
