package com.example.ui.canvas

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.CanvasNode
import com.example.data.model.Connection
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ConnectionRenderer(
    connections: List<Connection>,
    nodes: List<CanvasNode>,
    panX: Float,
    panY: Float,
    zoom: Float,
    onConnectionClick: (Connection) -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeMap = nodes.associateBy { it.id }
    val defaultLineColor = com.example.ui.theme.HighDensityLine
    val labelTextColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(connections, panX, panY, zoom) {
                detectTapGestures { tapOffset ->
                    // Find if tap is near any connection midpoint
                    for (conn in connections) {
                        val fromNode = nodeMap[conn.fromNodeId] ?: continue
                        val toNode = nodeMap[conn.toNodeId] ?: continue

                        val (p1, p2) = calculateAnchorPoints(fromNode, toNode, panX, panY, zoom)
                        val midX = (p1.x + p2.x) / 2f
                        val midY = (p1.y + p2.y) / 2f

                        val dist = sqrt((tapOffset.x - midX) * (tapOffset.x - midX) + (tapOffset.y - midY) * (tapOffset.y - midY))
                        if (dist < 32f) {
                            onConnectionClick(conn)
                            return@detectTapGestures
                        }
                    }
                }
            }
    ) {
        val strokeWidth = (2.5f * zoom).coerceIn(1.5f, 6f)

        for (conn in connections) {
            val fromNode = nodeMap[conn.fromNodeId] ?: continue
            val toNode = nodeMap[conn.toNodeId] ?: continue

            val (startPoint, endPoint) = calculateAnchorPoints(fromNode, toNode, panX, panY, zoom)

            val parsedColor = runCatching {
                Color(android.graphics.Color.parseColor(conn.colorHex))
            }.getOrDefault(defaultLineColor)

            // Cubic Bezier curve
            val dx = endPoint.x - startPoint.x
            val dy = endPoint.y - startPoint.y

            // Smooth horizontal curvature
            val controlOffset = (kotlin.math.abs(dx) * 0.45f).coerceAtLeast(40f * zoom)
            val c1 = Offset(startPoint.x + (if (dx >= 0) controlOffset else -controlOffset), startPoint.y)
            val c2 = Offset(endPoint.x - (if (dx >= 0) controlOffset else -controlOffset), endPoint.y)

            val path = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                cubicTo(c1.x, c1.y, c2.x, c2.y, endPoint.x, endPoint.y)
            }

            drawPath(
                path = path,
                color = parsedColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw directional arrowhead at endPoint
            val arrowAngle = atan2(endPoint.y - c2.y, endPoint.x - c2.x)
            val arrowLength = 14f * zoom
            val arrowWingAngle = 0.45f

            val arrowPath = Path().apply {
                moveTo(endPoint.x, endPoint.y)
                lineTo(
                    endPoint.x - arrowLength * cos(arrowAngle - arrowWingAngle).toFloat(),
                    endPoint.y - arrowLength * sin(arrowAngle - arrowWingAngle).toFloat()
                )
                moveTo(endPoint.x, endPoint.y)
                lineTo(
                    endPoint.x - arrowLength * cos(arrowAngle + arrowWingAngle).toFloat(),
                    endPoint.y - arrowLength * sin(arrowAngle + arrowWingAngle).toFloat()
                )
            }

            drawPath(
                path = arrowPath,
                color = parsedColor,
                style = Stroke(width = strokeWidth * 1.1f, cap = StrokeCap.Round)
            )

            // Draw midpoint dot or label
            val midX = (startPoint.x + endPoint.x) / 2f
            val midY = (startPoint.y + endPoint.y) / 2f

            drawCircle(
                color = parsedColor,
                radius = 4.5f * zoom,
                center = Offset(midX, midY)
            )

            if (conn.label.isNotBlank() && zoom >= 0.5f) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = android.graphics.Color.argb(
                            220,
                            (labelTextColor.red * 255).toInt(),
                            (labelTextColor.green * 255).toInt(),
                            (labelTextColor.blue * 255).toInt()
                        )
                        textSize = 12f * zoom
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    drawText(conn.label, midX, midY - (8f * zoom), paint)
                }
            }
        }
    }
}

private fun calculateAnchorPoints(
    from: CanvasNode,
    to: CanvasNode,
    panX: Float,
    panY: Float,
    zoom: Float
): Pair<Offset, Offset> {
    val fromScreenX = panX + from.x * zoom
    val fromScreenY = panY + from.y * zoom
    val fromW = from.width * zoom
    val fromH = from.height * zoom

    val toScreenX = panX + to.x * zoom
    val toScreenY = panY + to.y * zoom
    val toW = to.width * zoom
    val toH = to.height * zoom

    val fromCenterX = fromScreenX + fromW / 2f
    val fromCenterY = fromScreenY + fromH / 2f
    val toCenterX = toScreenX + toW / 2f
    val toCenterY = toScreenY + toH / 2f

    // Pick closest horizontal anchors
    val (startX, endX) = if (fromCenterX < toCenterX) {
        (fromScreenX + fromW) to toScreenX
    } else {
        fromScreenX to (toScreenX + toW)
    }

    return Offset(startX, fromCenterY) to Offset(endX, toCenterY)
}
