package ru.kafpin.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider

class DailyExpiryWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "DailyExpiryWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "🧹 Запуск DailyExpiryWorker")

        return try {
            val database = LibraryDatabase.getInstance(applicationContext)
            val bookingRepository = RepositoryProvider.getBookingRepository(
                database = database,
                authRepository = RepositoryProvider.getAuthRepository(
                    database,
                    applicationContext
                ),
                context = context
            )

            // Очищаем старые PENDING брони
            bookingRepository.cleanupOldPendingBookings()

            Log.d(TAG, "✅ Очистка завершена")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "💥 Ошибка в DailyExpiryWorker", e)
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "DailyExpiryWorker"
    }
}