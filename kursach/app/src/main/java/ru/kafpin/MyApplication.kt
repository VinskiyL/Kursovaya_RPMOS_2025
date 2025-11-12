package ru.kafpin

import android.app.Application

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Здесь позже инициализируем БД
    }
}