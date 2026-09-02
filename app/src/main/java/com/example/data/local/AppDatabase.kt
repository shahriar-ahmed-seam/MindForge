package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.Board
import com.example.data.model.CanvasNode
import com.example.data.model.Connection
import com.example.data.model.NodeType
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val panX: Float,
    val panY: Float,
    val zoom: Float,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Board = Board(
        id = id,
        name = name,
        description = description,
        panX = panX,
        panY = panY,
        zoom = zoom,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(board: Board): BoardEntity = BoardEntity(
            id = board.id,
            name = board.name,
            description = board.description,
            panX = board.panX,
            panY = board.panY,
            zoom = board.zoom,
            createdAt = board.createdAt,
            updatedAt = board.updatedAt
        )
    }
}

@Entity(tableName = "canvas_nodes")
data class CanvasNodeEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val type: String,
    val title: String,
    val content: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val colorHex: String,
    val tags: String,
    val codeLanguage: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): CanvasNode = CanvasNode(
        id = id,
        boardId = boardId,
        type = runCatching { NodeType.valueOf(type) }.getOrDefault(NodeType.IDEA),
        title = title,
        content = content,
        x = x,
        y = y,
        width = width,
        height = height,
        colorHex = colorHex,
        tags = tags,
        codeLanguage = codeLanguage,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(node: CanvasNode): CanvasNodeEntity = CanvasNodeEntity(
            id = node.id,
            boardId = node.boardId,
            type = node.type.name,
            title = node.title,
            content = node.content,
            x = node.x,
            y = node.y,
            width = node.width,
            height = node.height,
            colorHex = node.colorHex,
            tags = node.tags,
            codeLanguage = node.codeLanguage,
            createdAt = node.createdAt,
            updatedAt = node.updatedAt
        )
    }
}

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val label: String,
    val style: String,
    val colorHex: String,
    val createdAt: Long
) {
    fun toDomain(): Connection = Connection(
        id = id,
        boardId = boardId,
        fromNodeId = fromNodeId,
        toNodeId = toNodeId,
        label = label,
        style = style,
        colorHex = colorHex,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(conn: Connection): ConnectionEntity = ConnectionEntity(
            id = conn.id,
            boardId = conn.boardId,
            fromNodeId = conn.fromNodeId,
            toNodeId = conn.toNodeId,
            label = conn.label,
            style = conn.style,
            colorHex = conn.colorHex,
            createdAt = conn.createdAt
        )
    }
}

@Dao
interface CanvasDao {
    // Boards
    @Query("SELECT * FROM boards ORDER BY updatedAt DESC")
    fun getAllBoards(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE id = :boardId LIMIT 1")
    suspend fun getBoardById(boardId: String): BoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: BoardEntity)

    @Update
    suspend fun updateBoard(board: BoardEntity)

    @Query("UPDATE boards SET panX = :panX, panY = :panY, zoom = :zoom, updatedAt = :updatedAt WHERE id = :boardId")
    suspend fun updateBoardViewport(boardId: String, panX: Float, panY: Float, zoom: Float, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM boards WHERE id = :boardId")
    suspend fun deleteBoard(boardId: String)

    // Nodes
    @Query("SELECT * FROM canvas_nodes WHERE boardId = :boardId ORDER BY createdAt ASC")
    fun getNodesByBoard(boardId: String): Flow<List<CanvasNodeEntity>>

    @Query("SELECT * FROM canvas_nodes WHERE boardId = :boardId")
    suspend fun getNodesByBoardOnce(boardId: String): List<CanvasNodeEntity>

    @Query("SELECT * FROM canvas_nodes WHERE id = :nodeId LIMIT 1")
    suspend fun getNodeById(nodeId: String): CanvasNodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: CanvasNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<CanvasNodeEntity>)

    @Update
    suspend fun updateNode(node: CanvasNodeEntity)

    @Query("UPDATE canvas_nodes SET x = :x, y = :y, updatedAt = :updatedAt WHERE id = :nodeId")
    suspend fun updateNodePosition(nodeId: String, x: Float, y: Float, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_nodes SET width = :width, height = :height, updatedAt = :updatedAt WHERE id = :nodeId")
    suspend fun updateNodeSize(nodeId: String, width: Float, height: Float, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM canvas_nodes WHERE id = :nodeId")
    suspend fun deleteNode(nodeId: String)

    @Query("DELETE FROM canvas_nodes WHERE boardId = :boardId")
    suspend fun deleteNodesByBoard(boardId: String)

    // Connections
    @Query("SELECT * FROM connections WHERE boardId = :boardId")
    fun getConnectionsByBoard(boardId: String): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE boardId = :boardId")
    suspend fun getConnectionsByBoardOnce(boardId: String): List<ConnectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: ConnectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnections(connections: List<ConnectionEntity>)

    @Query("DELETE FROM connections WHERE id = :connectionId")
    suspend fun deleteConnection(connectionId: String)

    @Query("DELETE FROM connections WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
    suspend fun deleteConnectionsForNode(nodeId: String)

    @Query("DELETE FROM connections WHERE boardId = :boardId")
    suspend fun deleteConnectionsByBoard(boardId: String)
}

@Database(
    entities = [BoardEntity::class, CanvasNodeEntity::class, ConnectionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun canvasDao(): CanvasDao
}
