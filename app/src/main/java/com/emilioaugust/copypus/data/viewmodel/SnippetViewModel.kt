package com.emilioaugust.copypus.data.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emilioaugust.copypus.data.database.AppDatabase
import com.emilioaugust.copypus.data.entity.SnippetEntity
import com.emilioaugust.copypus.data.repository.SnippetRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SnippetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository =
        SnippetRepository(application.applicationContext,
            AppDatabase.Companion.getInstance(application).snippetDao()
        )

    val items = repository.observeAll().stateIn(scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    private val _uiEvent = MutableSharedFlow<SnippetEvent>()
    val uiEvent: SharedFlow<SnippetEvent> = _uiEvent

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    fun createSnippet(key: String, value: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.create(key = key, value = value)
                _saveError.value = null
                _uiEvent.emit(SnippetEvent.Saved)
                onSuccess()
            } catch (e: IllegalArgumentException) {
                _saveError.value = e.message ?: "Invalid key"
            } catch (e: IllegalStateException) {
                // дубликат ключа
                _saveError.value = e.message ?: "Snippet already exists"
            } catch (e: Exception) {
                _saveError.value = "Save failed: ${e.message}"
                Log.e("SnippetVM", "createSnippet failed", e)
            }
        }
    }

    fun deleteSnippet(snippet: SnippetEntity) {
        viewModelScope.launch {
            repository.delete(snippet)
        }
    }

    fun updateSnippet(
        item: SnippetEntity,
        newKey: String,
        newValue: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateSnippet(
                    id = item.id,
                    newKey = newKey,
                    newValue = newValue
                )
                _saveError.value = null
                onSuccess()
            } catch (e: IllegalArgumentException) {
                _saveError.value = e.message ?: "Invalid key"
            } catch (e: IllegalStateException) {
                _saveError.value = e.message ?: "Snippet with this key already exists"
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                _saveError.value = "Snippet with this key already exists"
            } catch (e: Exception) {
                _saveError.value = "Update failed: ${e.message}"
                Log.e("SnippetVM", "updateSnippet failed", e)
            }
        }
    }

    fun clearError() { _saveError.value = null }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    sealed interface SnippetEvent {
        data object Saved : SnippetEvent
    }
}