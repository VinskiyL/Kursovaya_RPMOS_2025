package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: Long,
    val index: String,
    val authorsMark: String, // временно, пока не сделаем связи
    val title: String,
    val placePublication: String,
    val informationPublication: String,
    val volume: Int,
    val quantityTotal: Int,
    val quantityRemaining: Int,
    val cover: String?,
    val datePublication: String,
    val lastSynced: Long = System.currentTimeMillis()
)