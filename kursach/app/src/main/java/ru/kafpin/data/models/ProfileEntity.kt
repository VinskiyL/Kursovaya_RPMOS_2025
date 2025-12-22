package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,

    // Связь с UserEntity (как в бронированиях userId)
    val userId: Long,

    // Все поля как String (по аналогии с dateIssue: String в бронированиях)
    val surname: String,
    val name: String,
    val patronymic: String?,
    val birthday: String,           // String как dateIssue в BookingEntity
    val education: String,
    val profession: String?,
    val educationalInst: String?,
    val city: String,
    val street: String,
    val house: Int,
    val buildingHouse: String?,
    val flat: Int?,
    val passportSeries: Int,
    val passportNumber: Int,
    val issuedByWhom: String,
    val dateIssue: String,          // String как в бронированиях
    val consistsOf: String,         // String
    val reRegistration: String?,    // String
    val phone: String,
    val login: String,
    val mail: String,
    val admin: Boolean,
    val isActive: Boolean,

    // Для синхронизации (по аналогии с lastUpdated в BookingEntity)
    val lastSyncedAt: Long = System.currentTimeMillis()
)