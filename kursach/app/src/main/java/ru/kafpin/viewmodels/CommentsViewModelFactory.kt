package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CommentsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CommentsViewModel::class.java)) {
            throw IllegalArgumentException("Неизвестный ViewModel класс")
        }
        return CommentsViewModel(context) as T
    }

    companion object {
        @Volatile
        private var INSTANCE: CommentsViewModelFactory? = null

        fun getInstance(context: Context): CommentsViewModelFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CommentsViewModelFactory(context).also { INSTANCE = it }
            }
        }
    }
}