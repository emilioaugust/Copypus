package com.emilioaugust.copypus.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.emilioaugust.copypus.data.dao.ClipboardDao
import com.emilioaugust.copypus.data.entity.ClipboardItem
import com.emilioaugust.copypus.widget.ClipboardWidget
import kotlinx.coroutines.flow.Flow

class ClipboardRepository(context: Context, private val dao: ClipboardDao) {
    private val appContext = context.applicationContext

    private suspend fun refreshWidget() {
        ClipboardWidget().updateAll(appContext)
    }

    fun getAllItems() = dao.getAllItems()

    fun getAllImages() = dao.getAllImages()
    fun getAllFavorites() = dao.getAllFavorites()

    suspend fun getImageByHash(imageHash: String): ClipboardItem? = dao.getImageByHash(imageHash)
    suspend fun saveItem(text: String) {
        dao.insert(ClipboardItem(text = text))
        refreshWidget()
    }

    suspend fun insertItem(item: ClipboardItem) {
        dao.insert(item)
        refreshWidget()
    }

    suspend fun saveImage(imageFileName: String, imagePath: String, imageHash: String) {
        dao.insert(
            ClipboardItem(
                text = imageFileName,
                imagePath = imagePath,
                imageHash = imageHash,
                type = "IMAGE"
            )
        )
        refreshWidget()
    }
    suspend fun deleteItem(item: ClipboardItem) = dao.delete(item)

    suspend fun deleteOldItems(time: Long) = dao.deleteOlderThan(time)
    suspend fun clearAll() = dao.clearAll()

    suspend fun clearAllImages() = dao.clearAllImages()
    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }
    suspend fun getLatestItem(): ClipboardItem? {
        return dao.getLatestItem()
    }

    fun observeLatestItem(): Flow<ClipboardItem?> {
        return dao.observeLatestItem()
    }

    suspend fun updateItem(item: ClipboardItem) {
        dao.updateItem(item)
    }

    suspend fun getAllItemsForBackup(): List<ClipboardItem> {
        return dao.getAllItemsForBackup()
    }

    suspend fun insertForBackup(item: ClipboardItem): Long {
        return dao.insertForBackup(item)
    }

}