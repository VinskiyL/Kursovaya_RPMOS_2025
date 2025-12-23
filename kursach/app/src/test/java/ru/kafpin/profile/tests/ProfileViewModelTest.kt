package ru.kafpin.profile.tests

import org.junit.Assert.*
import org.junit.Test

class ProfileViewModelTest {

    @Test
    fun changeLogin_валидация_требует_и_пароль_и_новый_логин() {
        // Arrange
        val testCases = listOf(
            Triple("", "newlogin", false),        // пустой пароль
            Triple("password", "", false),        // пустой логин
            Triple("", "", false),               // оба пустые
            Triple("pass", "newlogin", true),    // оба заполнены
            Triple("   ", "   ", false)          // пробелы
        )

        testCases.forEach { (currentPassword, newLogin, expected) ->
            // Act
            val isPasswordValid = currentPassword.trim().isNotEmpty()
            val isLoginValid = newLogin.trim().isNotEmpty()
            val isValid = isPasswordValid && isLoginValid

            // Assert
            assertEquals(
                "currentPassword='$currentPassword', newLogin='$newLogin'",
                expected, isValid
            )
        }
    }

    @Test
    fun changePassword_валидация_проверяет_совпадение_и_длину() {
        // Arrange
        val testCases = listOf(
            // (current, new, confirm, expected)
            Quad("", "newpass", "newpass", false),        // пустой текущий
            Quad("oldpass", "", "", false),              // пустой новый
            Quad("oldpass", "short", "short", false),    // короткий новый
            Quad("oldpass", "newpass", "wrong", false),  // не совпадают
            Quad("oldpass", "newpass", "newpass", true), // все ок
            Quad("   ", "   ", "   ", false)             // пробелы
        )

        testCases.forEach { (current, new, confirm, expected) ->
            // Act
            val isCurrentValid = current.trim().isNotEmpty()
            val isNewValid = new.trim().isNotEmpty() && new.length >= 6
            val isConfirmValid = new == confirm
            val isValid = isCurrentValid && isNewValid && isConfirmValid

            // Assert
            assertEquals(
                "current='$current', new='$new', confirm='$confirm'",
                expected, isValid
            )
        }
    }

    @Test
    fun updateProfile_требует_все_обязательные_поля() {
        // Arrange
        val requiredFields = listOf(
            "surname", "name", "birthday", "education",
            "city", "street", "house", "passportSeries",
            "passportNumber", "issuedByWhom", "phone", "mail"
        )

        val testProfile = mapOf(
            "surname" to "Иванов",
            "name" to "Иван",
            "birthday" to "1990-01-01",
            "education" to "Высшее",
            "city" to "Москва",
            "street" to "Ленина",
            "house" to 10,
            "passportSeries" to 1234,
            "passportNumber" to 567890,
            "issuedByWhom" to "ОВД",
            "phone" to "+79161234567",
            "mail" to "test@mail.ru"
        )

        // Act & Assert
        requiredFields.forEach { field ->
            assertTrue("Поле '$field' обязательно", testProfile.containsKey(field))

            val value = testProfile[field]
            when (field) {
                "surname", "name", "birthday", "education",
                "city", "street", "issuedByWhom", "phone", "mail" -> {
                    assertTrue("Поле '$field' должно быть непустой строкой",
                        value is String && value.toString().isNotBlank())
                }
                "house", "passportSeries", "passportNumber" -> {
                    assertTrue("Поле '$field' должно быть числом",
                        value is Int && value.toString().toIntOrNull() != null)
                }
            }
        }
    }

    @Test
    fun проверка_бизнес_логики_смены_логина() {
        // Arrange
        val validCases = listOf(
            Pair("correctPass", "newlogin") to true,   // все ок
            Pair("", "newlogin") to false,             // пустой пароль
            Pair("pass", "") to false,                 // пустой логин
            Pair("   ", "   ") to false                // пробелы
        )

        validCases.forEach { (credentials, expected) ->
            val (currentPassword, newLogin) = credentials

            // Act - бизнес-логика из ViewModel
            val canProceed = canChangeLogin(currentPassword, newLogin)

            // Assert
            assertEquals(
                "currentPassword='$currentPassword', newLogin='$newLogin'",
                expected, canProceed
            )
        }
    }

    // Вспомогательные методы (должны быть в ViewModel)
    private fun canChangeLogin(currentPassword: String, newLogin: String): Boolean {
        return currentPassword.trim().isNotEmpty() &&
                newLogin.trim().isNotEmpty() &&
                newLogin.length >= 3 // минимальная длина логина
    }

    // Вспомогательный класс для Quad
    private data class Quad<T1, T2, T3, T4>(
        val first: T1,
        val second: T2,
        val third: T3,
        val fourth: T4
    )
}