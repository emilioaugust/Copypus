package com.emilioaugust.copypus.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emilioaugust.copypus.data.entity.SnippetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(snippet: SnippetEntity): Long

    @Update
    suspend fun update(snippet: SnippetEntity)

    @Delete
    suspend fun delete(snippet: SnippetEntity)

    @Query("SELECT * FROM snippets ORDER BY `key` COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets ORDER BY `key` COLLATE NOCASE ASC")
    suspend fun getAll(): List<SnippetEntity>

    @Query("SELECT * FROM snippets WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): SnippetEntity?

    @Query("SELECT * FROM snippets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SnippetEntity?

    @Query("DELETE FROM snippets")
    suspend fun clearAll()
}