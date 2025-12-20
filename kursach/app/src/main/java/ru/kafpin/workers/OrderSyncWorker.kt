package ru.kafpin.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider

class OrderSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "OrderSyncWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Запуск OrderSyncWorker")

        return try {
            val database = LibraryDatabase.getInstance(applicationContext)
            val authRepository = RepositoryProvider.getAuthRepository(
                database,
                applicationContext
            )
            val orderRepository = RepositoryProvider.getOrderRepository(
                database = database,
                authRepository = authRepository,
                context = context
            )

            val networkMonitor = (applicationContext as ru.kafpin.MyApplication).networkMonitor
            if (!networkMonitor.isOnline.value) {
                Log.d(TAG, "Нет интернета, пропускаем синхронизацию")
                return Result.success()
            }

            if (!authRepository.hasValidTokenForApi()) {
                Log.d(TAG, "Нет валидного токена, пропускаем синхронизацию")
                return Result.success()
            }

            val results = orderRepository.syncPendingOrders()

            val hasSuccess = results.any { it is ru.kafpin.repositories.SyncResult.Success }
            val hasErrors = results.any { it is ru.kafpin.repositories.SyncResult.Error }

            when {
                hasSuccess -> {
                    Log.d(TAG, "Синхронизация завершена успешно (${results.size} результатов)")
                    Result.success()
                }
                hasErrors -> {
                    Log.w(TAG, "Синхронизация завершена с ошибками")
                    Result.retry()
                }
                else -> {
                    Log.w(TAG, "Нет результатов синхронизации")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в OrderSyncWorker", e)
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "OrderSyncWorker"
    }
}