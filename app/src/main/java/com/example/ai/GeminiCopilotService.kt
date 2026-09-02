package com.example.ai

import com.example.BuildConfig
import com.example.data.local.JsonHelper
import com.example.data.model.CanvasNode
import com.example.data.model.ChecklistItem
import com.example.data.model.Connection
import com.example.data.model.NodeType
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.data.remote.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

enum class AIBranchType(val title: String, val description: String, val colorHex: String) {
    OPPOSING_ARGUMENTS("Opposing Arguments", "Challenges, counterpoints & devil's advocate", "#EF4444"),
    EXECUTION_PLAN("Execution Plan", "Phased roadmap & milestones", "#10B981"),
    RELATED_IDEAS("Related Ideas", "Creative expansions & adjacent thoughts", "#8B5CF6"),
    RISKS_AND_PROBLEMS("Risks & Failure Modes", "Security, scaling & operational pitfalls", "#F59E0B"),
    TECHNICAL_IMPLEMENTATION("Technical Architecture", "Stack, patterns, API contracts & DB", "#06B6D4"),
    QUESTIONS_TO_INVESTIGATE("Questions to Investigate", "Open hypotheses & user inquiries", "#EC4899")
}

data class ClusterResult(
    val mainTopicNode: CanvasNode,
    val subtopicNodes: List<CanvasNode>,
    val connections: List<Connection>
)

