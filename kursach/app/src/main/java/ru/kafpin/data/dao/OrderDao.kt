package ru.kafpin.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.kafpin.data.models.OrderEntity
import ru.kafpin.data.models.OrderStatus

@Dao
interface OrderDao {

    @Insert
    suspend fun insert(order: OrderEntity): Long

    @Update
    suspend fun update(order: OrderEntity)

    @Delete
    suspend fun delete(order: OrderEntity)

    @Query("DELETE FROM orders WHERE localId = :localId")
    suspend fun deleteById(localId: Long)

    @Query("SELECT * FROM orders WHERE localId = :localId")
    suspend fun findById(localId: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE serverId = :serverId")
    suspend fun findByServerId(serverId: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAt DESC")
    fun getByUserIdFlow(userId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getByUserId(userId: Long): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE status = 'LOCAL_PENDING' AND markedForDeletion = 0")
    suspend fun getPendingForSync(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE markedForDeletion = 1")
    suspend fun getMarkedForDeletion(): List<OrderEntity>

    @Query("SELECT COUNT(*) FROM orders WHERE userId = :userId AND status IN ('LOCAL_PENDING', 'SERVER_PENDING')")
    suspend fun countActiveByUser(userId: Long): Int

    @Query("SELECT COUNT(*) FROM orders WHERE userId = :userId")
    suspend fun countByUser(userId: Long): Int

    @Query("SELECT * FROM orders WHERE title LIKE '%' || :query || '%' AND userId = :userId ORDER BY createdAt DESC")
    suspend fun searchByTitle(query: String, userId: Long): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE status = 'LOCAL_PENDING' AND createdAt < :timestamp")
    suspend fun getOldPending(timestamp: Long): List<OrderEntity>

    @Query("UPDATE orders SET status = :status, lastUpdated = :timestamp WHERE localId = :localId")
    suspend fun updateStatus(localId: Long, status: OrderStatus, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM orders WHERE userId = :userId AND status != 'LOCAL_PENDING'")
    suspend fun deleteAllExceptLocalPendingByUserId(userId: Long)
}