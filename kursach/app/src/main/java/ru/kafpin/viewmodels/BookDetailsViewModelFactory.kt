package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.IllegalArgumentException

class BookDetailsViewModelFactory(
    private val context: Context,
    private val bookId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(BookDetailsViewModel::class.java)) {
            throw IllegalArgumentException("Неизвестный ViewModel класс")
        }
        return BookDetailsViewModel(context, bookId) as T
    }
}