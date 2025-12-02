package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BookDetailsViewModelFactory(
    private val context: Context,
    private val bookId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookDetailsViewModel::class.java)) {
            return BookDetailsViewModel(context, bookId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}