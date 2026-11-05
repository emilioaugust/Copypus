package com.emilioaugust.copypus.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emilioaugust.copypus.data.entity.ClipboardItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(item: ClipboardItem)

    @Query("""
        SELECT *
        FROM clipboard_items
        WHERE type = 'TEXT'
        ORDER BY timestamp DESC
    """)
    fun getAllItems(): Flow<List<ClipboardItem>>

    @Query("""
        SELECT *
        FROM clipboard_items
        WHERE type = 'IMAGE'
        ORDER BY timestamp DESC
    """)
    fun getAllImages(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<ClipboardItem>>

    @Query("""
    SELECT * FROM clipboard_items
    WHERE imageHash = :imageHash
    LIMIT 1
    """)
    suspend fun getImageByHash(imageHash: String): ClipboardItem?

    @Query("DELETE FROM clipboard_items WHERE timestamp < :time")
    suspend fun deleteOlderThan(time: Long)

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE type = 'TEXT'")
    suspend fun clearAll()

    @Query("DELETE FROM clipboard_items WHERE type = 'IMAGE'")
    suspend fun clearAllImages()

    @Query("UPDATE clipboard_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("""
    SELECT * FROM clipboard_items
    ORDER BY timestamp DESC
    LIMIT 1
""")
    suspend fun getLatestItem(): ClipboardItem?

    @Query("""
    SELECT * FROM clipboard_items
    ORDER BY timestamp DESC, id DESC
    LIMIT 1
""")
    fun observeLatestItem(): Flow<ClipboardItem?>

    @Update
    suspend fun updateItem(item: ClipboardItem)

    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    suspend fun getAllItemsForBackup(): List<ClipboardItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertForBackup(item: ClipboardItem): Long

}