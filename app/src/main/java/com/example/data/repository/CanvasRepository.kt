package com.example.data.repository

import com.example.data.local.BoardEntity
import com.example.data.local.CanvasDao
import com.example.data.local.CanvasNodeEntity
import com.example.data.local.ConnectionEntity
import com.example.data.local.JsonHelper
import com.example.data.model.Board
import com.example.data.model.CanvasNode
import com.example.data.model.ChecklistItem
import com.example.data.model.Connection
import com.example.data.model.NodeType
import com.example.data.model.WorkspaceExport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class CanvasRepository(private val canvasDao: CanvasDao) {

    fun getAllBoards(): Flow<List<Board>> {
        return canvasDao.getAllBoards().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getBoardById(boardId: String): Board? {
        return canvasDao.getBoardById(boardId)?.toDomain()
    }

    fun getNodesByBoard(boardId: String): Flow<List<CanvasNode>> {
        return canvasDao.getNodesByBoard(boardId).map { list -> list.map { it.toDomain() } }
    }

    fun getConnectionsByBoard(boardId: String): Flow<List<Connection>> {
        return canvasDao.getConnectionsByBoard(boardId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun saveBoard(board: Board) {
        canvasDao.insertBoard(BoardEntity.fromDomain(board))
    }

    suspend fun updateViewport(boardId: String, panX: Float, panY: Float, zoom: Float) {
        canvasDao.updateBoardViewport(boardId, panX, panY, zoom)
    }

    suspend fun deleteBoard(boardId: String) {
        canvasDao.deleteConnectionsByBoard(boardId)
        canvasDao.deleteNodesByBoard(boardId)
        canvasDao.deleteBoard(boardId)
    }

    suspend fun saveNode(node: CanvasNode) {
        canvasDao.insertNode(CanvasNodeEntity.fromDomain(node))
    }

    suspend fun saveNodes(nodes: List<CanvasNode>) {
        canvasDao.insertNodes(nodes.map { CanvasNodeEntity.fromDomain(it) })
    }

    suspend fun updateNodePosition(nodeId: String, x: Float, y: Float) {
        canvasDao.updateNodePosition(nodeId, x, y)
    }

    suspend fun updateNodeSize(nodeId: String, width: Float, height: Float) {
        canvasDao.updateNodeSize(nodeId, width, height)
    }

    suspend fun deleteNode(nodeId: String) {
        canvasDao.deleteConnectionsForNode(nodeId)
        canvasDao.deleteNode(nodeId)
    }

    suspend fun saveConnection(connection: Connection) {
        canvasDao.insertConnection(ConnectionEntity.fromDomain(connection))
    }

    suspend fun saveConnections(connections: List<Connection>) {
        canvasDao.insertConnections(connections.map { ConnectionEntity.fromDomain(it) })
    }

    suspend fun deleteConnection(connectionId: String) {
        canvasDao.deleteConnection(connectionId)
    }

    suspend fun exportWorkspace(boardId: String): WorkspaceExport? {
        val board = canvasDao.getBoardById(boardId)?.toDomain() ?: return null
        val nodes = canvasDao.getNodesByBoardOnce(boardId).map { it.toDomain() }
        val connections = canvasDao.getConnectionsByBoardOnce(boardId).map { it.toDomain() }
        return WorkspaceExport(
            board = board,
            nodes = nodes,
            connections = connections
        )
    }

    suspend fun importWorkspace(export: WorkspaceExport): Board {
        val newBoardId = UUID.randomUUID().toString()
        val importedBoard = export.board.copy(
            id = newBoardId,
            name = "${export.board.name} (Imported)",
            updatedAt = System.currentTimeMillis()
        )
        canvasDao.insertBoard(BoardEntity.fromDomain(importedBoard))

        val oldToNewIdMap = mutableMapOf<String, String>()
        val newNodes = export.nodes.map { oldNode ->
            val newNodeId = UUID.randomUUID().toString()
            oldToNewIdMap[oldNode.id] = newNodeId
            oldNode.copy(
                id = newNodeId,
                boardId = newBoardId,
                updatedAt = System.currentTimeMillis()
            )
        }
        canvasDao.insertNodes(newNodes.map { CanvasNodeEntity.fromDomain(it) })

        val newConnections = export.connections.mapNotNull { oldConn ->
            val newFrom = oldToNewIdMap[oldConn.fromNodeId] ?: oldConn.fromNodeId
            val newTo = oldToNewIdMap[oldConn.toNodeId] ?: oldConn.toNodeId
            if (oldToNewIdMap.containsKey(oldConn.fromNodeId) && oldToNewIdMap.containsKey(oldConn.toNodeId)) {
                oldConn.copy(
                    id = UUID.randomUUID().toString(),
                    boardId = newBoardId,
                    fromNodeId = newFrom,
                    toNodeId = newTo
                )
            } else null
        }
        if (newConnections.isNotEmpty()) {
            canvasDao.insertConnections(newConnections.map { ConnectionEntity.fromDomain(it) })
        }

        return importedBoard
    }

    suspend fun ensureDefaultBoard(): Board {
        val existing = canvasDao.getBoardById("default_board")
        if (existing != null) {
            return existing.toDomain()
        }

        val defaultBoard = Board(
            id = "default_board",
            name = "Project Brainstorm",
            description = "Main infinite canvas for ideas, architecture & AI branches",
            panX = 150f,
            panY = 150f,
            zoom = 0.95f
        )
        canvasDao.insertBoard(BoardEntity.fromDomain(defaultBoard))

        // Create sample seed nodes to demonstrate all 5 node types and connections
        val node1 = CanvasNode(
            id = "node_welcome_1",
            boardId = "default_board",
            type = NodeType.SECTION,
            title = "Mind Canvas Architecture",
            content = "Infinite canvas + semantic notebook + ambient Gemini copilot",
            x = 200f,
            y = 80f,
            width = 340f,
            height = 140f,
            colorHex = "#6366F1",
            tags = "architecture, overview"
        )

        val node2 = CanvasNode(
            id = "node_welcome_2",
            boardId = "default_board",
            type = NodeType.IDEA,
            title = "Infinite Workspace",
            content = "• Smooth drag, pinch-to-zoom & panning\n• Resizable & duplicatable nodes\n• Preserves exact viewport on reopen",
            x = 100f,
            y = 280f,
            width = 280f,
            height = 190f,
            colorHex = "#3B82F6",
            tags = "canvas, gesture, local-first"
        )

        val node3 = CanvasNode(
            id = "node_welcome_3",
            boardId = "default_board",
            type = NodeType.STICKY,
            title = "Ambient Gemini AI",
            content = "Tap any node and choose 'AI Branch' or open the Copilot panel to auto-cluster, summarize, or generate execution steps!",
            x = 440f,
            y = 280f,
            width = 260f,
            height = 190f,
            colorHex = "#F59E0B",
            tags = "gemini, copilot, ideas"
        )

        val checklistItems = listOf(
            ChecklistItem(text = "Infinite canvas panning & zooming", checked = true),
            ChecklistItem(text = "Connect nodes with smart curves", checked = true),
            ChecklistItem(text = "Try AI Branching on sticky note", checked = false),
            ChecklistItem(text = "Export workspace to JSON", checked = false)
        )

        val node4 = CanvasNode(
            id = "node_welcome_4",
            boardId = "default_board",
            type = NodeType.CHECKLIST,
            title = "Launch Checklist",
            content = JsonHelper.serializeChecklist(checklistItems),
            x = 100f,
            y = 520f,
            width = 300f,
            height = 240f,
            colorHex = "#10B981",
            tags = "tasks, roadmap"
        )

        val node5 = CanvasNode(
            id = "node_welcome_5",
            boardId = "default_board",
            type = NodeType.CODE,
            title = "Local-first Flow",
            content = "// Pure Room DB offline persistence\nfun observeBoard(id: String): Flow<BoardState> {\n  return repository.getBoardState(id)\n}",
            x = 450f,
            y = 520f,
            width = 320f,
            height = 220f,
            colorHex = "#8B5CF6",
            tags = "kotlin, room, offline",
            codeLanguage = "Kotlin"
        )

        canvasDao.insertNodes(listOf(node1, node2, node3, node4, node5).map { CanvasNodeEntity.fromDomain(it) })

        val conn1 = Connection(
            id = "conn_1",
            boardId = "default_board",
            fromNodeId = "node_welcome_1",
            toNodeId = "node_welcome_2",
            label = "Canvas Engine",
            style = "CURVED",
            colorHex = "#6366F1"
        )
        val conn2 = Connection(
            id = "conn_2",
            boardId = "default_board",
            fromNodeId = "node_welcome_1",
            toNodeId = "node_welcome_3",
            label = "Ambient Intelligence",
            style = "CURVED",
            colorHex = "#8B5CF6"
        )
        val conn3 = Connection(
            id = "conn_3",
            boardId = "default_board",
            fromNodeId = "node_welcome_2",
            toNodeId = "node_welcome_4",
            label = "Action Items",
            style = "CURVED",
            colorHex = "#10B981"
        )

        canvasDao.insertConnections(listOf(conn1, conn2, conn3).map { ConnectionEntity.fromDomain(it) })

        return defaultBoard
    }
}
