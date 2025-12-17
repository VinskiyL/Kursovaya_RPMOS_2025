package ru.kafpin.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.kafpin.data.models.BookingEntity

@Dao
interface BookingDao {
    @Insert
    suspend fun insert(booking: BookingEntity): Long

    @Update
    suspend fun update(booking: BookingEntity)

    @Delete
    suspend fun delete(booking: BookingEntity)

    @Query("SELECT * FROM bookings WHERE localId = :id")
    suspend fun findById(id: Long): BookingEntity?

    @Query("SELECT * FROM bookings WHERE serverId = :serverId")
    suspend fun findByServerId(serverId: Long): BookingEntity?

    @Query("SELECT * FROM bookings WHERE userId = :userId")
    fun getByUserIdFlow(userId: Long): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE status = 'PENDING' AND markedForDeletion = 0")
    suspend fun getPendingForSync(): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE markedForDeletion = 1")
    suspend fun getMarkedForDeletion(): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE LOWER(bookTitle) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchByBookTitle(query: String): List<BookingEntity>

    @Query("DELETE FROM bookings WHERE localId = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM bookings WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM bookings WHERE status = 'PENDING' AND createdAt < :timestamp")
    suspend fun getOldPending(timestamp: Long): List<BookingEntity>

    @Query("""
        SELECT * FROM bookings 
        WHERE bookId = :bookId 
        AND userId = :userId 
        LIMIT 1
    """)
    suspend fun findByBookAndUser(
        bookId: Long,
        userId: Long
    ): BookingEntity?

    @Query("SELECT * FROM bookings WHERE userId = :userId")
    suspend fun getBookingsByUser(userId: Long): List<BookingEntity>

    @Query("DELETE FROM bookings WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Long)
}