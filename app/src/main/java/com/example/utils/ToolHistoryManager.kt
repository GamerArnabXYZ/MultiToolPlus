package com.example.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ToolHistoryItem(
    val toolId: String,
    val timestamp: Long
)

object ToolHistoryManager {
    private const val PREFS_NAME = "omni_tool_history_prefs"
    private const val KEY_HISTORY = "recent_tools_history"

    private val _historyFlow = MutableStateFlow<List<ToolHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<ToolHistoryItem>> = _historyFlow.asStateFlow()

    fun init(context: Context) {
        _historyFlow.value = loadHistory(context)
    }

    fun recordToolLaunch(context: Context, toolId: String) {
        val current = loadHistory(context).toMutableList()
        // Remove duplicate if already present so it bumps to the top
        current.removeAll { it.toolId == toolId }
        current.add(0, ToolHistoryItem(toolId = toolId, timestamp = System.currentTimeMillis()))
        val trimmed = current.take(10)
        saveHistory(context, trimmed)
        _historyFlow.value = trimmed
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
        _historyFlow.value = emptyList()
    }

    private fun loadHistory(context: Context): List<ToolHistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ToolHistoryItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ToolHistoryItem(
                        toolId = obj.getString("toolId"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(context: Context, list: List<ToolHistoryItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("toolId", item.toolId)
            obj.put("timestamp", item.timestamp)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = (now - timestamp).coerceAtLeast(0)
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            else -> "${days}d ago"
        }
    }
}
