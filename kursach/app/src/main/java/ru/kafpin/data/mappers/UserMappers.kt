package ru.kafpin.data.mappers

import ru.kafpin.api.models.UserResponse
import ru.kafpin.data.models.UserEntity


fun UserResponse.toUserEntity(): UserEntity {
    return UserEntity(
        id = id,
        login = login,
        displayName = displayName,
        isActive = isActive,
        isAdmin = isAdmin,
        syncedAt = System.currentTimeMillis()
    )
}

fun UserEntity.toUserResponse(): UserResponse {
    return UserResponse(
        id = id,
        login = login,
        displayName = displayName,
        isActive = isActive,
        isAdmin = isAdmin
    )
}
