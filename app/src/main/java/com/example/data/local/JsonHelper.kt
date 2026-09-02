package com.example.data.local

import com.example.data.model.ChecklistItem
import com.example.data.model.WorkspaceExport
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonHelper {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val checklistType = Types.newParameterizedType(List::class.java, ChecklistItem::class.java)
    private val checklistAdapter = moshi.adapter<List<ChecklistItem>>(checklistType)
    private val workspaceAdapter = moshi.adapter(WorkspaceExport::class.java).indent("  ")

    fun serializeChecklist(items: List<ChecklistItem>): String {
        return runCatching { checklistAdapter.toJson(items) }.getOrDefault("[]")
    }

    fun parseChecklist(json: String): List<ChecklistItem> {
        if (json.isBlank()) return emptyList()
        return runCatching { checklistAdapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    fun serializeWorkspace(export: WorkspaceExport): String {
        return runCatching { workspaceAdapter.toJson(export) }.getOrDefault("")
    }

    fun parseWorkspace(json: String): WorkspaceExport? {
        if (json.isBlank()) return null
        return runCatching { workspaceAdapter.fromJson(json) }.getOrNull()
    }
}
