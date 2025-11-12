package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.kafpin.repositories.BookRepository
import ru.kafpin.utils.NetworkMonitor

class BookViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    private val networkMonitor = NetworkMonitor(context)

    init {
        networkMonitor.start() // Запускаем мониторинг сети
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            val repository = BookRepository(context)
            return BookViewModel(repository, networkMonitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}