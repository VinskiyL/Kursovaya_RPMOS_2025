package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDate

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,

    val serverId: Long? = null,

    val bookId: Long,
    val bookTitle: String,
    val bookAuthors: String,
    val bookGenres: String,
    val availableCopies: Int,

    val userId: Long,

    val quantity: Int,
    val dateIssue: String,
    val dateReturn: String,

    val status: BookingStatus,
    val markedForDeletion: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class BookingStatus {
    PENDING,    // Создана локально, не синхронизирована
    CONFIRMED,  // Сервер подтвердил (issued=false)
    ISSUED,     // Книга выдана (issued=true, returned=false)
    RETURNED    // Книга возвращена (issued=true, returned=true)
}