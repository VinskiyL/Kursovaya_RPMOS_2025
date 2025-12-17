package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.IllegalArgumentException

class BookingViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(BookingViewModel::class.java)) {
            throw IllegalArgumentException("Неизвестный ViewModel класс")
        }
        return BookingViewModel(context) as T
    }

    companion object {
        @Volatile
        private var INSTANCE: BookingViewModelFactory? = null

        fun getInstance(context: Context): BookingViewModelFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BookingViewModelFactory(context).also { INSTANCE = it }
            }
        }
    }
}