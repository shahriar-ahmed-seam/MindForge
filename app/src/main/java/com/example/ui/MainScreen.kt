package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.model.NodeType
import com.example.ui.ai.AiCopilotPanel
import com.example.ui.canvas.CanvasControls
import com.example.ui.canvas.CanvasTopBar
import com.example.ui.canvas.InfiniteCanvas
import com.example.ui.dialogs.BranchNodeDialog
import com.example.ui.dialogs.EditNodeDialog
import com.example.ui.dialogs.ExportDialog
import com.example.ui.dialogs.ImportDialog
import com.example.ui.dialogs.NewBoardDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.notebook.SemanticNotebookView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CanvasViewModel

@Composable
fun MainScreen(viewModel: CanvasViewModel) {
    val isSystemDark = isSystemInDarkTheme()
    val isDarkOverride by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkOverride ?: isSystemDark

    MyApplicationTheme(darkTheme = isDark) {
        val activeBoard by viewModel.activeBoard.collectAsState()
        val allBoards by viewModel.allBoards.collectAsState()
        val nodes by viewModel.nodes.collectAsState()
        val connections by viewModel.connections.collectAsState()
        val panX by viewModel.panX.collectAsState()
        val panY by viewModel.panY.collectAsState()
        val zoom by viewModel.zoom.collectAsState()
        val selectedNodeIds by viewModel.selectedNodeIds.collectAsState()
        val isMultiSelect by viewModel.isMultiSelect.collectAsState()
        val connectingFromNodeId by viewModel.connectingFromNodeId.collectAsState()
        val canUndo by viewModel.canUndo.collectAsState()
        val canRedo by viewModel.canRedo.collectAsState()

        val isNotebookOpen by viewModel.isNotebookOpen.collectAsState()
        val isAiCopilotOpen by viewModel.isAiCopilotOpen.collectAsState()
        val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
        val editingNode by viewModel.editingNode.collectAsState()
        val branchingNode by viewModel.branchingNode.collectAsState()
        val exportJson by viewModel.exportJson.collectAsState()
        val isImportOpen by viewModel.isImportOpen.collectAsState()
        val isNewBoardDialogOpen by viewModel.isNewBoardDialogOpen.collectAsState()

        val aiResponse by viewModel.aiResponse.collectAsState()
        val isAiLoading by viewModel.isAiLoading.collectAsState()
        val aiActionLabel by viewModel.aiActionLabel.collectAsState()
        val notebookSearch by viewModel.notebookSearch.collectAsState()
        val notebookFilterType by viewModel.notebookFilterType.collectAsState()

        val updateStatus by viewModel.updateManager.updateStatus.collectAsState()
        val repoOwner by viewModel.githubRepoOwner.collectAsState()
        val repoName by viewModel.githubRepoName.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Infinite Canvas Engine
            InfiniteCanvas(
                nodes = nodes,
                connections = connections,
                selectedNodeIds = selectedNodeIds,
                connectingFromNodeId = connectingFromNodeId,
                panX = panX,
                panY = panY,
                zoom = zoom,
                onPanAndZoomChange = { nx, ny, nz -> viewModel.updatePanAndZoom(nx, ny, nz) },
                onTapEmptyCanvas = { viewModel.clearSelection() },
                onNodeTap = { id -> viewModel.onNodeTap(id) },
                onNodeDrag = { id, dx, dy -> viewModel.onNodeDrag(id, dx, dy) },
                onNodeResize = { id, dw, dh -> viewModel.onNodeResize(id, dw, dh) },
                onEditNode = { node -> viewModel.openEditDialog(node) },
                onBranchNode = { node -> viewModel.openBranchDialog(node) },
                onDuplicateNode = { id -> viewModel.duplicateNode(id) },
                onDeleteNode = { id -> viewModel.deleteNode(id) },
                onStartConnect = { id -> viewModel.startConnectingFrom(id) },
                onNodeColorChange = { id, hex -> viewModel.updateNodeColor(id, hex) },
                onChecklistToggle = { id, idx, checked -> viewModel.updateChecklistItem(id, idx, checked) },
                onAddChecklistItem = { id, text -> viewModel.addChecklistItem(id, text) },
                onDeleteConnection = { id -> viewModel.deleteConnection(id) }
            )

            // 2. Top Bar
            CanvasTopBar(
                activeBoard = activeBoard,
                nodeCount = nodes.size,
                connectionCount = connections.size,
                onOpenNotebook = { viewModel.openNotebook() },
                onOpenAiCopilot = { viewModel.openAiCopilot() },
                onOpenSettings = { viewModel.openSettings() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 3. Canvas Controls HUD (Zoom, Undo, Redo, Add Node)
            CanvasControls(
                zoom = zoom,
                canUndo = canUndo,
                canRedo = canRedo,
                isMultiSelect = isMultiSelect,
                isConnecting = connectingFromNodeId != null,
                onZoomIn = { viewModel.zoomIn() },
                onZoomOut = { viewModel.zoomOut() },
                onResetZoom = { viewModel.resetZoom() },
                onFitNodes = { viewModel.fitToNodes() },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleMultiSelect = { viewModel.toggleMultiSelect() },
                onCancelConnecting = { viewModel.cancelConnecting() },
                onAddNode = { type -> viewModel.addNode(type) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // 4. Semantic Notebook Layer
            AnimatedVisibility(
                visible = isNotebookOpen,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            ) {
                SemanticNotebookView(
                    boards = allBoards,
                    activeBoard = activeBoard,
                    nodes = nodes,
                    searchQuery = notebookSearch,
                    filterType = notebookFilterType,
                    onSearchChange = { viewModel.setNotebookSearch(it) },
                    onFilterChange = { viewModel.setNotebookFilter(it) },
                    onSelectBoard = { viewModel.selectBoard(it) },
                    onNewBoard = { viewModel.openNewBoardDialog() },
                    onDeleteBoard = { viewModel.deleteBoard(it) },
                    onFocusNode = { id -> viewModel.focusOnNode(id) },
                    onClose = { viewModel.closeNotebook() }
                )
            }

            // 5. Ambient Gemini AI Copilot Layer
            AnimatedVisibility(
                visible = isAiCopilotOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                val selectedNodes = nodes.filter { selectedNodeIds.contains(it.id) }
                AiCopilotPanel(
                    selectedNodes = selectedNodes,
                    totalCanvasNodes = nodes.size,
                    isLoading = isAiLoading,
                    actionLabel = aiActionLabel,
                    responseContent = aiResponse,
                    onActionSelected = { action -> viewModel.executeAiAction(action) },
                    onAutoCluster = { viewModel.autoClusterSelectedNodes() },
                    onAddResponseToCanvas = { title, content ->
                        viewModel.addNode(type = NodeType.IDEA, title = title, content = content)
                    },
                    onClose = { viewModel.closeAiCopilot() }
                )
            }

            // 6. Settings Layer
            AnimatedVisibility(
                visible = isSettingsOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SettingsDialog(
                    currentVersionName = viewModel.updateManager.currentVersionName,
                    currentVersionCode = viewModel.updateManager.currentVersionCode,
                    isDarkMode = isDark,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    repoOwner = repoOwner,
                    repoName = repoName,
                    onUpdateRepoInfo = { o, r ->
                        viewModel.githubRepoOwner.value = o
                        viewModel.githubRepoName.value = r
                    },
                    updateStatus = updateStatus,
                    onCheckForUpdates = { viewModel.checkForAppUpdates() },
                    onDownloadUpdate = { url -> viewModel.updateManager.downloadAndInstallApk(url) },
                    onExportWorkspace = { viewModel.prepareExport() },
                    onImportWorkspace = { viewModel.openImportDialog() },
                    onClose = { viewModel.closeSettings() }
                )
            }

            // Dialogs
            editingNode?.let { node ->
                EditNodeDialog(
                    node = node,
                    onDismiss = { viewModel.closeEditDialog() },
                    onSave = { updated -> viewModel.saveEditedNode(updated) }
                )
            }

            branchingNode?.let { node ->
                BranchNodeDialog(
                    node = node,
                    onDismiss = { viewModel.closeBranchDialog() },
                    onBranchSelected = { type -> viewModel.executeBranchIdea(node, type) },
                    onConvertToTasks = {
                        viewModel.closeBranchDialog()
                        viewModel.turnIdeaIntoChecklistNode(node)
                    }
                )
            }

            exportJson?.let { json ->
                ExportDialog(
                    jsonContent = json,
                    onDismiss = { viewModel.closeExportDialog() }
                )
            }

            if (isImportOpen) {
                ImportDialog(
                    onDismiss = { viewModel.closeImportDialog() },
                    onImport = { text -> viewModel.importWorkspace(text) }
                )
            }

            if (isNewBoardDialogOpen) {
                NewBoardDialog(
                    onDismiss = { viewModel.closeNewBoardDialog() },
                    onCreate = { name, desc -> viewModel.createBoard(name, desc) }
                )
            }
        }
    }
}
