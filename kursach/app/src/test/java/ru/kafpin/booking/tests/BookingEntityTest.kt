package ru.kafpin.booking.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.data.models.BookingEntity
import ru.kafpin.data.models.BookingStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class BookingEntityValidationTest {

    @Test
    fun создание_брони_с_валидным_количеством_должно_соответствовать_бизнес_правилам() {
        // Arrange
        val testCases = listOf(
            Triple(1, 10, true),   // 1 книга при 10 доступных - можно
            Triple(5, 10, true),   // 5 книг при 10 доступных - можно
            Triple(3, 3, true),    // все доступные - можно
            Triple(6, 10, false),  // больше 5 - нельзя по бизнес-правилу
            Triple(0, 10, false),  // 0 книг - нельзя
            Triple(2, 1, false),   // больше доступных - нельзя
            Triple(-1, 10, false)  // отрицательное - нельзя
        )

        testCases.forEach { (quantity, availableCopies, expectedValid) ->
            // Act
            val booking = createTestBooking(quantity = quantity, availableCopies = availableCopies)
            val isValid = isBookingQuantityValid(booking)

            // Assert
            assertEquals("quantity=$quantity, available=$availableCopies",
                expectedValid, isValid)
        }
    }

    @Test
    fun проверка_доступности_книги_для_бронирования() {
        // Arrange
        val testCases = listOf(
            Triple(5, 1, false),   // хочет 5, доступно 1 - нельзя
            Triple(3, 3, true),    // хочет 3, доступно 3 - можно
            Triple(1, 0, false),   // хочет 1, доступно 0 - нельзя
            Triple(2, 10, true),   // хочет 2, доступно 10 - можно
            Triple(0, 5, false)    // хочет 0, доступно 5 - нельзя
        )

        testCases.forEach { (wanted, available, expectedCanBook) ->
            // Act
            val canBook = canBookCopies(wanted, available)

            // Assert
            assertEquals("wanted=$wanted, available=$available",
                expectedCanBook, canBook)
        }
    }

    @Test
    fun валидация_дат_бронирования_должна_проверять_корректность_и_последовательность() {
        // Arrange
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        val testCases = listOf(
            // (dateIssue, dateReturn, expectedValid, description)
            Quad(
                today.plusDays(1).format(formatter),
                today.plusDays(15).format(formatter),
                true,
                "Будущие даты, возврат позже выдачи"
            ),
            Quad(
                today.format(formatter),
                today.minusDays(1).format(formatter),
                false,
                "Возврат раньше выдачи"
            ),
            Quad(
                today.minusDays(10).format(formatter),
                today.minusDays(5).format(formatter),
                false,
                "Прошедшие даты"
            ),
            Quad(
                today.plusDays(1).format(formatter),
                today.plusDays(1).format(formatter),
                false,
                "Одинаковые даты"
            ),
            Quad(
                "неправильная-дата",
                today.plusDays(15).format(formatter),
                false,
                "Невалидная дата выдачи"
            ),
            Quad(
                today.plusDays(1).format(formatter),
                "2024-13-45",
                false,
                "Невалидная дата возврата"
            )
        )

        testCases.forEach { (issueDate, returnDate, expectedValid, description) ->
            // Act
            val isValid = areBookingDatesValid(issueDate, returnDate)

            // Assert
            assertEquals("$description: issue=$issueDate, return=$returnDate",
                expectedValid, isValid)
        }
    }

    @Test
    fun проверка_максимального_срока_бронирования() {
        // Arrange
        val maxBookingDays = 30 // максимальный срок бронирования
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        val testCases = listOf(
            Pair(today, today.plusDays(30)) to true,   // ровно 30 дней
            Pair(today, today.plusDays(29)) to true,   // 29 дней
            Pair(today, today.plusDays(31)) to false,  // 31 день - слишком много
            Pair(today, today.plusDays(60)) to false,  // 60 дней - слишком много
            Pair(today, today.plusDays(1)) to true     // 1 день - можно
        )

        testCases.forEach { (dates, expectedValid) ->
            val (issueDate, returnDate) = dates

            // Act
            val daysBetween = ChronoUnit.DAYS.between(issueDate, returnDate).toInt()
            val isValid = daysBetween in 1..maxBookingDays

            // Assert
            assertEquals("Бронирование на $daysBetween дней", expectedValid, isValid)
        }
    }

    @Test
    fun проверка_просроченности_брони_с_учетом_статуса() {
        // Arrange
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        val testCases = listOf(
            // (status, dateReturn, expectedOverdue, description)
            Quad(
                BookingStatus.ISSUED,
                today.minusDays(1).format(formatter),
                true,
                "ISSUED и просрочен"
            ),
            Quad(
                BookingStatus.ISSUED,
                today.plusDays(5).format(formatter),
                false,
                "ISSUED и не просрочен"
            ),
            Quad(
                BookingStatus.PENDING,
                today.minusDays(1).format(formatter),
                false,
                "PENDING не может быть просроченным"
            ),
            Quad(
                BookingStatus.RETURNED,
                today.minusDays(10).format(formatter),
                false,
                "RETURNED уже возвращена"
            )
        )

        testCases.forEach { (status, dateReturn, expectedOverdue, description) ->
            // Act
            val booking = createTestBooking(status = status, dateReturn = dateReturn)
            val isOverdue = isBookingOverdue(booking)

            // Assert
            assertEquals("$description: status=$status, return=$dateReturn",
                expectedOverdue, isOverdue)
        }
    }

    @Test
    fun логика_синхронизации_брони_с_сервером() {
        // Arrange
        val originalBooking = createTestBooking(
            status = BookingStatus.PENDING,
            serverId = null,
            lastUpdated = 1000L
        )

        // Act (симуляция успешной синхронизации)
        val syncedBooking = originalBooking.copy(
            status = BookingStatus.CONFIRMED,
            serverId = 500L,
            lastUpdated = 2000L
        )

        // Assert
        assertNotNull("После синхронизации должен быть serverId", syncedBooking.serverId)
        assertEquals("Статус должен обновиться", BookingStatus.CONFIRMED, syncedBooking.status)
        assertTrue("Timestamp должен увеличиться", syncedBooking.lastUpdated > originalBooking.lastUpdated)
        assertEquals("Локальные данные не должны меняться",
            originalBooking.localId, syncedBooking.localId)
        assertEquals("Данные книги не должны меняться",
            originalBooking.bookId, syncedBooking.bookId)
    }

    @Test
    fun проверка_что_дата_возврата_не_раньше_даты_выдачи() {
        // Arrange
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val today = LocalDate.now()

        val validBookings = listOf(
            createTestBooking(
                dateIssue = today.format(formatter),
                dateReturn = today.plusDays(14).format(formatter)
            ),
            createTestBooking(
                dateIssue = today.plusDays(1).format(formatter),
                dateReturn = today.plusDays(15).format(formatter)
            )
        )

        val invalidBooking = createTestBooking(
            dateIssue = today.format(formatter),
            dateReturn = today.minusDays(1).format(formatter) // возврат раньше выдачи!
        )

        // Act & Assert
        validBookings.forEach { booking ->
            assertTrue("Возврат должен быть позже выдачи",
                isReturnAfterIssue(booking))
        }

        assertFalse("Возврат не может быть раньше выдачи",
            isReturnAfterIssue(invalidBooking))
    }

    // Вспомогательные методы
    private fun createTestBooking(
        bookId: Long = 1L,
        bookTitle: String = "Тестовая книга",
        bookAuthors: String = "Тестовый автор",
        bookGenres: String = "Тестовый жанр",
        availableCopies: Int = 5,
        userId: Long = 1L,
        quantity: Int = 1,
        dateIssue: String = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
        dateReturn: String = LocalDate.now().plusDays(15).format(DateTimeFormatter.ISO_LOCAL_DATE),
        status: BookingStatus = BookingStatus.PENDING,
        lastUpdated: Long = System.currentTimeMillis(),
        serverId: Long? = null
    ): BookingEntity {
        return BookingEntity(
            bookId = bookId,
            bookTitle = bookTitle,
            bookAuthors = bookAuthors,
            bookGenres = bookGenres,
            availableCopies = availableCopies,
            userId = userId,
            quantity = quantity,
            dateIssue = dateIssue,
            dateReturn = dateReturn,
            status = status,
            lastUpdated = lastUpdated,
            serverId = serverId
        )
    }

    private fun isBookingQuantityValid(booking: BookingEntity): Boolean {
        return booking.quantity in 1..5 &&
                booking.quantity <= booking.availableCopies
    }

    private fun canBookCopies(wanted: Int, available: Int): Boolean {
        return wanted in 1..5 && wanted <= available
    }

    private fun areBookingDatesValid(issueDate: String, returnDate: String): Boolean {
        return try {
            val issue = LocalDate.parse(issueDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val returnDate = LocalDate.parse(returnDate, DateTimeFormatter.ISO_LOCAL_DATE)
            issue.isBefore(returnDate) && issue.isAfter(LocalDate.now().minusDays(1))
        } catch (e: Exception) {
            false
        }
    }

    private fun isBookingOverdue(booking: BookingEntity): Boolean {
        if (booking.status != BookingStatus.ISSUED) return false

        return try {
            val returnDate = LocalDate.parse(booking.dateReturn, DateTimeFormatter.ISO_LOCAL_DATE)
            returnDate.isBefore(LocalDate.now())
        } catch (e: Exception) {
            false
        }
    }

    private fun isReturnAfterIssue(booking: BookingEntity): Boolean {
        return try {
            val issue = LocalDate.parse(booking.dateIssue, DateTimeFormatter.ISO_LOCAL_DATE)
            val returnDate = LocalDate.parse(booking.dateReturn, DateTimeFormatter.ISO_LOCAL_DATE)
            returnDate.isAfter(issue)
        } catch (e: Exception) {
            false
        }
    }

    // Вспомогательный класс для Quad
    private data class Quad<T1, T2, T3, T4>(
        val first: T1,
        val second: T2,
        val third: T3,
        val fourth: T4
    )
}