package ru.kafpin

import android.app.Application
import android.content.ContentValues.TAG
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.*
import ru.kafpin.utils.NetworkMonitor
import ru.kafpin.utils.NotificationHelper
import ru.kafpin.workers.BookingSyncWorker
import ru.kafpin.workers.DailyExpiryWorker
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

        networkMonitor

        NotificationHelper.createNotificationChannel(this)

        Handler(Looper.getMainLooper()).postDelayed({
            setupWorkManager()
        }, 1000)
    }

    private fun setupWorkManager() {
        Log.d(TAG, "⚙️ Настраиваем WorkManager...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workManager = WorkManager.getInstance(this)

        val bookingSyncRequest = PeriodicWorkRequestBuilder<BookingSyncWorker>(
            1, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(0, TimeUnit.MINUTES)
            .addTag("BOOKING_SYNC")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "UNIQUE_BOOKING_SYNC",
            ExistingPeriodicWorkPolicy.UPDATE,
            bookingSyncRequest
        )

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .addTag("BOOK_SYNC")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "UNIQUE_BOOK_SYNC",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncWorkRequest
        )

        val cleanupRequest = PeriodicWorkRequestBuilder<DailyExpiryWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(16, TimeUnit.MINUTES)
            .addTag("DAILY_CLEANUP")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "UNIQUE_DAILY_CLEANUP",
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest
        )

        Log.d(TAG, "✅ Все Workers настроены")
        Log.d(TAG, "📅 Брони: каждую 1 мин (сразу)")
        Log.d(TAG, "📚 Книги: каждые 15 мин (через 5 мин)")
        Log.d(TAG, "🧹 Очистка: каждые 24 ч")
    }
}