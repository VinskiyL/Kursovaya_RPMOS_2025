package ru.kafpin

import android.app.Application
import ru.kafpin.utils.NetworkMonitor

class MyApplication : Application() {

    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate() {
        super.onCreate()

        // Инициализируем NetworkMonitor
        networkMonitor = NetworkMonitor(this)
        networkMonitor.start()

        // Можно добавить другие инициализации
        // Например, Timber для логирования
    }

    override fun onTerminate() {
        super.onTerminate()
        networkMonitor.stop()
    }
}