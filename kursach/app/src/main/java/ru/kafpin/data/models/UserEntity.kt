package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Long,

    val login: String,
    val displayName: String?,
    val isActive: Boolean,
    val isAdmin: Boolean,
    val syncedAt: Long = 0
)