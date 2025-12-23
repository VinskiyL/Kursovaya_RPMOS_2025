package ru.kafpin.registration.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.api.models.RegistrationRequest

class RegistrationRequestTest {

    @Test
    fun создание_объекта_со_всеми_полями() {
        val запрос = RegistrationRequest(
            surname = "Иванов",
            name = "Иван",
            patronymic = "Иванович",
            birthday = "1990-01-01",
            education = "Высшее",
            profession = "Программист",
            educationalInst = "МГУ",
            city = "Москва",
            street = "Ленина",
            house = 10,
            buildingHouse = "1А",
            flat = 25,
            passportSeries = 1234,
            passportNumber = 567890,
            issuedByWhom = "ОУФМС",
            dateIssue = "2010-05-15",
            phone = "+79161234567",
            login = "ivanov",
            password = "password123",
            confirmPassword = "password123",
            mail = "ivanov@mail.ru"
        )

        // Проверка
        assertEquals("Иванов", запрос.surname)
        assertEquals("Иван", запрос.name)
        assertEquals("Иванович", запрос.patronymic)
        assertEquals("1990-01-01", запрос.birthday)
        assertEquals("Высшее", запрос.education)
        assertEquals(1234, запрос.passportSeries)
        assertEquals(567890, запрос.passportNumber)
        assertEquals("+79161234567", запрос.phone)
        assertEquals("ivanov", запрос.login)
        assertEquals("ivanov@mail.ru", запрос.mail)
    }

    @Test
    fun создание_объекта_без_необязательных_полей() {
        val запрос = RegistrationRequest(
            surname = "Петров",
            name = "Петр",
            patronymic = null,
            birthday = "1995-02-20",
            education = "Среднее",
            profession = null,
            educationalInst = null,
            city = "Санкт-Петербург",
            street = "Невский",
            house = 15,
            buildingHouse = null,
            flat = null,
            passportSeries = 5678,
            passportNumber = 123456,
            issuedByWhom = "ГУ МВД",
            dateIssue = "2015-06-10",
            phone = "+79211234567",
            login = "petrov",
            password = "qwerty",
            confirmPassword = "qwerty",
            mail = "petrov@mail.ru"
        )

        assertEquals("Петров", запрос.surname)
        assertNull(запрос.patronymic) // patronymic должен быть null
        assertNull(запрос.profession) // profession должен быть null
        assertEquals("Среднее", запрос.education)
    }
}