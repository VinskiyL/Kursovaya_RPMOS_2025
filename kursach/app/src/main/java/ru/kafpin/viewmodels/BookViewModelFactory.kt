package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.kafpin.repositories.BookRepository

class BookViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            val repository = BookRepository(context)
            val networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor

            return BookViewModel(repository, networkMonitor, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}