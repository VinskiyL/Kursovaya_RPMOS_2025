package ru.kafpin.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val userId: Long,
    val surname: String,
    val name: String,
    val patronymic: String?,
    val birthday: String,
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
    val dateIssue: String,
    val consistsOf: String,
    val reRegistration: String?,
    val phone: String,
    val login: String,
    val mail: String,
    val admin: Boolean,
    val isActive: Boolean,

    val lastSyncedAt: Long = System.currentTimeMillis()
)