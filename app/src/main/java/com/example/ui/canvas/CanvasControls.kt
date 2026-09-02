package com.example.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NodeType
import com.example.ui.theme.ChecklistNodeAccent
import com.example.ui.theme.CodeNodeAccent
import com.example.ui.theme.ConceptNodeAccent
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensityLightBlue
import com.example.ui.theme.HighDensityNavy
import com.example.ui.theme.HighDensityTextMuted
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.SectionNodeAccent
import com.example.ui.theme.StickyNodeAccent
import kotlin.math.roundToInt

@Composable
fun CanvasControls(
    zoom: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    isMultiSelect: Boolean,
    isConnecting: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onFitNodes: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onCancelConnecting: () -> Unit,
    onAddNode: (NodeType) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAddMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Active Connecting High Density Banner
        AnimatedVisibility(
            visible = isConnecting,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = HighDensityLightBlue,
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(20.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT TARGET NODE TO LINK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityNavy,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onCancelConnecting,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel connection",
                            tint = HighDensityNavy,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Expanded Node Type Selector Popup (High Density Card)
        AnimatedVisibility(
            visible = isAddMenuExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    NodeCreationMenuItem(
                        icon = Icons.Default.Lightbulb,
                        category = "CONCEPT",
                        title = "Semantic Thought / Idea",
                        color = ConceptNodeAccent,
                        onClick = {
                            onAddNode(NodeType.IDEA)
                            isAddMenuExpanded = false
                        }
                    )
                    NodeCreationMenuItem(
                        icon = Icons.Default.NoteAlt,
                        category = "STICKY NOTE",
                        title = "Quick Sticky Memo",
                        color = StickyNodeAccent,
                        onClick = {
                            onAddNode(NodeType.STICKY)
                            isAddMenuExpanded = false
                        }
                    )
                    NodeCreationMenuItem(
                        icon = Icons.Default.Checklist,
                        category = "CHECKLIST",
                        title = "Action Task Checklist",
                        color = ChecklistNodeAccent,
                        onClick = {
                            onAddNode(NodeType.CHECKLIST)
                            isAddMenuExpanded = false
                        }
                    )
                    NodeCreationMenuItem(
                        icon = Icons.Default.Code,
                        category = "CODE BLOCK",
                        title = "Syntax Snippet",
                        color = CodeNodeAccent,
                        onClick = {
                            onAddNode(NodeType.CODE)
                            isAddMenuExpanded = false
                        }
                    )
                    NodeCreationMenuItem(
                        icon = Icons.Default.FormatShapes,
                        category = "SECTION",
                        title = "Group / Project Header",
                        color = SectionNodeAccent,
                        onClick = {
                            onAddNode(NodeType.SECTION)
                            isAddMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Bottom HUD: [High Density Navigation & Zoom Pill] and [High Density FAB]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Density Viewport Pill: bg-white border border-[#E1E2E9] rounded-full shadow-lg
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(32.dp).testTag("button_undo")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            modifier = Modifier.size(16.dp),
                            tint = if (canUndo) HighDensityTextPrimary else HighDensityTextMuted.copy(alpha = 0.4f)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier.size(32.dp).testTag("button_redo")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            modifier = Modifier.size(16.dp),
                            tint = if (canRedo) HighDensityTextPrimary else HighDensityTextMuted.copy(alpha = 0.4f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(1.dp)
                            .background(HighDensityBorder)
                    )

                    // Zoom Out
                    IconButton(
                        onClick = onZoomOut,
                        modifier = Modifier.size(32.dp).testTag("button_zoom_out")
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Zoom out",
                            modifier = Modifier.size(16.dp),
                            tint = HighDensityTextPrimary
                        )
                    }

                    // Zoom Percent: text-[10px] font-bold text-[#44474E]
                    Text(
                        text = "${(zoom * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary,
                        modifier = Modifier
                            .clickable { onResetZoom() }
                            .padding(horizontal = 4.dp)
                            .testTag("text_zoom_percent"),
                        fontSize = 11.sp
                    )

                    // Zoom In
                    IconButton(
                        onClick = onZoomIn,
                        modifier = Modifier.size(32.dp).testTag("button_zoom_in")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Zoom in",
                            modifier = Modifier.size(16.dp),
                            tint = HighDensityTextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(1.dp)
                            .background(HighDensityBorder)
                    )

                    // Fit to View
                    IconButton(
                        onClick = onFitNodes,
                        modifier = Modifier.size(32.dp).testTag("button_fit_view")
                    ) {
                        Icon(
                            Icons.Default.CenterFocusStrong,
                            contentDescription = "Fit to view",
                            modifier = Modifier.size(16.dp),
                            tint = HighDensityTextPrimary
                        )
                    }

                    // Multi-Select Toggle
                    IconButton(
                        onClick = onToggleMultiSelect,
                        modifier = Modifier.size(32.dp).testTag("button_multiselect")
                    ) {
                        Icon(
                            Icons.Default.SelectAll,
                            contentDescription = "Toggle multi-select",
                            modifier = Modifier.size(16.dp),
                            tint = if (isMultiSelect) HighDensityBlue else HighDensityTextMuted
                        )
                    }
                }
            }

            // High Density Add Node FAB: 56.dp, rounded-2xl (16.dp), bg-[#D3E3FD], icon [#041E49]
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(10.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(HighDensityLightBlue)
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp))
                    .clickable { isAddMenuExpanded = !isAddMenuExpanded }
                    .testTag("fab_add_node"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAddMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Add node",
                    tint = HighDensityNavy,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun NodeCreationMenuItem(
    icon: ImageVector,
    category: String,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = HighDensityTextPrimary,
                fontSize = 11.sp
            )
        }
    }
}
