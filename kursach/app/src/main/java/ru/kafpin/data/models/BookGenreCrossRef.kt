package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BookGenreCrossRef(
    @PrimaryKey
    val id: Long,
    val bookId: Long,
    val genreId: Long
)