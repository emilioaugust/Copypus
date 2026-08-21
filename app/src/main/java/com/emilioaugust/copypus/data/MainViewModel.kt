package com.emilioaugust.copypus.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ClipboardRepository(AppDatabase.getInstance(application).clipboardDao())
    private var lastSavedText: String? = null
    val items = repository.getAllItems().stateIn(scope = viewModelScope,
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