package com.example.ui.canvas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.JsonHelper
import com.example.data.model.CanvasNode
import com.example.data.model.NodeType
import com.example.ui.theme.ChecklistNodeAccent
import com.example.ui.theme.ChecklistNodeBg
import com.example.ui.theme.ChecklistNodeBorder
import com.example.ui.theme.CodeNodeAccent
import com.example.ui.theme.CodeNodeBg
import com.example.ui.theme.CodeNodeBorder
import com.example.ui.theme.CodeNodeStatusDot
import com.example.ui.theme.ConceptNodeAccent
import com.example.ui.theme.ConceptNodeBg
import com.example.ui.theme.ConceptNodeBorder
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensityTextMuted
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.SectionNodeAccent
import com.example.ui.theme.SectionNodeBg
import com.example.ui.theme.SectionNodeBorder
import com.example.ui.theme.StickyNodeAccent
import com.example.ui.theme.StickyNodeBg
import com.example.ui.theme.StickyNodeBorder
import kotlin.math.roundToInt

@Composable
fun NodeView(
    node: CanvasNode,
    isSelected: Boolean,
    isConnectingSource: Boolean,
    panX: Float,
    panY: Float,
    zoom: Float,
    onTap: () -> Unit,
    onDrag: (deltaXWorld: Float, deltaYWorld: Float) -> Unit,
    onResize: (deltaWidth: Float, deltaHeight: Float) -> Unit,
    onEdit: () -> Unit,
    onBranch: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onStartConnect: () -> Unit,
    onColorChange: (String) -> Unit,
    onChecklistToggle: (Int, Boolean) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showColorPalette by remember { mutableStateOf(false) }

    val screenX = panX + node.x * zoom
    val screenY = panY + node.y * zoom
    val screenWidth = (node.width * zoom).coerceAtLeast(140f)
    val screenHeight = (node.height * zoom).coerceAtLeast(100f)

    // Node Type styling mapping from High Density Design theme
    val (nodeBgColor, nodeBorderColor, nodeBadgeText, nodeBadgeColor) = when (node.type) {
        NodeType.IDEA -> Quadruple(
            ConceptNodeBg,
            ConceptNodeBorder,
            "CONCEPT",
            ConceptNodeAccent
        )
        NodeType.STICKY -> Quadruple(
            StickyNodeBg,
            StickyNodeBorder,
            "STICKY NOTE",
            StickyNodeAccent
        )
        NodeType.CODE -> Quadruple(
            CodeNodeBg,
            CodeNodeBorder,
            "CODE BLOCK",
            CodeNodeAccent
        )
        NodeType.CHECKLIST -> Quadruple(
            ChecklistNodeBg,
            ChecklistNodeBorder,
            "CHECKLIST",
            ChecklistNodeAccent
        )
        NodeType.SECTION -> Quadruple(
            SectionNodeBg,
            SectionNodeBorder,
            "SECTION",
            SectionNodeAccent
        )
    }

    val borderModifier = when {
        isConnectingSource -> Modifier.border(
            width = 2.5.dp,
            color = MaterialTheme.colorScheme.secondary,
            shape = RoundedCornerShape(14.dp)
        )
        isSelected -> Modifier.border(
            width = 2.5.dp,
            color = HighDensityBlue,
            shape = RoundedCornerShape(14.dp)
        )
        else -> Modifier.border(
            width = 1.dp,
            color = nodeBorderColor,
            shape = RoundedCornerShape(14.dp)
        )
    }

    Box(
        modifier = modifier
            .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
            .size(width = screenWidth.dp, height = screenHeight.dp)
    ) {
        // Main High Density Node Card
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (isSelected) 10.dp else 4.dp,
                    shape = RoundedCornerShape(14.dp)
                )
                .then(borderModifier)
                .pointerInput(node.id, zoom) {
                    detectTapGestures { onTap() }
                }
                .pointerInput(node.id, zoom) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x / zoom, dragAmount.y / zoom)
                    }
                }
                .testTag("node_${node.id}"),
            shape = RoundedCornerShape(14.dp),
            color = nodeBgColor,
            tonalElevation = if (isSelected) 4.dp else 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // High Density Node Header: Uppercase category badge + title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = nodeBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = nodeBadgeColor,
                        fontSize = (9f * zoom).coerceIn(7.5f, 11f).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )

                    if (node.type == NodeType.CODE) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(CodeNodeStatusDot)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = node.codeLanguage.ifBlank { "kotlin" },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontSize = (8.5f * zoom).coerceIn(7f, 10f).sp
                            )
                        }
                    }
                }

                // Title Line (tight tracking)
                Text(
                    text = node.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (node.type == NodeType.CODE) Color.White else HighDensityTextPrimary,
                    fontSize = (12.5f * zoom).coerceIn(10f, 16f).sp,
                    lineHeight = (15f * zoom).coerceIn(12f, 18f).sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Node Body Content based on NodeType
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (node.type) {
                        NodeType.CHECKLIST -> HighDensityChecklistContent(
                            content = node.content,
                            zoom = zoom,
                            onToggle = onChecklistToggle
                        )
                        NodeType.CODE -> HighDensityCodeContent(
                            content = node.content,
                            zoom = zoom,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Code", node.content))
                                Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                        NodeType.STICKY -> HighDensityStickyContent(
                            content = node.content,
                            zoom = zoom
                        )
                        NodeType.SECTION -> HighDensitySectionContent(
                            content = node.content,
                            zoom = zoom
                        )
                        NodeType.IDEA -> HighDensityIdeaContent(
                            content = node.content,
                            zoom = zoom
                        )
                    }
                }
            }
        }

        // High Density Floating Action Bar above selected node
        if (isSelected && zoom >= 0.4f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-42).dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    // Connect button
                    IconButton(
                        onClick = onStartConnect,
                        modifier = Modifier.size(32.dp).testTag("action_connect_${node.id}")
                    ) {
                        Icon(
                            Icons.Default.Hub,
                            contentDescription = "Connect to another node",
                            tint = HighDensityBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // AI Branch button
                    IconButton(
                        onClick = onBranch,
                        modifier = Modifier.size(32.dp).testTag("action_branch_${node.id}")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Branch",
                            tint = ConceptNodeAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Color Picker button
                    IconButton(
                        onClick = { showColorPalette = !showColorPalette },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Change color",
                            tint = nodeBadgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Edit button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("action_edit_${node.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit node",
                            tint = HighDensityTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Duplicate button
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicate node",
                            tint = HighDensityTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("action_delete_${node.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete node",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Popout Color Palette
            if (showColorPalette) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-80).dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "#0B57D0", "#0284C7", "#10B981",
                            "#F59E0B", "#E11D48", "#6750A4", "#475569"
                        ).forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .pointerInput(hex) {
                                        detectTapGestures {
                                            onColorChange(hex)
                                            showColorPalette = false
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        // High Density Bottom-Right Corner Resize Handle
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 5.dp, y = 5.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(HighDensityBlue)
                    .border(2.dp, Color.White, CircleShape)
                    .shadow(3.dp, CircleShape)
                    .pointerInput(node.id, zoom) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x / zoom, dragAmount.y / zoom)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun HighDensityIdeaContent(content: String, zoom: Float) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodySmall,
        color = HighDensityTextSecondary,
        fontWeight = FontWeight.Medium,
        fontSize = (11f * zoom).coerceIn(8f, 14f).sp,
        lineHeight = (14f * zoom).coerceIn(10f, 17f).sp,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun HighDensityStickyContent(content: String, zoom: Float) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF3E2723),
        fontWeight = FontWeight.Normal,
        fontSize = (11f * zoom).coerceIn(8f, 14f).sp,
        lineHeight = (14f * zoom).coerceIn(10f, 17f).sp,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun HighDensitySectionContent(content: String, zoom: Float) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodySmall,
        color = HighDensityTextSecondary,
        fontWeight = FontWeight.Medium,
        fontSize = (11f * zoom).coerceIn(8f, 14f).sp,
        lineHeight = (14f * zoom).coerceIn(10f, 17f).sp,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun HighDensityCodeContent(content: String, zoom: Float, onCopy: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141218))
            .padding(6.dp)
    ) {
        Text(
            text = content,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFE2E2E6),
            fontSize = (9.5f * zoom).coerceIn(7f, 12f).sp,
            lineHeight = (13f * zoom).coerceIn(9f, 15f).sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HighDensityChecklistContent(
    content: String,
    zoom: Float,
    onToggle: (Int, Boolean) -> Unit
) {
    val items = remember(content) { JsonHelper.parseChecklist(content) }
    val doneCount = items.count { it.checked }
    val progress = if (items.isNotEmpty()) doneCount.toFloat() / items.size else 0f

    Column(modifier = Modifier.fillMaxSize()) {
        if (items.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$doneCount/${items.size} done",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = (8.5f * zoom).coerceIn(7.5f, 11f).sp,
                    color = HighDensityBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.5.dp).clip(CircleShape),
                color = HighDensityBlue,
                trackColor = HighDensityBorder
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            items.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .border(
                                width = 1.dp,
                                color = if (item.checked) HighDensityBlue else HighDensityTextMuted,
                                shape = RoundedCornerShape(3.dp)
                            )
                            .background(if (item.checked) HighDensityBlue else Color.Transparent)
                            .pointerInput(item.checked) {
                                detectTapGestures { onToggle(index, !item.checked) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.checked) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = (10f * zoom).coerceIn(8f, 13f).sp,
                        color = if (item.checked) HighDensityTextMuted else HighDensityTextPrimary,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
