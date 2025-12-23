package ru.kafpin.login.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.data.models.UserEntity

class UserEntityTest {

    @Test
    fun проверка_синхронизации_пользователя() {
        // Arrange
        val user = UserEntity(
            id = 1L,
            login = "user123",
            displayName = "Тестовый",
            isActive = true,
            isAdmin = false,
            syncedAt = 1000L
        )

        // Act (симуляция успешной синхронизации)
        val updatedUser = user.copy(
            displayName = "Обновлённый",
            syncedAt = 2000L
        )

        // Assert
        assertEquals("ID не должен меняться", user.id, updatedUser.id)
        assertEquals("Логин не должен меняться", user.login, updatedUser.login)
        assertNotEquals("DisplayName может измениться",
            user.displayName, updatedUser.displayName)
        assertTrue("Время синхронизации должно увеличиться",
            updatedUser.syncedAt > user.syncedAt)
    }
}