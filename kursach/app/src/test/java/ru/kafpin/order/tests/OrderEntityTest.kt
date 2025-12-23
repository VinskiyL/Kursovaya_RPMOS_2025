package ru.kafpin.order.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.data.models.OrderEntity
import ru.kafpin.data.models.OrderStatus

class OrderEntityValidationTest {

    @Test
    fun создание_заказа_с_валидным_количеством_должно_быть_возможно() {
        // Arrange
        val validQuantities = listOf(1, 2, 3, 4, 5)

        validQuantities.forEach { quantity ->
            // Act
            val order = createTestOrder(quantity = quantity)

            // Assert
            assertTrue("Количество $quantity должно быть валидным",
                isQuantityValid(order.quantity))
        }
    }

    @Test
    fun создание_заказа_с_невалидным_количеством_не_должно_блокировать_конструктор() {
        // Arrange
        val invalidQuantities = listOf(0, -1, -5, 6, 10, 100)

        invalidQuantities.forEach { quantity ->
            // Act
            val order = createTestOrder(quantity = quantity)

            // Assert
            assertFalse("Количество $quantity должно быть невалидным",
                isQuantityValid(order.quantity))
            // Но объект всё равно создаётся - валидация где-то в другом месте
        }
    }

    @Test
    fun проверка_формата_года_публикации_для_отображения() {
        // Arrange
        val validYears = listOf("2023", "2000", "1999", "2024")
        val invalidYears = listOf("202", "20", "год", "abcd", "20234", "", "  ")

        // Act & Assert для валидных
        validYears.forEach { year ->
            assertTrue("Год '$year' должен быть валидным",
                isValidPublicationYear(year))
        }

        // Act & Assert для невалидных
        invalidYears.forEach { year ->
            assertFalse("Год '$year' должен быть невалидным",
                isValidPublicationYear(year))
        }
    }

    @Test
    fun год_публикации_может_быть_null_для_старых_книг() {
        // Arrange & Act
        val orderWithYear = createTestOrder(datePublication = "2023")
        val orderWithoutYear = createTestOrder(datePublication = null)

        // Assert
        assertNotNull("Может быть с годом", orderWithYear.datePublication)
        assertNull("Может быть без года", orderWithoutYear.datePublication)

        // Проверка логики отображения
        val displayYear = orderWithoutYear.datePublication ?: "Не указан"
        assertEquals("Не указан", displayYear)
    }

    @Test
    fun проверка_логики_синхронизации_заказа() {
        // Arrange
        val originalOrder = createTestOrder(
            title = "Оригинальное название",
            status = OrderStatus.LOCAL_PENDING,
            lastUpdated = 1000L,
            serverId = null
        )

        // Act (симуляция успешной синхронизации)
        val syncedOrder = originalOrder.copy(
            status = OrderStatus.CONFIRMED,
            serverId = 123L,
            lastUpdated = 2000L
        )

        // Assert
        assertNotEquals("Должен измениться статус",
            originalOrder.status, syncedOrder.status)
        assertNotNull("Должен появиться serverId", syncedOrder.serverId)
        assertTrue("Должен обновиться timestamp",
            syncedOrder.lastUpdated > originalOrder.lastUpdated)
        assertEquals("Локальный ID не должен меняться",
            originalOrder.localId, syncedOrder.localId)
    }

    // Вспомогательные методы
    private fun createTestOrder(
        title: String = "Тестовая книга",
        authorSurname: String = "Тестовый автор",
        authorName: String? = null,
        authorPatronymic: String? = null,
        quantity: Int = 1,
        datePublication: String? = "2023",
        userId: Long = 1L,
        status: OrderStatus = OrderStatus.LOCAL_PENDING,
        lastUpdated: Long = System.currentTimeMillis(),
        serverId: Long? = null
    ): OrderEntity {
        return OrderEntity(
            title = title,
            authorSurname = authorSurname,
            authorName = authorName,
            authorPatronymic = authorPatronymic,
            quantity = quantity,
            datePublication = datePublication,
            userId = userId,
            status = status,
            lastUpdated = lastUpdated,
            serverId = serverId
        )
    }

    private fun isQuantityValid(quantity: Int): Boolean {
        return quantity in 1..5
    }

    private fun isValidPublicationYear(year: String?): Boolean {
        return year != null && year.matches(Regex("\\d{4}"))
    }
}