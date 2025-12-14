package ru.kafpin.data.mappers

import ru.kafpin.api.models.AuthResponse
import ru.kafpin.data.models.AuthSessionEntity

fun AuthResponse.toAuthSessionEntity(userId: Long): AuthSessionEntity {
    val currentTime = System.currentTimeMillis()
    return AuthSessionEntity(
        userId = userId,
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessExpiresAt = currentTime + 15 * 60 * 1000L,      // 15 минут
        refreshExpiresAt = currentTime + 24 * 60 * 60 * 1000L // 1 день
    )
}