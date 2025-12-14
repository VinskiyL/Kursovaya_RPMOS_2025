package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.repositories.BookRepository

class BookViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != BookViewModel::class.java) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        val app = context.applicationContext as ru.kafpin.MyApplication
        val networkMonitor = app.networkMonitor

        val bookRepository = BookRepository(context)

        val database = LibraryDatabase.getInstance(context)
        val bookDetailsRepository = RepositoryProvider.getBookDetailsRepository(database)

        return BookViewModel(
            bookRepository = bookRepository,
            bookDetailsRepository = bookDetailsRepository,
            context = context
        ) as T
    }
}