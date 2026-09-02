package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class NodeType {
    IDEA,
    STICKY,
    CODE,
    CHECKLIST,
    SECTION
}

@JsonClass(generateAdapter = true)
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val checked: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CanvasNode(
    val id: String = UUID.randomUUID().toString(),
    val boardId: String,
    val type: NodeType = NodeType.IDEA,
    val title: String,
    val content: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 260f,
    val height: Float = 180f,
    val colorHex: String = "#6366F1",
    val tags: String = "",
    val codeLanguage: String = "Kotlin",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class Connection(
    val id: String = UUID.randomUUID().toString(),
    val boardId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val label: String = "",
    val style: String = "CURVED",
    val colorHex: String = "#818CF8",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class Board(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoom: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class WorkspaceExport(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val board: Board,
    val nodes: List<CanvasNode>,
    val connections: List<Connection>
)
