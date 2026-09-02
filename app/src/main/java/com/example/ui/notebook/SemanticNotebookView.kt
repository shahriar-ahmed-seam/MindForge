package com.example.ui.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Board
import com.example.data.model.CanvasNode
import com.example.data.model.NodeType
import com.example.ui.theme.ChecklistNodeAccent
import com.example.ui.theme.CodeNodeAccent
import com.example.ui.theme.ConceptNodeAccent
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensityCanvasBg
import com.example.ui.theme.HighDensityLightBlue
import com.example.ui.theme.HighDensityNavy
import com.example.ui.theme.HighDensityTextMuted
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.SectionNodeAccent
import com.example.ui.theme.StickyNodeAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SemanticNotebookView(
    boards: List<Board>,
    activeBoard: Board?,
    nodes: List<CanvasNode>,
    searchQuery: String,
    filterType: NodeType?,
    onSearchChange: (String) -> Unit,
    onFilterChange: (NodeType?) -> Unit,
    onSelectBoard: (Board) -> Unit,
    onNewBoard: () -> Unit,
    onDeleteBoard: (String) -> Unit,
    onFocusNode: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredNodes = nodes.filter { node ->
        val matchesType = filterType == null || node.type == filterType
        val matchesQuery = searchQuery.isBlank() ||
                node.title.contains(searchQuery, ignoreCase = true) ||
                node.content.contains(searchQuery, ignoreCase = true) ||
                node.tags.contains(searchQuery, ignoreCase = true)
        matchesType && matchesQuery
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = HighDensityCanvasBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // High Density Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SEMANTIC NOTEBOOK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = HighDensityTextPrimary
                    )
                    Text(
                        text = "${nodes.size} THOUGHTS ACROSS CANVAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextMuted,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.4.sp
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("button_close_notebook")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close notebook",
                        tint = HighDensityTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Boards / Canvases Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CANVASES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextMuted,
                    letterSpacing = 0.5.sp
                )
                TextButton(
                    onClick = onNewBoard,
                    modifier = Modifier.testTag("button_new_board")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = HighDensityBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "New Canvas",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(boards) { board ->
                    val isSelected = board.id == activeBoard?.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) HighDensityLightBlue else Color.White)
                            .border(
                                1.dp,
                                if (isSelected) HighDensityBlue else HighDensityBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectBoard(board) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = board.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) HighDensityNavy else HighDensityTextPrimary,
                                fontSize = 11.5.sp
                            )
                            if (boards.size > 1 && !isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteBoard(board.id) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete board",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_notebook_search"),
                placeholder = {
                    Text(
                        "Search title, content, or tags...",
                        fontSize = 12.sp,
                        color = HighDensityTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = HighDensityTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = HighDensityBlue,
                    unfocusedBorderColor = HighDensityBorder
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // High Density Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    NotebookFilterPill(
                        label = "ALL (${nodes.size})",
                        isSelected = filterType == null,
                        onClick = { onFilterChange(null) }
                    )
                }
                item {
                    NotebookFilterPill(
                        label = "CONCEPTS",
                        isSelected = filterType == NodeType.IDEA,
                        onClick = { onFilterChange(if (filterType == NodeType.IDEA) null else NodeType.IDEA) }
                    )
                }
                item {
                    NotebookFilterPill(
                        label = "STICKY NOTES",
                        isSelected = filterType == NodeType.STICKY,
                        onClick = { onFilterChange(if (filterType == NodeType.STICKY) null else NodeType.STICKY) }
                    )
                }
                item {
                    NotebookFilterPill(
                        label = "CHECKLISTS",
                        isSelected = filterType == NodeType.CHECKLIST,
                        onClick = { onFilterChange(if (filterType == NodeType.CHECKLIST) null else NodeType.CHECKLIST) }
                    )
                }
                item {
                    NotebookFilterPill(
                        label = "CODE",
                        isSelected = filterType == NodeType.CODE,
                        onClick = { onFilterChange(if (filterType == NodeType.CODE) null else NodeType.CODE) }
                    )
                }
                item {
                    NotebookFilterPill(
                        label = "SECTIONS",
                        isSelected = filterType == NodeType.SECTION,
                        onClick = { onFilterChange(if (filterType == NodeType.SECTION) null else NodeType.SECTION) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nodes List
            if (filteredNodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No notes match '$searchQuery'" else "No notes found on canvas",
                        style = MaterialTheme.typography.bodySmall,
                        color = HighDensityTextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredNodes, key = { it.id }) { node ->
                        NotebookNoteCard(
                            node = node,
                            onClick = { onFocusNode(node.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotebookFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) HighDensityBlue else Color.White)
            .border(
                1.dp,
                if (isSelected) HighDensityBlue else HighDensityBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else HighDensityTextSecondary,
            fontSize = 9.sp,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun NotebookNoteCard(
    node: CanvasNode,
    onClick: () -> Unit
) {
    val dateStr = remember(node.updatedAt) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(node.updatedAt))
    }

    val (typeIcon, typeCategory, typeColor) = when (node.type) {
        NodeType.IDEA -> Triple(Icons.Default.Lightbulb, "CONCEPT", ConceptNodeAccent)
        NodeType.STICKY -> Triple(Icons.Default.NoteAlt, "STICKY", StickyNodeAccent)
        NodeType.CODE -> Triple(Icons.Default.Code, "CODE", CodeNodeAccent)
        NodeType.CHECKLIST -> Triple(Icons.Default.Checklist, "CHECKLIST", ChecklistNodeAccent)
        NodeType.SECTION -> Triple(Icons.Default.FormatShapes, "SECTION", SectionNodeAccent)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("notebook_item_${node.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeCategory,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = node.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextMuted,
                        fontSize = 9.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = node.content.take(100).replace("\n", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = HighDensityTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )

                if (node.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "#${node.tags.replace(",", " #")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityBlue,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
