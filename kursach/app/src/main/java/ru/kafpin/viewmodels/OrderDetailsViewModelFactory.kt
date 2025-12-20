package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.IllegalArgumentException

class OrderDetailsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(OrderDetailsViewModel::class.java)) {
            throw IllegalArgumentException("Неизвестный ViewModel класс")
        }
        return OrderDetailsViewModel(context) as T
    }

    companion object {
        @Volatile
        private var INSTANCE: OrderDetailsViewModelFactory? = null

        fun getInstance(context: Context): OrderDetailsViewModelFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OrderDetailsViewModelFactory(context).also { INSTANCE = it }
            }
        }
    }
}