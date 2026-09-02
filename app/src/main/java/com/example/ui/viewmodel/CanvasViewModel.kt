package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AIBranchType
import com.example.ai.GeminiCopilotService
import com.example.data.local.AppDatabase
import com.example.data.local.JsonHelper
import com.example.data.model.Board
import com.example.data.model.CanvasNode
import com.example.data.model.ChecklistItem
import com.example.data.model.Connection
import com.example.data.model.NodeType
import com.example.data.model.WorkspaceExport
import com.example.data.remote.UpdateManager
import com.example.data.repository.CanvasRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class UndoSnapshot(
    val nodes: List<CanvasNode>,
    val connections: List<Connection>
)

class CanvasViewModel(application: Application) : AndroidViewModel(application) {

    private val db = androidx.room.Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "mind_canvas.db"
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    val repository = CanvasRepository(db.canvasDao())
    val updateManager = UpdateManager(application)
    val copilotService = GeminiCopilotService()

    val allBoards: StateFlow<List<Board>> = repository.getAllBoards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeBoard = MutableStateFlow<Board?>(null)
    val activeBoard: StateFlow<Board?> = _activeBoard.asStateFlow()

    private val _nodes = MutableStateFlow<List<CanvasNode>>(emptyList())
    val nodes: StateFlow<List<CanvasNode>> = _nodes.asStateFlow()

    private val _connections = MutableStateFlow<List<Connection>>(emptyList())
    val connections: StateFlow<List<Connection>> = _connections.asStateFlow()

    // Viewport
    private val _panX = MutableStateFlow(100f)
    val panX: StateFlow<Float> = _panX.asStateFlow()

    private val _panY = MutableStateFlow(100f)
    val panY: StateFlow<Float> = _panY.asStateFlow()

