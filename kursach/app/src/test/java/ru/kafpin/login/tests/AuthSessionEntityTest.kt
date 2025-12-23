package ru.kafpin.login.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.data.models.AuthSessionEntity
import java.time.Instant
import java.time.temporal.ChronoUnit

class AuthSessionEntityTest {

    @Test
    fun создание_сессии_с_будущими_сроками_действие_должно_быть_валидным() {
        // Arrange
        val currentTime = System.currentTimeMillis()
        val accessExpiresAt = currentTime + 15 * 60 * 1000L // +15 минут
        val refreshExpiresAt = currentTime + 24 * 60 * 60 * 1000L // +24 часа

        // Act
        val session = AuthSessionEntity(
            userId = 1L,
            accessToken = "access_token_abc123",
            refreshToken = "refresh_token_xyz789",
            accessExpiresAt = accessExpiresAt,
            refreshExpiresAt = refreshExpiresAt
        )

        // Assert
        assertTrue("Access token должен быть в будущем",
            session.accessExpiresAt > currentTime)
        assertTrue("Refresh token должен быть в будущем",
            session.refreshExpiresAt > currentTime)
        assertTrue("Refresh token должен действовать дольше access token",
            session.refreshExpiresAt > session.accessExpiresAt)
    }

    @Test
    fun проверка_можно_ли_обновить_токен_по_времени() {
        // Arrange
        val currentTime = System.currentTimeMillis()

        val sessions = listOf(
            // (accessExpires, refreshExpires, canRefresh)
            AuthSessionEntity(
                userId = 1L,
                accessToken = "token1",
                refreshToken = "refresh1",
                accessExpiresAt = currentTime - 1000, // истёк
                refreshExpiresAt = currentTime + 600000 // живёт ещё 10 минут
            ) to true,

            AuthSessionEntity(
                userId = 2L,
                accessToken = "token2",
                refreshToken = "refresh2",
                accessExpiresAt = currentTime + 30000, // живёт 30 секунд
                refreshExpiresAt = currentTime - 1000 // истёк
            ) to false,

            AuthSessionEntity(
                userId = 3L,
                accessToken = "token3",
                refreshToken = "refresh3",
                accessExpiresAt = currentTime - 5000, // истёк 5 секунд назад
                refreshExpiresAt = currentTime - 1000 // истёк 1 секунду назад
            ) to false
        )

        sessions.forEach { (session, expectedCanRefresh) ->
            // Act
            val canRefresh = canRefreshToken(session, currentTime)

            // Assert
            assertEquals("Session userId=${session.userId} canRefresh=$expectedCanRefresh",
                expectedCanRefresh, canRefresh)
        }
    }

    @Test
    fun обновление_сессии_должно_сохранять_userId_и_увеличивать_сроки() {
        // Arrange
        val originalSession = AuthSessionEntity(
            userId = 100L,
            accessToken = "old_access_123",
            refreshToken = "old_refresh_456",
            accessExpiresAt = 1000L,
            refreshExpiresAt = 2000L,
            createdAt = 500L
        )

        // Act (симуляция обновления токенов)
        val updatedSession = originalSession.copy(
            accessToken = "new_access_789",
            refreshToken = "new_refresh_012",
            accessExpiresAt = 3000L,
            refreshExpiresAt = 4000L
        )

        // Assert
        assertEquals("UserId должен остаться прежним",
            originalSession.userId, updatedSession.userId)
        assertEquals("CreatedAt должен остаться прежним",
            originalSession.createdAt, updatedSession.createdAt)
        assertNotEquals("Access token должен измениться",
            originalSession.accessToken, updatedSession.accessToken)
        assertNotEquals("Refresh token должен измениться",
            originalSession.refreshToken, updatedSession.refreshToken)
        assertTrue("Новый access expires должен быть больше",
            updatedSession.accessExpiresAt > originalSession.accessExpiresAt)
        assertTrue("Новый refresh expires должен быть больше",
            updatedSession.refreshExpiresAt > originalSession.refreshExpiresAt)
    }

    @Test
    fun проверка_что_access_token_истекает_раньше_refresh_token() {
        // Arrange
        val currentTime = System.currentTimeMillis()

        val validSessions = listOf(
            AuthSessionEntity(
                userId = 1L, accessToken = "t1", refreshToken = "r1",
                accessExpiresAt = currentTime + 900000, // 15 минут
                refreshExpiresAt = currentTime + 86400000 // 24 часа
            ),
            AuthSessionEntity(
                userId = 2L, accessToken = "t2", refreshToken = "r2",
                accessExpiresAt = currentTime + 300000, // 5 минут
                refreshExpiresAt = currentTime + 7200000 // 2 часа
            )
        )

        val invalidSession = AuthSessionEntity(
            userId = 3L, accessToken = "t3", refreshToken = "r3",
            accessExpiresAt = currentTime + 86400000, // 24 часа
            refreshExpiresAt = currentTime + 900000 // 15 минут - НЕПРАВИЛЬНО!
        )

        // Act & Assert для валидных
        validSessions.forEach { session ->
            assertTrue("Access должен истекать раньше refresh",
                session.accessExpiresAt < session.refreshExpiresAt)
        }

        // Act & Assert для невалидного
        assertFalse("Access не должен истекать позже refresh",
            invalidSession.accessExpiresAt < invalidSession.refreshExpiresAt)
    }

    @Test
    fun форматирование_времени_жизни_токенов_для_логирования() {
        // Arrange
        val currentTime = System.currentTimeMillis()
        val session = AuthSessionEntity(
            userId = 1L,
            accessToken = "token",
            refreshToken = "refresh",
            accessExpiresAt = currentTime + 900000, // 15 минут
            refreshExpiresAt = currentTime + 86400000 // 24 часа
        )

        // Act
        val accessSecondsLeft = (session.accessExpiresAt - currentTime) / 1000
        val refreshSecondsLeft = (session.refreshExpiresAt - currentTime) / 1000

        // Assert
        assertTrue("Access должен жить 15 минут (±1 минута)",
            accessSecondsLeft in (14 * 60)..(16 * 60))
        assertTrue("Refresh должен жить 24 часа (±1 час)",
            refreshSecondsLeft in (23 * 60 * 60)..(25 * 60 * 60))

        // Форматирование для логов
        val accessFormatted = formatTimeLeft(accessSecondsLeft)
        val refreshFormatted = formatTimeLeft(refreshSecondsLeft)

        assertEquals("15 мин", accessFormatted)
        assertEquals("24 ч", refreshFormatted)
    }

    // Вспомогательные методы
    private fun canRefreshToken(session: AuthSessionEntity, currentTime: Long): Boolean {
        return session.refreshExpiresAt > currentTime
    }

    private fun formatTimeLeft(seconds: Long): String {
        return when {
            seconds < 60 -> "$seconds сек"
            seconds < 3600 -> "${seconds / 60} мин"
            else -> "${seconds / 3600} ч"
        }
    }
}