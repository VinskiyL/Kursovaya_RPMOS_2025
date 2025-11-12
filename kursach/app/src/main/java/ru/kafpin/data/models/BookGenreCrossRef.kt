package ru.kafpin.data.models

import androidx.room.Entity

@Entity(
    tableName = "book_genres",
    primaryKeys = ["bookId", "genreId"]
)
data class BookGenreCrossRef(
    val bookId: Long,
    val genreId: Long
)