    private val _zoom = MutableStateFlow(1.0f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()

    // Selection
    private val _selectedNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNodeIds: StateFlow<Set<String>> = _selectedNodeIds.asStateFlow()

    private val _isMultiSelect = MutableStateFlow(false)
    val isMultiSelect: StateFlow<Boolean> = _isMultiSelect.asStateFlow()

    private val _connectingFromNodeId = MutableStateFlow<String?>(null)
    val connectingFromNodeId: StateFlow<String?> = _connectingFromNodeId.asStateFlow()

    // UI Panels
    private val _isNotebookOpen = MutableStateFlow(false)
    val isNotebookOpen: StateFlow<Boolean> = _isNotebookOpen.asStateFlow()

    private val _isAiCopilotOpen = MutableStateFlow(false)
    val isAiCopilotOpen: StateFlow<Boolean> = _isAiCopilotOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _editingNode = MutableStateFlow<CanvasNode?>(null)
    val editingNode: StateFlow<CanvasNode?> = _editingNode.asStateFlow()

    private val _branchingNode = MutableStateFlow<CanvasNode?>(null)
    val branchingNode: StateFlow<CanvasNode?> = _branchingNode.asStateFlow()

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson: StateFlow<String?> = _exportJson.asStateFlow()

    private val _isImportOpen = MutableStateFlow(false)
    val isImportOpen: StateFlow<Boolean> = _isImportOpen.asStateFlow()

    private val _isNewBoardDialogOpen = MutableStateFlow(false)
    val isNewBoardDialogOpen: StateFlow<Boolean> = _isNewBoardDialogOpen.asStateFlow()

    // AI Copilot State
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiActionLabel = MutableStateFlow<String?>(null)
    val aiActionLabel: StateFlow<String?> = _aiActionLabel.asStateFlow()

    // Notebook Search & Filter
    private val _notebookSearch = MutableStateFlow("")
    val notebookSearch: StateFlow<String> = _notebookSearch.asStateFlow()

    private val _notebookFilterType = MutableStateFlow<NodeType?>(null)
    val notebookFilterType: StateFlow<NodeType?> = _notebookFilterType.asStateFlow()

    // Theme Override
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<UndoSnapshot>()
    private val redoStack = mutableListOf<UndoSnapshot>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // GitHub Repo for Updates
    val githubRepoOwner = MutableStateFlow("shahriarseam")
    val githubRepoName = MutableStateFlow("mind-canvas")

    private var nodeCollectionJob: Job? = null
    private var connectionCollectionJob: Job? = null
    private var viewportDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            val defaultBoard = repository.ensureDefaultBoard()
            selectBoard(defaultBoard)
        }
    }

    fun selectBoard(board: Board) {
        _activeBoard.value = board
        _panX.value = board.panX
        _panY.value = board.panY
        _zoom.value = board.zoom
        _selectedNodeIds.value = emptySet()
        _connectingFromNodeId.value = null

        nodeCollectionJob?.cancel()
        nodeCollectionJob = viewModelScope.launch {
            repository.getNodesByBoard(board.id).collect {
                _nodes.value = it
            }
        }

        connectionCollectionJob?.cancel()
        connectionCollectionJob = viewModelScope.launch {
            repository.getConnectionsByBoard(board.id).collect {
                _connections.value = it
            }
        }
    }

    fun createBoard(name: String, description: String = "") {
        viewModelScope.launch {
            val newBoard = Board(
                name = name.ifBlank { "Untitled Board" },
                description = description,
                panX = 100f,
                panY = 100f,
                zoom = 1.0f
            )
            repository.saveBoard(newBoard)
            selectBoard(newBoard)
            _isNewBoardDialogOpen.value = false
        }
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch {
            repository.deleteBoard(boardId)
            val remaining = allBoards.value.filter { it.id != boardId }
            if (remaining.isNotEmpty()) {
                selectBoard(remaining.first())
            } else {
                val fresh = repository.ensureDefaultBoard()
                selectBoard(fresh)
            }
        }
    }

    // --- Viewport Operations ---

    fun updatePanAndZoom(newPanX: Float, newPanY: Float, newZoom: Float) {
        _panX.value = newPanX
        _panY.value = newPanY
        _zoom.value = newZoom.coerceIn(0.2f, 3.0f)

        viewportDebounceJob?.cancel()
        viewportDebounceJob = viewModelScope.launch {
            delay(500)
            activeBoard.value?.let { b ->
                repository.updateViewport(b.id, _panX.value, _panY.value, _zoom.value)
            }
        }
    }

    fun zoomIn() {
        val current = _zoom.value
        updatePanAndZoom(_panX.value, _panY.value, current * 1.25f)
    }

    fun zoomOut() {
        val current = _zoom.value
        updatePanAndZoom(_panX.value, _panY.value, current / 1.25f)
    }

    fun resetZoom() {
        updatePanAndZoom(_panX.value, _panY.value, 1.0f)
    }

    fun fitToNodes() {
        val currentNodes = _nodes.value
        if (currentNodes.isEmpty()) {
            updatePanAndZoom(100f, 100f, 1.0f)
            return
        }
        val minX = currentNodes.minOf { it.x }
        val minY = currentNodes.minOf { it.y }
        val maxX = currentNodes.maxOf { it.x + it.width }
        val maxY = currentNodes.maxOf { it.y + it.height }

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        // Center nodes in viewport
        val screenCenterX = 400f // estimated center
        val screenCenterY = 500f
        val targetPanX = screenCenterX - centerX
        val targetPanY = screenCenterY - centerY

        updatePanAndZoom(targetPanX, targetPanY, 0.85f)
    }

    fun focusOnNode(nodeId: String) {
        val node = _nodes.value.firstOrNull { it.id == nodeId } ?: return
        _selectedNodeIds.value = setOf(nodeId)
        _isNotebookOpen.value = false

        val screenCenterX = 400f
        val screenCenterY = 500f
        val targetPanX = screenCenterX - (node.x + node.width / 2f)
        val targetPanY = screenCenterY - (node.y + node.height / 2f)
        updatePanAndZoom(targetPanX, targetPanY, 1.0f)
    }

    // --- Node CRUD & Gestures ---

    private fun pushUndoSnapshot() {
        undoStack.add(UndoSnapshot(_nodes.value, _connections.value))
        if (undoStack.size > 20) undoStack.removeAt(0)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentSnapshot = UndoSnapshot(_nodes.value, _connections.value)
        redoStack.add(currentSnapshot)
        val previous = undoStack.removeAt(undoStack.lastIndex)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true

        viewModelScope.launch {
            _nodes.value = previous.nodes
            _connections.value = previous.connections
            activeBoard.value?.let { b ->
                repository.saveNodes(previous.nodes)
                repository.saveConnections(previous.connections)
            }
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentSnapshot = UndoSnapshot(_nodes.value, _connections.value)
        undoStack.add(currentSnapshot)
        val next = redoStack.removeAt(redoStack.lastIndex)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()

        viewModelScope.launch {
            _nodes.value = next.nodes
            _connections.value = next.connections
            activeBoard.value?.let { b ->
                repository.saveNodes(next.nodes)
                repository.saveConnections(next.connections)
            }
        }
    }

    fun addNode(
        type: NodeType,
        title: String = "",
        content: String = "",
        colorHex: String? = null
    ) {
        val board = activeBoard.value ?: return
        pushUndoSnapshot()

        // Place near center of current view
        val worldCenterX = (-_panX.value + 400f) / _zoom.value
        val worldCenterY = (-_panY.value + 500f) / _zoom.value

        val defaultColor = when (type) {
            NodeType.IDEA -> "#6366F1"
            NodeType.STICKY -> "#F59E0B"
            NodeType.CODE -> "#8B5CF6"
            NodeType.CHECKLIST -> "#10B981"
            NodeType.SECTION -> "#0EA5E9"
        }

        val defaultContent = when (type) {
            NodeType.CHECKLIST -> JsonHelper.serializeChecklist(
                listOf(
                    ChecklistItem(text = "First task item", checked = false),
                    ChecklistItem(text = "Second task item", checked = false)
                )
            )
            NodeType.CODE -> "// Enter code snippet here\nfun solve() {\n  // TODO\n}"
            else -> content
        }

        val defaultTitle = title.ifBlank {
            when (type) {
                NodeType.IDEA -> "New Thought"
                NodeType.STICKY -> "Note"
                NodeType.CODE -> "Code Block"
                NodeType.CHECKLIST -> "Action List"
                NodeType.SECTION -> "Section Header"
            }
        }

        val (w, h) = when (type) {
            NodeType.SECTION -> 340f to 130f
            NodeType.CHECKLIST -> 290f to 230f
            NodeType.CODE -> 320f to 220f
            NodeType.STICKY -> 260f to 200f
            NodeType.IDEA -> 270f to 180f
        }

        val newNode = CanvasNode(
            id = UUID.randomUUID().toString(),
            boardId = board.id,
            type = type,
            title = defaultTitle,
            content = defaultContent,
            x = worldCenterX - w / 2f,
            y = worldCenterY - h / 2f,
            width = w,
            height = h,
            colorHex = colorHex ?: defaultColor
        )

        viewModelScope.launch {
            repository.saveNode(newNode)
            _selectedNodeIds.value = setOf(newNode.id)
        }
    }

    fun onNodeDrag(nodeId: String, deltaXWorld: Float, deltaYWorld: Float) {
        val currentNodes = _nodes.value
        val target = currentNodes.firstOrNull { it.id == nodeId } ?: return
        val newX = target.x + deltaXWorld
        val newY = target.y + deltaYWorld

        _nodes.value = currentNodes.map {
            if (it.id == nodeId) it.copy(x = newX, y = newY) else it
        }

        viewModelScope.launch {
            repository.updateNodePosition(nodeId, newX, newY)
        }
    }

    fun onNodeResize(nodeId: String, deltaWidth: Float, deltaHeight: Float) {
        val currentNodes = _nodes.value
        val target = currentNodes.firstOrNull { it.id == nodeId } ?: return
        val newWidth = (target.width + deltaWidth).coerceAtLeast(180f)
        val newHeight = (target.height + deltaHeight).coerceAtLeast(120f)

        _nodes.value = currentNodes.map {
            if (it.id == nodeId) it.copy(width = newWidth, height = newHeight) else it
        }

        viewModelScope.launch {
            repository.updateNodeSize(nodeId, newWidth, newHeight)
        }
    }

    fun saveEditedNode(updatedNode: CanvasNode) {
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.saveNode(updatedNode.copy(updatedAt = System.currentTimeMillis()))
            _editingNode.value = null
        }
    }

    fun updateChecklistItem(nodeId: String, itemIndex: Int, isChecked: Boolean) {
        val target = _nodes.value.firstOrNull { it.id == nodeId } ?: return
        val items = JsonHelper.parseChecklist(target.content).toMutableList()
        if (itemIndex in items.indices) {
            items[itemIndex] = items[itemIndex].copy(checked = isChecked)
            val updated = target.copy(
                content = JsonHelper.serializeChecklist(items),
                updatedAt = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.saveNode(updated)
            }
        }
    }

    fun addChecklistItem(nodeId: String, text: String) {
        if (text.isBlank()) return
        val target = _nodes.value.firstOrNull { it.id == nodeId } ?: return
        val items = JsonHelper.parseChecklist(target.content).toMutableList()
        items.add(ChecklistItem(text = text.trim(), checked = false))
        val updated = target.copy(
            content = JsonHelper.serializeChecklist(items),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveNode(updated)
        }
    }

    fun duplicateNode(nodeId: String) {
        val target = _nodes.value.firstOrNull { it.id == nodeId } ?: return
        pushUndoSnapshot()
        val duplicated = target.copy(
            id = UUID.randomUUID().toString(),
            title = "${target.title} (Copy)",
            x = target.x + 40f,
            y = target.y + 40f,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveNode(duplicated)
            _selectedNodeIds.value = setOf(duplicated.id)
        }
    }

    fun deleteNode(nodeId: String) {
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.deleteNode(nodeId)
            _selectedNodeIds.value = _selectedNodeIds.value - nodeId
        }
    }

    fun deleteSelectedNodes() {
        val selected = _selectedNodeIds.value
        if (selected.isEmpty()) return
        pushUndoSnapshot()
        viewModelScope.launch {
            selected.forEach { repository.deleteNode(it) }
            _selectedNodeIds.value = emptySet()
        }
    }

    fun updateNodeColor(nodeId: String, colorHex: String) {
        val target = _nodes.value.firstOrNull { it.id == nodeId } ?: return
        val updated = target.copy(colorHex = colorHex, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.saveNode(updated)
        }
    }

    // --- Selection and Connecting ---

    fun onNodeTap(nodeId: String) {
        val fromId = _connectingFromNodeId.value
        if (fromId != null) {
            // In connection mode
            if (fromId != nodeId) {
                createConnection(fromId, nodeId)
            }
            _connectingFromNodeId.value = null
            return
        }

        if (_isMultiSelect.value) {
            val current = _selectedNodeIds.value.toMutableSet()
            if (current.contains(nodeId)) current.remove(nodeId) else current.add(nodeId)
            _selectedNodeIds.value = current
        } else {
            _selectedNodeIds.value = setOf(nodeId)
        }
    }

    fun clearSelection() {
        _selectedNodeIds.value = emptySet()
        _connectingFromNodeId.value = null
    }

    fun toggleMultiSelect() {
        _isMultiSelect.value = !_isMultiSelect.value
        if (!_isMultiSelect.value && _selectedNodeIds.value.size > 1) {
            _selectedNodeIds.value = emptySet()
        }
    }

    fun startConnectingFrom(nodeId: String) {
        _connectingFromNodeId.value = nodeId
    }

    fun cancelConnecting() {
        _connectingFromNodeId.value = null
    }

    fun createConnection(fromId: String, toId: String, label: String = "") {
        val board = activeBoard.value ?: return
        // Check if connection already exists
        val exists = _connections.value.any {
            (it.fromNodeId == fromId && it.toNodeId == toId) ||
            (it.fromNodeId == toId && it.toNodeId == fromId)
        }
        if (exists) return

        pushUndoSnapshot()
        val conn = Connection(
            id = UUID.randomUUID().toString(),
            boardId = board.id,
            fromNodeId = fromId,
            toNodeId = toId,
            label = label
        )
        viewModelScope.launch {
            repository.saveConnection(conn)
        }
    }

    fun deleteConnection(connectionId: String) {
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.deleteConnection(connectionId)
        }
    }

    // --- AI Copilot Actions ---

    fun executeAiAction(actionName: String) {
        val currentNodes = if (_selectedNodeIds.value.isNotEmpty()) {
            _nodes.value.filter { _selectedNodeIds.value.contains(it.id) }
        } else {
            _nodes.value
        }

        if (currentNodes.isEmpty()) {
            _aiResponse.value = "Please create or select nodes on the canvas first."
            _isAiCopilotOpen.value = true
            return
        }

        _isAiLoading.value = true
        _aiActionLabel.value = actionName
        _isAiCopilotOpen.value = true

        viewModelScope.launch {
            val response = when (actionName) {
                "Summarize" -> copilotService.summarize(currentNodes)
                "Find Relationships" -> copilotService.findRelationships(currentNodes)
                "Project Outline" -> copilotService.generateProjectOutline(currentNodes)
                "Execution Plan" -> copilotService.generateExecutionPlan(currentNodes)
                "Opposing Arguments" -> copilotService.generateOpposingArguments(currentNodes)
                "Technical Concepts" -> copilotService.generateTechnicalConcepts(currentNodes)
                "Missing Steps" -> copilotService.suggestMissingSteps(currentNodes)
                else -> "Unrecognized AI action"
            }
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun autoClusterSelectedNodes() {
        val board = activeBoard.value ?: return
        val selectedNodes = _nodes.value.filter { _selectedNodeIds.value.contains(it.id) }
        if (selectedNodes.isEmpty()) {
            _aiResponse.value = "Select at least 2 nodes to cluster automatically."
            _isAiCopilotOpen.value = true
            return
        }

        pushUndoSnapshot()
        _isAiLoading.value = true
        _aiActionLabel.value = "Auto Cluster"
        _isAiCopilotOpen.value = true

        viewModelScope.launch {
            val clusterResult = copilotService.autoCluster(board.id, selectedNodes)
            repository.saveNode(clusterResult.mainTopicNode)
            repository.saveNodes(clusterResult.subtopicNodes)
            repository.saveConnections(clusterResult.connections)

            _aiResponse.value = "Successfully clustered ${selectedNodes.size} nodes under '${clusterResult.mainTopicNode.title}' with subtopics and connections."
            _isAiLoading.value = false
            _selectedNodeIds.value = setOf(clusterResult.mainTopicNode.id)
        }
    }

    fun executeBranchIdea(node: CanvasNode, branchType: AIBranchType) {
        val board = activeBoard.value ?: return
        pushUndoSnapshot()
        _isAiLoading.value = true
        _branchingNode.value = null

        viewModelScope.launch {
            val newBranches = copilotService.branchIdea(node, branchType)
            repository.saveNodes(newBranches)

            // Connect each branch to original node
            val connections = newBranches.map { branch ->
                Connection(
                    boardId = board.id,
                    fromNodeId = node.id,
                    toNodeId = branch.id,
                    label = branchType.title,
                    colorHex = branchType.colorHex
                )
            }
            repository.saveConnections(connections)
            _isAiLoading.value = false
            _selectedNodeIds.value = newBranches.map { it.id }.toSet()
        }
    }

    fun turnIdeaIntoChecklistNode(node: CanvasNode) {
        val board = activeBoard.value ?: return
        pushUndoSnapshot()
        _isAiLoading.value = true

        viewModelScope.launch {
            val checklistItems = copilotService.turnIdeaIntoChecklist(node)
            val newNode = CanvasNode(
                boardId = board.id,
                type = NodeType.CHECKLIST,
                title = "${node.title} - Tasks",
                content = JsonHelper.serializeChecklist(checklistItems),
                x = node.x + node.width + 40f,
                y = node.y,
                width = 300f,
                height = 240f,
                colorHex = "#10B981"
            )
            repository.saveNode(newNode)
            repository.saveConnection(
                Connection(
                    boardId = board.id,
                    fromNodeId = node.id,
                    toNodeId = newNode.id,
                    label = "Action Items",
                    colorHex = "#10B981"
                )
            )
            _isAiLoading.value = false
            _selectedNodeIds.value = setOf(newNode.id)
        }
    }

    // --- Import / Export ---

    fun prepareExport() {
        val board = activeBoard.value ?: return
        viewModelScope.launch {
            val export = repository.exportWorkspace(board.id)
            if (export != null) {
                _exportJson.value = JsonHelper.serializeWorkspace(export)
            }
        }
    }

    fun importWorkspace(jsonText: String): Boolean {
        val export = JsonHelper.parseWorkspace(jsonText) ?: return false
        pushUndoSnapshot()
        viewModelScope.launch {
            val imported = repository.importWorkspace(export)
            selectBoard(imported)
            _isImportOpen.value = false
        }
        return true
    }

    // --- UI Dialog Toggles ---

    fun openNotebook() { _isNotebookOpen.value = true }
    fun closeNotebook() { _isNotebookOpen.value = false }
    fun openAiCopilot() { _isAiCopilotOpen.value = true }
    fun closeAiCopilot() { _isAiCopilotOpen.value = false }
    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }
    fun openEditDialog(node: CanvasNode) { _editingNode.value = node }
    fun closeEditDialog() { _editingNode.value = null }
    fun openBranchDialog(node: CanvasNode) { _branchingNode.value = node }
    fun closeBranchDialog() { _branchingNode.value = null }
    fun openImportDialog() { _isImportOpen.value = true }
    fun closeImportDialog() { _isImportOpen.value = false }
    fun closeExportDialog() { _exportJson.value = null }
    fun openNewBoardDialog() { _isNewBoardDialogOpen.value = true }
    fun closeNewBoardDialog() { _isNewBoardDialogOpen.value = false }

    fun setNotebookSearch(query: String) { _notebookSearch.value = query }
    fun setNotebookFilter(type: NodeType?) { _notebookFilterType.value = type }
    fun toggleDarkMode() {
        _isDarkMode.value = !(_isDarkMode.value ?: false)
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates(githubRepoOwner.value, githubRepoName.value)
        }
    }
}
