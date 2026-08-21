package com.emilioaugust.copypus.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
@Dao
interface ClipboardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ClipboardItem)

    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE isFavorite = 1")
    fun getAllFavorites(): Flow<List<ClipboardItem>>

    @Query("DELETE FROM clipboard_items WHERE timestamp < :time")
    suspend fun deleteOlderThan(time: Long)

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items")
    suspend fun clearAll()

    @Query("UPDATE clipboard_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("""
    SELECT * FROM clipboard_items
    ORDER BY timestamp DESC
    LIMIT 1
""")
    suspend fun getLatestItem(): ClipboardItem?

    @Update
    suspend fun updateItem(item: ClipboardItem)

}