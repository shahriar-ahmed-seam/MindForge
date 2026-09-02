package com.example.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CanvasNode
import com.example.ui.theme.HighDensityBlue
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensityCanvasBg
import com.example.ui.theme.HighDensityLightBlue
import com.example.ui.theme.HighDensityNavy
import com.example.ui.theme.HighDensityTextMuted
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary

data class AiActionItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun AiCopilotPanel(
    selectedNodes: List<CanvasNode>,
    totalCanvasNodes: Int,
    isLoading: Boolean,
    actionLabel: String?,
    responseContent: String?,
    onActionSelected: (String) -> Unit,
    onAutoCluster: () -> Unit,
    onAddResponseToCanvas: (String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val aiActions = listOf(
        AiActionItem(
            id = "Summarize",
            title = "Executive Summary",
            description = "Synthesize key ideas into core takeaways",
            icon = Icons.AutoMirrored.Filled.FactCheck,
            color = HighDensityBlue
        ),
        AiActionItem(
            id = "Find Relationships",
            title = "Hidden Synergies",
            description = "Uncover connections between disparate thoughts",
            icon = Icons.AutoMirrored.Filled.AltRoute,
            color = Color(0xFF6750A4)
        ),
        AiActionItem(
            id = "Execution Plan",
            title = "Execution Roadmap",
            description = "Step-by-step phases, priorities & milestones",
            icon = Icons.Default.Timeline,
            color = Color(0xFF10B981)
        ),
        AiActionItem(
            id = "Project Outline",
            title = "Project Spec",
            description = "Structured scope, metrics & deliverables",
            icon = Icons.Default.AccountTree,
            color = Color(0xFF0284C7)
        ),
        AiActionItem(
            id = "Opposing Arguments",
            title = "Devil's Advocate",
            description = "Sharp counterpoints, trade-offs & blind spots",
            icon = Icons.Default.Warning,
            color = Color(0xFFE11D48)
        ),
        AiActionItem(
            id = "Technical Concepts",
            title = "Tech Architecture",
            description = "System patterns, database schemas & code APIs",
            icon = Icons.Default.DeveloperBoard,
            color = Color(0xFF0D9488)
        ),
        AiActionItem(
            id = "Missing Steps",
            title = "Missing Links",
            description = "Detect gaps and overlooked prerequisites",
            icon = Icons.Default.Lightbulb,
            color = Color(0xFFF59E0B)
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

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
            // High Density Copilot Header: Pulse dot + GEMINI COPILOT + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .alpha(pulseAlpha)
                            .background(Color(0xFF3B82F6))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GEMINI COPILOT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedNodes.isNotEmpty())
                            "(${selectedNodes.size} selected)"
                        else
                            "(${totalCanvasNodes} canvas nodes)",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextMuted,
                        fontSize = 9.sp
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("button_close_ai")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close AI panel",
                        tint = HighDensityTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // High Density Action Grid: [AUTO-CLUSTER] & [BRANCH IDEA]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto-Cluster Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAutoCluster() }
                        .testTag("button_auto_cluster")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "AUTO-CLUSTER",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Group notes into spatial semantic clusters",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }

                // Branch Selected Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (selectedNodes.isNotEmpty()) {
                                onActionSelected("Find Relationships")
                            } else {
                                onActionSelected("Summarize")
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (selectedNodes.isNotEmpty()) "BRANCH IDEA" else "SUMMARIZE ALL",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selectedNodes.isNotEmpty())
                                "Expand selected into connected thoughts"
                            else
                                "Synthesize board into key takeaways",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick High Density Chips Scrollbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Summarize All" to "Summarize",
                    "Project Outline" to "Project Outline",
                    "Technical Risks" to "Opposing Arguments",
                    "Execution Plan" to "Execution Plan",
                    "Missing Links" to "Missing Steps"
                ).forEach { (label, actionId) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp))
                            .clickable { onActionSelected(actionId) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityTextPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Loading Indicator
            AnimatedVisibility(visible = isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = HighDensityBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Synthesizing with ${actionLabel ?: "Gemini"}...",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = HighDensityTextSecondary
                    )
                }
            }

            // Results View or Action Grid
            if (!responseContent.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .border(1.dp, HighDensityBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (actionLabel ?: "AI Analysis").uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityBlue,
                                letterSpacing = 0.5.sp
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("AI", responseContent))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = HighDensityTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                Text(
                                    text = responseContent,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HighDensityTextPrimary,
                                    lineHeight = 17.sp,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onAddResponseToCanvas(actionLabel ?: "AI Insight", responseContent)
                                Toast.makeText(context, "Added new node to canvas", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HighDensityLightBlue,
                                contentColor = HighDensityNavy
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("button_add_ai_node")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Pin Insight to Canvas as Node",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "TACTILE INTELLIGENCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(aiActions.size) { index ->
                        val item = aiActions[index]
                        AiActionRowCard(item = item, onClick = { onActionSelected(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AiActionRowCard(
    item: AiActionItem,
    onClick: () -> Unit
) {
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
            .testTag("ai_action_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = HighDensityTextPrimary,
                    fontSize = 12.sp
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = HighDensityTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
