package com.emilioaugust.copypus.data

class ClipboardRepository(private val dao: ClipboardDao) {
    fun getAllItems() = dao.getAllItems()
    fun getAllFavorites() = dao.getAllFavorites()
    suspend fun saveItem(text: String) = dao.insert(ClipboardItem(text = text))

    suspend fun insertItem(item: ClipboardItem) = dao.insert(item)
    suspend fun deleteItem(item: ClipboardItem) = dao.delete(item)

    suspend fun deleteOldItems(time: Long) = dao.deleteOlderThan(time)
    suspend fun clearAll() = dao.clearAll()
    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }
    suspend fun getLatestItem(): ClipboardItem? {
        return dao.getLatestItem()
    }

    suspend fun updateItem(item: ClipboardItem) {
        dao.updateItem(item)
    }

}