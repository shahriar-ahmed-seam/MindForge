package com.example.ui.canvas

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.CanvasNode
import com.example.data.model.Connection

@Composable
fun InfiniteCanvas(
    nodes: List<CanvasNode>,
    connections: List<Connection>,
    selectedNodeIds: Set<String>,
    connectingFromNodeId: String?,
    panX: Float,
    panY: Float,
    zoom: Float,
    onPanAndZoomChange: (Float, Float, Float) -> Unit,
    onTapEmptyCanvas: () -> Unit,
    onNodeTap: (String) -> Unit,
    onNodeDrag: (nodeId: String, deltaXWorld: Float, deltaYWorld: Float) -> Unit,
    onNodeResize: (nodeId: String, deltaWidth: Float, deltaHeight: Float) -> Unit,
    onEditNode: (CanvasNode) -> Unit,
    onBranchNode: (CanvasNode) -> Unit,
    onDuplicateNode: (String) -> Unit,
    onDeleteNode: (String) -> Unit,
    onStartConnect: (String) -> Unit,
    onNodeColorChange: (nodeId: String, hex: String) -> Unit,
    onChecklistToggle: (nodeId: String, itemIndex: Int, isChecked: Boolean) -> Unit,
    onAddChecklistItem: (nodeId: String, text: String) -> Unit,
    onDeleteConnection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedConnectionToDelete by remember { mutableStateOf<Connection?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Background touch handler: two-finger pinch zoom + pan, or one-finger background pan
            .pointerInput(panX, panY, zoom) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceIn(0.2f, 3.0f)
                    val newPanX = panX + pan.x
                    val newPanY = panY + pan.y
                    onPanAndZoomChange(newPanX, newPanY, newZoom)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { onTapEmptyCanvas() }
            }
    ) {
        // 1. Dot Grid layer
        CanvasGrid(
            panX = panX,
            panY = panY,
            zoom = zoom
        )

        // 2. Connections layer (drawn under nodes)
        ConnectionRenderer(
            connections = connections,
            nodes = nodes,
            panX = panX,
            panY = panY,
            zoom = zoom,
            onConnectionClick = { conn ->
                selectedConnectionToDelete = conn
            }
        )

        // 3. Nodes layer
        nodes.forEach { node ->
            NodeView(
                node = node,
                isSelected = selectedNodeIds.contains(node.id),
                isConnectingSource = connectingFromNodeId == node.id,
                panX = panX,
                panY = panY,
                zoom = zoom,
                onTap = { onNodeTap(node.id) },
                onDrag = { dx, dy -> onNodeDrag(node.id, dx, dy) },
                onResize = { dw, dh -> onNodeResize(node.id, dw, dh) },
                onEdit = { onEditNode(node) },
                onBranch = { onBranchNode(node) },
                onDuplicate = { onDuplicateNode(node.id) },
                onDelete = { onDeleteNode(node.id) },
                onStartConnect = { onStartConnect(node.id) },
                onColorChange = { hex -> onNodeColorChange(node.id, hex) },
                onChecklistToggle = { idx, checked -> onChecklistToggle(node.id, idx, checked) },
                onAddChecklistItem = { text -> onAddChecklistItem(node.id, text) }
            )
        }
    }

    // Connection Delete / Inspect Dialog
    selectedConnectionToDelete?.let { conn ->
        AlertDialog(
            onDismissRequest = { selectedConnectionToDelete = null },
            title = { Text("Connection Link") },
            text = {
                Text(
                    if (conn.label.isNotBlank())
                        "Link label: \"${conn.label}\"\nWould you like to remove this connection?"
                    else
                        "Remove the visual link connecting these two nodes?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConnection(conn.id)
                        selectedConnectionToDelete = null
                    }
                ) {
                    Text("Delete Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedConnectionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
