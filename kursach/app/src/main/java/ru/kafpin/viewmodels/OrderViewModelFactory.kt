package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.IllegalArgumentException

class OrderViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(OrderViewModel::class.java)) {
            throw IllegalArgumentException("Неизвестный ViewModel класс")
        }
        return OrderViewModel(context) as T
    }

    companion object {
        @Volatile
        private var INSTANCE: OrderViewModelFactory? = null

        fun getInstance(context: Context): OrderViewModelFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OrderViewModelFactory(context).also { INSTANCE = it }
            }
        }
    }
}