package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.IllegalArgumentException

class BookingDetailsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(BookingDetailsViewModel::class.java)) {
            throw IllegalArgumentException("Неизвестный ViewModel класс")
        }
        return BookingDetailsViewModel(context) as T
    }

    companion object {
        @Volatile
        private var INSTANCE: BookingDetailsViewModelFactory? = null

        fun getInstance(context: Context): BookingDetailsViewModelFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BookingDetailsViewModelFactory(context).also { INSTANCE = it }
            }
        }
    }
}