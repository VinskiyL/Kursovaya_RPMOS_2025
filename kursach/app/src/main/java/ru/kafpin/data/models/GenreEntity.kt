package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val lastSynced: Long = System.currentTimeMillis()
)