class GeminiCopilotService(
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY }
) {
    private val geminiApi = NetworkClient.geminiApi

    private suspend fun callGemini(prompt: String): String? = withContext(Dispatchers.IO) {
        val key = apiKeyProvider()
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return@withContext null
        }
        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt)),
                        role = "user"
                    )
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.7f)
            )
            val response = geminiApi.generateContent(key, request)
            response.getFirstText()?.trim()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun branchIdea(
        sourceNode: CanvasNode,
        branchType: AIBranchType
    ): List<CanvasNode> = withContext(Dispatchers.Default) {
        val prompt = """
            You are a brainstorming copilot on an infinite canvas.
            Generate 3 distinct, high-impact branches for this node:
            Title: "${sourceNode.title}"
            Content: "${sourceNode.content}"
            Branch category: ${branchType.title} (${branchType.description})
            
            Return strictly 3 items formatted as:
            1. [Short Title] - Detailed explanation or action item
            2. [Short Title] - Detailed explanation or action item
            3. [Short Title] - Detailed explanation or action item
        """.trimIndent()

        val rawResponse = callGemini(prompt)
        val branchItems: List<Pair<String, String>> = if (!rawResponse.isNullOrBlank()) {
            parseBranchText(rawResponse)
        } else {
            generateOfflineBranches(sourceNode, branchType)
        }

        // Arrange branches geometrically around the source node
        val baseDistance = 320f
        val angles = listOf(-0.35f, 0f, 0.35f) // radiate to the right

        branchItems.take(3).mapIndexed { index, (branchTitle, branchContent) ->
            val angle = angles.getOrElse(index) { 0f }
            val targetX = sourceNode.x + baseDistance
            val targetY = sourceNode.y + (index - 1) * 210f

            CanvasNode(
                id = UUID.randomUUID().toString(),
                boardId = sourceNode.boardId,
                type = if (branchType == AIBranchType.EXECUTION_PLAN) NodeType.CHECKLIST else NodeType.IDEA,
                title = branchTitle,
                content = if (branchType == AIBranchType.EXECUTION_PLAN) {
                    JsonHelper.serializeChecklist(
                        listOf(
                            ChecklistItem(text = branchContent.take(60)),
                            ChecklistItem(text = "Define verification criteria"),
                            ChecklistItem(text = "Review outcomes")
                        )
                    )
                } else branchContent,
                x = targetX,
                y = targetY,
                width = 270f,
                height = 180f,
                colorHex = branchType.colorHex,
                tags = "${branchType.name.lowercase()}, branched"
            )
        }
    }

    suspend fun autoCluster(
        boardId: String,
        nodes: List<CanvasNode>
    ): ClusterResult = withContext(Dispatchers.Default) {
        if (nodes.isEmpty()) {
            val emptyTopic = CanvasNode(
                boardId = boardId,
                type = NodeType.SECTION,
                title = "Empty Cluster",
                content = "No nodes were selected to cluster."
            )
            return@withContext ClusterResult(emptyTopic, emptyList(), emptyList())
        }

        val nodesSummary = nodes.joinToString("\n") { "- [${it.title}]: ${it.content.take(80)}" }
        val prompt = """
            Analyze these ideas from an infinite canvas brainstorming session:
            $nodesSummary
            
            Synthesize them into:
            1. A single overarching Main Topic title (3-6 words)
            2. Two sub-clusters that organize them cleanly.
            Format:
            TOPIC: [Main Topic Name]
            SUB1: [First Sub-category Name]
            SUB2: [Second Sub-category Name]
        """.trimIndent()

        val response = callGemini(prompt)
        var mainTitle = "Synthesized Topic Cluster"
        var sub1 = "Core Concepts"
        var sub2 = "Action Items & Execution"

        if (!response.isNullOrBlank()) {
            response.lines().forEach { line ->
                when {
                    line.startsWith("TOPIC:", ignoreCase = true) ->
                        mainTitle = line.substringAfter(":").trim()
                    line.startsWith("SUB1:", ignoreCase = true) ->
                        sub1 = line.substringAfter(":").trim()
                    line.startsWith("SUB2:", ignoreCase = true) ->
                        sub2 = line.substringAfter(":").trim()
                }
            }
        } else {
            mainTitle = "Unified: ${nodes.firstOrNull()?.title ?: "Ideas"}"
        }

        // Calculate bounding box of existing nodes to place cluster frame above them
        val minX = nodes.minOfOrNull { it.x } ?: 200f
        val minY = nodes.minOfOrNull { it.y } ?: 200f
        val maxX = nodes.maxOfOrNull { it.x + it.width } ?: 600f

        val centerX = (minX + maxX) / 2f - 180f
        val topicY = minY - 220f

        val mainTopicNode = CanvasNode(
            id = UUID.randomUUID().toString(),
            boardId = boardId,
            type = NodeType.SECTION,
            title = mainTitle,
            content = "Synthesized cluster encompassing ${nodes.size} interconnected nodes.",
            x = centerX,
            y = topicY,
            width = 360f,
            height = 140f,
            colorHex = "#6366F1",
            tags = "cluster, topic"
        )

        // Create subtopic nodes
        val subtopic1 = CanvasNode(
            id = UUID.randomUUID().toString(),
            boardId = boardId,
            type = NodeType.STICKY,
            title = sub1,
            content = "Focus area 1 synthesizing related concepts and assumptions.",
            x = minX - 40f,
            y = topicY + 160f,
            width = 240f,
            height = 150f,
            colorHex = "#3B82F6",
            tags = "subtopic"
        )

        val subtopic2 = CanvasNode(
            id = UUID.randomUUID().toString(),
            boardId = boardId,
            type = NodeType.STICKY,
            title = sub2,
            content = "Focus area 2 grouping execution requirements and deliverables.",
            x = maxX - 200f,
            y = topicY + 160f,
            width = 240f,
            height = 150f,
            colorHex = "#10B981",
            tags = "subtopic"
        )

        val connections = mutableListOf<Connection>()
        connections.add(
            Connection(
                boardId = boardId,
                fromNodeId = mainTopicNode.id,
                toNodeId = subtopic1.id,
                label = "Category A",
                colorHex = "#6366F1"
            )
        )
        connections.add(
            Connection(
                boardId = boardId,
                fromNodeId = mainTopicNode.id,
                toNodeId = subtopic2.id,
                label = "Category B",
                colorHex = "#6366F1"
            )
        )

        // Connect first half of nodes to subtopic1, second half to subtopic2
        val half = (nodes.size + 1) / 2
        nodes.take(half).forEach {
            connections.add(Connection(boardId = boardId, fromNodeId = subtopic1.id, toNodeId = it.id, label = "Relates"))
        }
        nodes.drop(half).forEach {
            connections.add(Connection(boardId = boardId, fromNodeId = subtopic2.id, toNodeId = it.id, label = "Relates"))
        }

        ClusterResult(
            mainTopicNode = mainTopicNode,
            subtopicNodes = listOf(subtopic1, subtopic2),
            connections = connections
        )
    }

    suspend fun summarize(nodes: List<CanvasNode>): String = withContext(Dispatchers.Default) {
        if (nodes.isEmpty()) return@withContext "Please select one or more nodes to summarize."
        val text = nodes.joinToString("\n\n") { "Title: ${it.title}\nContent: ${it.content}" }
        val prompt = "Provide a concise, high-density executive synthesis and key takeaways of the following canvas ideas:\n$text"
        callGemini(prompt) ?: offlineSummary(nodes)
    }

    suspend fun findRelationships(nodes: List<CanvasNode>): String = withContext(Dispatchers.Default) {
        if (nodes.size < 2) return@withContext "Select at least 2 nodes to uncover relationships and synergies."
        val text = nodes.joinToString("\n\n") { "Node: ${it.title}\n${it.content}" }
        val prompt = "Compare these ideas. Highlight 3 synergies, 2 potential contradictions, and recommended connecting bridges:\n$text"
        callGemini(prompt) ?: offlineRelationships(nodes)
    }

    suspend fun generateProjectOutline(nodes: List<CanvasNode>): String = withContext(Dispatchers.Default) {
        val text = nodes.joinToString("\n") { "- ${it.title}: ${it.content.take(60)}" }
        val prompt = "Turn these brainstorming thoughts into a structured project outline with Phases, Deliverables, and Success Metrics:\n$text"
        callGemini(prompt) ?: offlineProjectOutline(nodes)
    }

    suspend fun generateExecutionPlan(nodes: List<CanvasNode>): String = withContext(Dispatchers.Default) {
        val text = nodes.joinToString("\n") { "- ${it.title}" }
        val prompt = "Formulate a pragmatic step-by-step execution roadmap with immediate next actions, short-term milestones, and critical dependencies for:\n$text"
        callGemini(prompt) ?: offlineExecutionPlan(nodes)
    }

    suspend fun generateOpposingArguments(nodes: List<CanvasNode>): String = withContext(Dispatchers.Default) {
        val text = nodes.joinToString("\n") { "- ${it.title}: ${it.content}" }
        val prompt = "Play Devil's Advocate. List 4 sharp counter-arguments, failure modes, and critical assumptions being taken for granted:\n$text"
        callGemini(prompt) ?: offlineOpposingArguments(nodes)
    }

    suspend fun generateTechnicalConcepts(nodes: List<CanvasNode>): String = withContext(Dispatchers.Default) {
        val text = nodes.joinToString("\n") { "- ${it.title}: ${it.content}" }
        val prompt = "Suggest relevant software architecture patterns, data models, protocols, and technical libraries suited for:\n$text"
        callGemini(prompt) ?: offlineTechnicalConcepts(nodes)
    }

    suspend fun suggestMissingSteps(nodes: List<CanvasNode>): String = withContext(Dispatchers.Default) {
        val text = nodes.joinToString("\n") { "- ${it.title}" }
        val prompt = "Identify blind spots, overlooked prerequisites, and missing intermediate steps in this workflow:\n$text"
        callGemini(prompt) ?: offlineMissingSteps(nodes)
    }

    suspend fun turnIdeaIntoChecklist(node: CanvasNode): List<ChecklistItem> = withContext(Dispatchers.Default) {
        val prompt = """
            Convert this idea into 5 concrete, actionable checklist items:
            Idea: "${node.title}"
            Details: "${node.content}"
            Output exactly 5 bullet lines without markdown bolding.
        """.trimIndent()
        val text = callGemini(prompt)
        if (!text.isNullOrBlank()) {
            text.lines()
                .map { it.trim().removePrefix("-").removePrefix("*").replace(Regex("^\\d+\\."), "").trim() }
                .filter { it.isNotBlank() }
                .take(6)
                .map { ChecklistItem(text = it, checked = false) }
        } else {
            listOf(
                ChecklistItem(text = "Research feasibility & constraints for ${node.title}"),
                ChecklistItem(text = "Draft architecture and specification"),
                ChecklistItem(text = "Implement initial prototype or MVP"),
                ChecklistItem(text = "Test edge cases & validate assumptions"),
                ChecklistItem(text = "Deploy and measure feedback")
            )
        }
    }

    // --- Helpers & Offline fallbacks ---

    private fun parseBranchText(text: String): List<Pair<String, String>> {
        val items = mutableListOf<Pair<String, String>>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines) {
            val clean = line.removePrefix("-").removePrefix("*").replace(Regex("^\\d+\\."), "").trim()
            val match = Regex("\\[(.*?)\\][\\s:-]*(.*)").find(clean)
            if (match != null) {
                val (title, desc) = match.destructured
                items.add(title.trim() to desc.trim())
            } else if (clean.contains(" - ")) {
                val parts = clean.split(" - ", limit = 2)
                items.add(parts[0].trim() to parts.getOrElse(1) { "" }.trim())
            } else if (clean.contains(": ")) {
                val parts = clean.split(": ", limit = 2)
                items.add(parts[0].trim() to parts.getOrElse(1) { "" }.trim())
            } else {
                items.add("Branch Idea" to clean)
            }
        }
        return items.ifEmpty {
            listOf("Point 1" to "First insight", "Point 2" to "Second insight", "Point 3" to "Third insight")
        }
    }

    private fun generateOfflineBranches(sourceNode: CanvasNode, type: AIBranchType): List<Pair<String, String>> {
        val title = sourceNode.title
        return when (type) {
            AIBranchType.OPPOSING_ARGUMENTS -> listOf(
                "Scalability Constraint" to "Consider the compute and memory ceiling if data volume expands 100x.",
                "User Cognitive Friction" to "Will everyday users understand this workflow without dedicated onboarding?",
                "Alternative Paradigm" to "What if this were solved purely serverless or with deterministic logic?"
            )
            AIBranchType.EXECUTION_PLAN -> listOf(
                "Phase 1: Spec & Prototyping" to "Define interface boundaries, core entities, and minimal test cases.",
                "Phase 2: Core Integration" to "Implement Room local caching, reactive streams, and gesture bindings.",
                "Phase 3: Hardening & Polish" to "Benchmark render frame rates, offline resilience, and accessibility."
            )
            AIBranchType.RELATED_IDEAS -> listOf(
                "Semantic Search Embedding" to "Index vector embeddings locally to search nodes by conceptual meaning.",
                "Spatial Auto-Layout" to "Force-directed physics simulation to untangle complex node graphs automatically.",
                "Collaborative Sync Export" to "CRDT-based change logs for peer-to-peer workspace syncing."
            )
            AIBranchType.RISKS_AND_PROBLEMS -> listOf(
                "Data Corruption Risk" to "Ensure atomic database transactions when batching node moves.",
                "Memory Leak on Zoom" to "Recycle off-screen canvas paths and clip viewport rendering.",
                "API Quota Limits" to "Graceful degrade with cached responses when rate limits are approached."
            )
            AIBranchType.TECHNICAL_IMPLEMENTATION -> listOf(
                "Jetpack Compose Canvas" to "Hardware-accelerated DrawScope with matrix transformation for scale & pan.",
                "Room Offline Cache" to "Single source of truth utilizing Flow<List<Entity>> for reactive UI updates.",
                "OkHttp Connection Pooling" to "HTTP/2 multiplexing with 60s read/write timeouts for LLM streaming."
            )
            AIBranchType.QUESTIONS_TO_INVESTIGATE -> listOf(
                "What is the primary failure mode?" to "How does the system behave when network drops unexpectedly?",
                "What is the golden metric?" to "Time from opening app to placing first interconnected thought.",
                "Can this be simplified?" to "Which configuration flags can be eliminated with intelligent defaults?"
            )
        }
    }

    private fun offlineSummary(nodes: List<CanvasNode>): String {
        return buildString {
            appendLine("### Executive Synthesis (${nodes.size} Nodes Analyzed)")
            appendLine("• **Core Objective:** Developing a unified workflow across ${nodes.joinToString(", ") { it.title.take(20) }}.")
            appendLine("• **Architecture:** Emphasizes local-first privacy, sub-millisecond tactile interactions, and ambient AI augmentation.")
            appendLine("• **Next Milestone:** Connect loose nodes and validate execution steps in Phase 1.")
        }
    }

    private fun offlineRelationships(nodes: List<CanvasNode>): String {
        return buildString {
            appendLine("### Identified Relationships & Synergies")
            appendLine("1. **Direct Dependency:** `${nodes.getOrNull(0)?.title}` serves as the foundational substrate for `${nodes.getOrNull(1)?.title}`.")
            appendLine("2. **Data Pipeline:** Changes in one node propagate downstream into linked checklist tasks.")
            appendLine("3. **Recommended Link:** Create a directional connector linking inputs to deliverables.")
        }
    }

    private fun offlineProjectOutline(nodes: List<CanvasNode>): String {
        return buildString {
            appendLine("### Structured Project Outline")
            appendLine("#### Phase 1: Exploration & Definition")
            appendLine("- Synthesize core problem statement and target audience constraints.")
            appendLine("- Establish benchmark metrics.")
            appendLine("#### Phase 2: Architecture & Implementation")
            appendLine("- Construct local-first data models and gesture interaction pipeline.")
            appendLine("- Integrate ambient AI helpers and export capabilities.")
            appendLine("#### Phase 3: Validation & Launch")
            appendLine("- Verification tests, automated release CI, and in-app distribution.")
        }
    }

    private fun offlineExecutionPlan(nodes: List<CanvasNode>): String {
        return buildString {
            appendLine("### Pragmatic Execution Plan")
            appendLine("1. [Immediate] Verify core node creation and bidirectional linking.")
            appendLine("2. [Day 2] Polish gesture responsiveness and viewport persistence.")
            appendLine("3. [Day 3] Integrate AI branching and auto-clustering capabilities.")
            appendLine("4. [Day 4] Run end-to-end import/export verification.")
        }
    }

    private fun offlineOpposingArguments(nodes: List<CanvasNode>): String {
        return buildString {
            appendLine("### Devil's Advocate (Critical Considerations)")
            appendLine("• **Over-engineering Risk:** Are we introducing complexity where a simpler list would suffice?")
            appendLine("• **Performance overhead:** Heavy node graphs may tax lower-end mobile chipsets without viewport culling.")
            appendLine("• **User Onboarding:** Infinite canvases require immediate visual signposts to avoid blank-canvas paralysis.")
        }
    }

    private fun offlineTechnicalConcepts(nodes: List<CanvasNode>): String {
        return buildString {
            appendLine("### Recommended Technical Architecture")
            appendLine("• **State Flow:** Unidirectional data flow via ViewModel & MutableStateFlow.")
            appendLine("• **Local Persistence:** Room Database with KSP and custom converters.")
            appendLine("• **Vector Graphics:** Compose Bezier spline rendering for low-overhead curved connections.")
            appendLine("• **Network Protocol:** Retrofit with Moshi serialization and 60-second timeouts.")
        }
    }

    private fun offlineMissingSteps(nodes: List<CanvasNode>): String {
        return buildString {
            appendLine("### Overlooked Prerequisite & Intermediate Steps")
            appendLine("• Missing error handling for low-storage and out-of-memory situations.")
            appendLine("• User feedback for active background operations.")
            appendLine("• Clear visual cues for undo / redo stack availability.")
        }
    }
}
