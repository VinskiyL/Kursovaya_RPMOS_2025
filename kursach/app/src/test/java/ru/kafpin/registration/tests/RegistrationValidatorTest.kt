package ru.kafpin.registration.tests

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

class RegistrationValidationTest {

    // Выносим логику валидации из Activity в тестируемый класс
    class RegistrationValidator {

        companion object {
            fun isValidSurname(surname: String): Boolean {
                return surname.trim().isNotEmpty()
            }

            fun isValidName(name: String): Boolean {
                return name.trim().isNotEmpty()
            }

            fun isValidBirthday(birthday: String): Boolean {
                if (birthday.trim().isEmpty()) return false
                return try {
                    // Проверка формата yyyy-MM-dd (как в DatePicker)
                    LocalDate.parse(birthday, DateTimeFormatter.ISO_DATE)
                    true
                } catch (e: Exception) {
                    false
                }
            }

            fun isValidHouse(house: String): Boolean {
                val houseInt = house.trim().toIntOrNull()
                return houseInt != null && houseInt in 1..1000
            }

            fun isValidPassportSeries(series: String): Boolean {
                return series.length == 4 && series.all { it.isDigit() }
            }

            fun isValidPassportNumber(number: String): Boolean {
                return number.length == 6 && number.all { it.isDigit() }
            }
            fun calculateAge(birthday: String): Int? {
                return try {
                    val birthDate = LocalDate.parse(birthday, DateTimeFormatter.ISO_DATE)
                    Period.between(birthDate, LocalDate.now()).years
                } catch (e: Exception) {
                    null
                }
            }

            fun isAdult(birthday: String): Boolean {
                val age = calculateAge(birthday)
                return age != null && age >= 18
            }
        }
    }

    @Test
    fun валидация_фамилии_и_имени_должна_проверять_непустоту() {
        // Arrange
        val validNames = listOf("Иванов", "Петров-Водкин", "Smith", "А")
        val invalidNames = listOf("", "   ", "\t\n")

        // Act & Assert для фамилии
        validNames.forEach { surname ->
            assertTrue("Фамилия '$surname' должна быть валидной",
                RegistrationValidator.isValidSurname(surname))
        }

        invalidNames.forEach { surname ->
            assertFalse("Пустая фамилия '$surname' должна быть невалидной",
                RegistrationValidator.isValidSurname(surname))
        }

        // Act & Assert для имени
        validNames.forEach { name ->
            assertTrue("Имя '$name' должно быть валидным",
                RegistrationValidator.isValidName(name))
        }

        invalidNames.forEach { name ->
            assertFalse("Пустое имя '$name' должно быть невалидным",
                RegistrationValidator.isValidName(name))
        }
    }

    @Test
    fun валидация_даты_рождения_должна_проверять_формат_и_возраст() {
        // Arrange
        val today = LocalDate.now()
        val validDates = listOf(
            today.minusYears(20).format(DateTimeFormatter.ISO_DATE), // 20 лет
            today.minusYears(30).format(DateTimeFormatter.ISO_DATE), // 30 лет
            "2000-01-01", // корректный формат
            "1999-12-31"
        )

        val invalidDates = listOf(
            "",                    // пусто
            "   ",                 // пробелы
            "01.01.2000",         // неправильный формат
            "2000-13-45",         // несуществующая дата
            "не-дата",            // текст
            "2000/01/01"          // другой формат
        )

        // Act & Assert для формата
        validDates.forEach { date ->
            assertTrue("Дата '$date' должна быть валидной",
                RegistrationValidator.isValidBirthday(date))
        }

        invalidDates.forEach { date ->
            assertFalse("Дата '$date' должна быть невалидной",
                RegistrationValidator.isValidBirthday(date))
        }

        // Act & Assert для возраста
        val childDate = today.minusYears(17).format(DateTimeFormatter.ISO_DATE)
        val adultDate = today.minusYears(18).format(DateTimeFormatter.ISO_DATE)

        assertFalse("17 лет - несовершеннолетний",
            RegistrationValidator.isAdult(childDate))
        assertTrue("18 лет - совершеннолетний",
            RegistrationValidator.isAdult(adultDate))
    }

    @Test
    fun валидация_паспортных_данных_должна_проверять_длину_и_цифры() {
        // Arrange
        val validSeries = listOf("1234", "0000", "9999", "0101")
        val invalidSeries = listOf("", "123", "12345", "12 34", "abcd", "12-34")

        val validNumbers = listOf("123456", "000000", "999999", "010101")
        val invalidNumbers = listOf("", "12345", "1234567", "123 456", "abcdef")

        // Act & Assert для серии
        validSeries.forEach { series ->
            assertTrue("Серия '$series' должна быть валидной",
                RegistrationValidator.isValidPassportSeries(series))
        }

        invalidSeries.forEach { series ->
            assertFalse("Серия '$series' должна быть невалидной",
                RegistrationValidator.isValidPassportSeries(series))
        }

        // Act & Assert для номера
        validNumbers.forEach { number ->
            assertTrue("Номер '$number' должен быть валидным",
                RegistrationValidator.isValidPassportNumber(number))
        }

        invalidNumbers.forEach { number ->
            assertFalse("Номер '$number' должен быть невалидным",
                RegistrationValidator.isValidPassportNumber(number))
        }
    }

}