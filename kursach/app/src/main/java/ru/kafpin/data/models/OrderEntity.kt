package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,

    val serverId: Long? = null,

    val title: String,
    val authorSurname: String,
    val authorName: String?,
    val authorPatronymic: String?,
    val quantity: Int,
    val datePublication: String?,
    val userId: Long,
    val status: OrderStatus,
    val markedForDeletion: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class OrderStatus {
    LOCAL_PENDING,
    SERVER_PENDING,
    CONFIRMED
}