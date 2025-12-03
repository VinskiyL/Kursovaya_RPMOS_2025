package ru.kafpin.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.kafpin.MyApplication
import ru.kafpin.repositories.BookRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "SyncWorker"

    init {
        Log.d(TAG, "🔧 SyncWorker создан!")
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🚀 WorkManager запустил SyncWorker")

        try {
            // Получаем NetworkMonitor из Application
            val app = applicationContext as MyApplication
            val networkMonitor = app.networkMonitor

            // Проверяем доступность сервера
            Log.d(TAG, "🌐 Проверяем NetworkMonitor.isOnline...")
            val isOnline = networkMonitor.isOnline.value

            if (!isOnline) {
                Log.d(TAG, "❌ Сервер недоступен, пропускаем синхронизацию")
                return Result.success() // Не retry, чтобы не засорять очередь
            }

            Log.d(TAG, "✅ Сервер доступен, начинаем синхронизацию...")

            val repository = BookRepository(applicationContext)
            val success = repository.syncBooks()

            if(success){ return Result.success()}
            return Result.failure()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка в SyncWorker", e)
            return Result.failure()
        }
    }
}