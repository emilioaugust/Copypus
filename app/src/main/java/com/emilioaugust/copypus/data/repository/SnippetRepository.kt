package com.emilioaugust.copypus.data.repository

import com.emilioaugust.copypus.data.dao.SnippetDao
import com.emilioaugust.copypus.data.entity.SnippetEntity
import kotlinx.coroutines.flow.Flow
import android.content.Context

class SnippetRepository(
    context: Context,
    private val dao: SnippetDao
) {
    fun observeAll(): Flow<List<SnippetEntity>> = dao.observeAll()

    suspend fun getAll(): List<SnippetEntity> = dao.getAll()

    suspend fun findValueByKey(rawKey: String): String? {
        val normalized = rawKey.trim().lowercase()
        if (normalized.isEmpty()) return null
        return dao.getByKey(normalized)?.value
    }

    suspend fun create(key: String, value: String): Long {
        val normalized = normalizeKey(key)
        require(normalized.isNotEmpty()) { "Snippet key is empty" }
        require(normalized.matches(KEY_REGEX)) {
            "Key may contain only letters, digits, '_' and '-'"
        }
        require(value.isNotEmpty()) { "Snippet value is empty" }
        require(dao.getByKey(normalized) == null) {
            "Snippet with key '$normalized' already exists"
        }
        return dao.insert(
            SnippetEntity(
                key = normalized,
                value = value,
            )
        )
    }

    suspend fun updateSnippet(
        id: Long,
        newKey: String,
        newValue: String
    ) {
        val normalized = normalizeKey(newKey)
        require(normalized.isNotEmpty()) { "Snippet key is empty" }
        require(normalized.matches(KEY_REGEX)) {
            "Key may contain only letters, digits, '_' and '-'"
        }
        require(newValue.isNotEmpty()) { "Snippet value is empty" }

        val existing = dao.getById(id)
            ?: throw IllegalArgumentException("Snippet not found (id=$id)")

        if (existing.key != normalized) {
            val conflict = dao.getByKey(normalized)
            if (conflict != null && conflict.id != id) {
                throw IllegalStateException(
                    "Snippet with key '$normalized' already exists"
                )
            }
        }

        dao.update(
            existing.copy(
                key = normalized,
                value = newValue,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(snippet: SnippetEntity) = dao.delete(snippet)

    suspend fun deleteById(id: Long) {
        val s = dao.getById(id) ?: return
        dao.delete(s)
    }

    suspend fun clearAll() = dao.clearAll()

    private fun normalizeKey(raw: String): String =
        raw.removePrefix(";;").trim().lowercase()

    companion object {
        val KEY_REGEX = Regex("^[\\p{L}0-9_\\-]{1,32}$")
    }
}