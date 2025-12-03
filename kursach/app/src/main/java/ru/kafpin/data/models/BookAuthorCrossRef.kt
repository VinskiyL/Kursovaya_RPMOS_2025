package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookauthorcrossref")
data class BookAuthorCrossRef(
    @PrimaryKey
    val id: Long,
    val bookId: Long,
    val authorId: Long,
    val lastSynced: Long = System.currentTimeMillis()
)