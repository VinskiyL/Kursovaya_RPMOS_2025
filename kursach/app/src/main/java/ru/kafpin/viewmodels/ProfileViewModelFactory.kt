package ru.kafpin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            throw IllegalArgumentException("Неизвестный ViewModel класс")
        }
        return ProfileViewModel(context) as T
    }

    companion object {
        @Volatile
        private var INSTANCE: ProfileViewModelFactory? = null

        fun getInstance(context: Context): ProfileViewModelFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileViewModelFactory(context).also { INSTANCE = it }
            }
        }
    }
}