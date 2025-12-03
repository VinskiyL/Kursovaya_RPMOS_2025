package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookgenrecrossref")
data class BookGenreCrossRef(
    @PrimaryKey
    val id: Long,
    val bookId: Long,
    val genreId: Long,
    val lastSynced: Long = System.currentTimeMillis()
)