package ru.kafpin.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import ru.kafpin.R
import ru.kafpin.activities.SettingsActivity

object NotificationHelper {
    private const val CHANNEL_ID = "bookings_channel"
    private const val CHANNEL_NAME = "Бронирования"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о статусе бронирований"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showStatusChangeNotification(
        context: Context,
        bookingId: Long,
        bookTitle: String,
        oldStatus: String,
        newStatus: String
    ) {
        if (!SettingsActivity.isNotificationsEnabled(context)) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val (title, message) = when (newStatus) {
            "CONFIRMED" -> "✅ Бронь подтверждена" to "Бронь на '$bookTitle' подтверждена"
            "ISSUED" -> "📚 Книга выдана" to "Книга '$bookTitle' выдана"
            "RETURNED" -> "🔄 Книга возвращена" to "Книга '$bookTitle' возвращена"
            "DELETED" -> "🗑️ Бронь удалена" to "Бронь на '$bookTitle' удалена"
            else -> "ℹ️ Статус изменён" to "Статус брони на '$bookTitle' изменён"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(bookingId.toInt(), notification)
    }

    fun showBookingCreatedNotification(context: Context, bookTitle: String, bookingId: Long) {
        if (!SettingsActivity.isNotificationsEnabled(context)) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📋 Бронь создана")
            .setContentText("Бронь на '$bookTitle' создана. Статус: Ожидает")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify("creation_${bookingId}".hashCode(), notification)
    }

    fun showPendingBookingExpiredNotification(
        context: Context,
        bookTitle: String,
        bookingId: Long
    ) {
        if (!SettingsActivity.isNotificationsEnabled(context)) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏳ Бронь удалена")
            .setContentText("Бронь на '$bookTitle' удалена из-за долгого отсутствия интернета")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Используем отрицательный ID чтобы не конфликтовать с реальными бронями
        notificationManager.notify((-bookingId).toInt(), notification)
    }
}