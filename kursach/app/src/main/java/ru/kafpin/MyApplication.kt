package ru.kafpin

import android.app.Application
import ru.kafpin.utils.NetworkMonitor

class MyApplication : Application() {
    val networkMonitor by lazy {
        NetworkMonitor(this).apply {
            start()
        }
    }
}