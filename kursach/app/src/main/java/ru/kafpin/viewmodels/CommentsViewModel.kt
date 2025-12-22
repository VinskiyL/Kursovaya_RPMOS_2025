package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kafpin.api.models.CommentResponse
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.repositories.CommentRepository

class CommentsViewModel(private val context: Context) : ViewModel() {
    private val TAG = "CommentsViewModel"

    private val authRepository = RepositoryProvider.getAuthRepository(
        LibraryDatabase.getInstance(context),
        context
    )
    private val commentRepository = CommentRepository(authRepository, context)

    private val _comments = MutableStateFlow<List<CommentResponse>>(emptyList())
    val comments: StateFlow<List<CommentResponse>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showEmptyState = MutableStateFlow(false)
    val showEmptyState: StateFlow<Boolean> = _showEmptyState.asStateFlow()

    private val _emptyStateText = MutableStateFlow("Нет комментариев")
    val emptyStateText: StateFlow<String> = _emptyStateText.asStateFlow()

    fun loadComments() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = commentRepository.getComments()

            if (result.isSuccess) {
                val commentsList = result.getOrDefault(emptyList())
                _comments.value = commentsList
                _showEmptyState.value = commentsList.isEmpty()
                _emptyStateText.value = "Нет комментариев"
            } else {
                _comments.value = emptyList()
                _showEmptyState.value = true
                _emptyStateText.value = "Нет подключения"
                _errorMessage.value = result.exceptionOrNull()?.message
            }

            _isLoading.value = false
        }
    }

    fun addComment(text: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = commentRepository.createComment(text)

            if (result.isSuccess) {
                loadComments()
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
            }

            _isLoading.value = false
        }
    }

    fun updateComment(id: Long, text: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = commentRepository.updateComment(id, text)

            if (result.isSuccess) {
                loadComments()
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
            }

            _isLoading.value = false
        }
    }

    fun deleteComment(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = commentRepository.deleteComment(id)

            if (result.isSuccess) {
                loadComments()
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun isMyComment(comment: CommentResponse): Boolean {
        val currentUser = authRepository.getCurrentUserSync()
        return currentUser != null && comment.userId == currentUser.id
    }
}