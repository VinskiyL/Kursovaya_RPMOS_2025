package ru.kafpin.data.models

import ru.kafpin.api.models.ReaderProfileResponse
import ru.kafpin.data.models.ProfileEntity

data class ProfileWithDetails(
    val profile: ProfileEntity,
    val isOnline: Boolean = false
) {

    // Вычисляемые свойства
    val fullName: String
        get() = buildString {
            append(profile.surname)
            append(" ")
            append(profile.name)
            profile.patronymic?.let { append(" $it") }
        }

    val address: String
        get() = buildString {
            append("г. ${profile.city}, ")
            append("ул. ${profile.street}, ")
            append("д. ${profile.house}")
            profile.buildingHouse?.let { append("/$it") }
            profile.flat?.let { append(", кв. $it") }
        }

    val passportInfo: String
        get() = "${profile.passportSeries} ${profile.passportNumber}, " +
                "выдан: ${profile.issuedByWhom}, " +
                "дата выдачи: ${profile.dateIssue}"

    // Для логирования (по аналогии с другими репозиториями)
    val displayInfo: String
        get() = "$fullName (${profile.login})"

    fun canEdit(isOnline: Boolean): Boolean = isOnline

    companion object {
        fun fromResponse(response: ReaderProfileResponse, userId: Long): ProfileWithDetails {
            val entity = response.toProfileEntity(userId)
            return ProfileWithDetails(profile = entity)
        }
    }
}