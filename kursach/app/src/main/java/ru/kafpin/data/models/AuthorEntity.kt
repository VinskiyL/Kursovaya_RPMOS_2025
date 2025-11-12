package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authors")
data class AuthorEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val lastSynced: Long = System.currentTimeMillis() // для синхронизации
)