package ru.kafpin.data.models

import ru.kafpin.api.models.ReaderProfileResponse
import ru.kafpin.data.models.ProfileEntity

data class ProfileWithDetails(
    val profile: ProfileEntity,
    val isOnline: Boolean = false
) {

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
}