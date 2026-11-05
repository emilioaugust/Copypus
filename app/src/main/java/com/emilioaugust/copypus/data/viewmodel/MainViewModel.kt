package com.emilioaugust.copypus.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emilioaugust.copypus.data.database.AppDatabase
import com.emilioaugust.copypus.data.enums.AutoDeleteOption
import com.emilioaugust.copypus.data.entity.ClipboardItem
import com.emilioaugust.copypus.data.repository.ClipboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository =
        ClipboardRepository(application.applicationContext, AppDatabase.Companion.getInstance(application).clipboardDao())

    private var lastSavedText: String? = null
    val items = repository.getAllItems().stateIn(scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val images = repository.getAllImages().stateIn(scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val favoriteItems = repository.getAllFavorites().stateIn(scope = viewModelScope,
        started = SharingStarted.WhileSubscribed((5000)), initialValue = emptyList())

    fun saveText(text: String) {
        if (text.isBlank()) return
        if (text == lastSavedText) return
        lastSavedText = text
        viewModelScope.launch {
            repository.saveItem(text)
        }
    }

    fun saveImage(
        text: String,
        imagePath: String,
        imageHash: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val existingImage = repository.getImageByHash(imageHash)

            if (existingImage != null) {
                File(imagePath).delete()

                withContext(Dispatchers.Main) {
                    onResult(false)
                }

                return@launch
            }

            repository.saveImage(
                imageFileName = text,
                imagePath = imagePath,
                imageHash = imageHash
            )

            withContext(Dispatchers.Main) {
                onResult(true)
            }
        }
    }

    fun deleteItem(item: ClipboardItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun restoreItem(item: ClipboardItem) {
        viewModelScope.launch {
            repository.insertItem(item)
        }
    }

    fun cleanupOldItems(option: AutoDeleteOption) {
        if (option == AutoDeleteOption.NEVER) {
            return
        }
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val deleteBefore = currentTime - option.toMillis()
            repository.deleteOldItems(deleteBefore)
        }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    fun clearAllImages() {
        viewModelScope.launch { repository.clearAllImages() }
    }

    fun toggleFavorite(item: ClipboardItem) {

        viewModelScope.launch {

            repository.updateFavorite(
                id = item.id,
                isFavorite = !item.isFavorite
            )
        }
    }

    fun updateItem(item: ClipboardItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }
}