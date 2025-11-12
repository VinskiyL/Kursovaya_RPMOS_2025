package ru.kafpin.data.models

import androidx.room.Entity

@Entity(
    tableName = "book_authors",
    primaryKeys = ["bookId", "authorId"]
)
data class BookAuthorCrossRef(
    val bookId: Long,
    val authorId: Long
)