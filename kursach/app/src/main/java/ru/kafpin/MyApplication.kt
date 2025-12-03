package ru.kafpin

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.kafpin.utils.NetworkMonitor
import ru.kafpin.workers.SyncWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    val networkMonitor by lazy {
        NetworkMonitor(this).apply {
            start()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "📱 Application создана")

        // Запускаем NetworkMonitor
        networkMonitor

        // Настраиваем и запускаем WorkManager
        setupWorkManager()
    }

    private fun setupWorkManager() {
        Log.d(TAG, "⚙️ Настраиваем WorkManager...")

        // 1. Создаём Constraints (ограничения)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Любая сеть
            .build()

        // 2. Создаём периодическую работу
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Интервал 15 минут
        )
            .setConstraints(constraints)
            .addTag("BOOK_SYNC") // Тег для управления
            .build()

        // 3. Получаем WorkManager
        val workManager = WorkManager.getInstance(this)

        // 4. Запускаем UNIQUE работу (чтобы не дублировать)
        workManager.enqueueUniquePeriodicWork(
            "UNIQUE_BOOK_SYNC", // Уникальное имя
            ExistingPeriodicWorkPolicy.UPDATE, // Обновляем если уже есть
            syncWorkRequest
        )

        Log.d(TAG, "✅ WorkManager настроен, синхронизация каждые 15 минут")
    }
}