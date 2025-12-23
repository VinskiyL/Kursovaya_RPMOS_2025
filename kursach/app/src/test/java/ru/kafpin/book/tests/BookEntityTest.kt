package ru.kafpin.book.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.data.models.BookEntity
import java.util.regex.Pattern

class BookEntityTest {
    @Test
    fun поиск_книги_по_разным_критериям() {
        // Arrange
        val books = listOf(
            createTestBook(
                id = 1L,
                title = "Программирование на Kotlin",
                authorsMark = "Иванов",
                datePublication = "2023"
            ),
            createTestBook(
                id = 2L,
                title = "Android Development Guide",
                authorsMark = "Петров",
                datePublication = "2022"
            ),
            createTestBook(
                id = 3L,
                title = "Kotlin для начинающих",
                authorsMark = "Сидоров",
                datePublication = "2024"
            ),
            createTestBook(
                id = 4L,
                title = "Java Programming",
                authorsMark = "Иванов",
                datePublication = "2021"
            )
        )

        // Act
        val byTitle = searchBooks(books, "kotlin", SearchField.TITLE)
        val byAuthor = searchBooks(books, "иванов", SearchField.AUTHOR)
        val byYear = books.filter { it.datePublication == "2023" }

        // Assert
        assertEquals("По заголовку 'kotlin' должно найти 2 книги", 2, byTitle.size)
        assertEquals("По автору 'иванов' должно найти 2 книги", 2, byAuthor.size)
        assertEquals("За 2023 год должно найти 1 книгу", 1, byYear.size)

        assertTrue("Должна найти 'Программирование на Kotlin'",
            byTitle.any { it.title.contains("Kotlin") })
        assertTrue("Должна найти книги Иванова",
            byAuthor.all { it.authorsMark == "Иванов" })
    }

    @Test
    fun проверка_формата_года_издания() {
        // Arrange
        val validYears = listOf("2023", "2000", "1999", "2024", "1900", "2100")
        val invalidYears = listOf("", "   ", "202", "20", "год", "abcd", "20234", "18 век")

        // Act & Assert для валидных
        validYears.forEach { year ->
            assertTrue("Год '$year' должен быть валидным", isValidPublicationYear(year))
            val book = createTestBook(datePublication = year)
            assertEquals("Год должен сохраняться", year, book.datePublication)
        }

        // Act & Assert для невалидных
        invalidYears.forEach { year ->
            assertFalse("Год '$year' должен быть невалидным", isValidPublicationYear(year))
        }
    }

    @Test
    fun проверка_обложки_книги_для_UI() {
        // Arrange
        val bookWithCover = createTestBook(
            cover = "cover.jpg",
            title = "Книга с обложкой"
        )

        val bookWithoutCover = createTestBook(
            cover = null,
            title = "Книга без обложки"
        )

        val bookEmptyCover = createTestBook(
            cover = "",
            title = "Книга с пустой обложкой"
        )

        // Act
        val hasCover1 = hasBookCover(bookWithCover)
        val hasCover2 = hasBookCover(bookWithoutCover)
        val hasCover3 = hasBookCover(bookEmptyCover)

        val defaultCover = getDefaultCoverUrl()

        // Assert
        assertTrue("Книга с обложкой должна её иметь", hasCover1)
        assertFalse("Книга без обложки не должна её иметь", hasCover2)
        assertFalse("Книга с пустой обложкой не должна её иметь", hasCover3)

        assertEquals("Обложка должна быть 'cover.jpg'", "cover.jpg", bookWithCover.cover)
        assertNull("Обложка должна быть null", bookWithoutCover.cover)

        // Проверка логики отображения
        val displayCover1 = bookWithCover.cover ?: defaultCover
        val displayCover2 = bookWithoutCover.cover ?: defaultCover

        assertEquals("cover.jpg", displayCover1)
        assertEquals(defaultCover, displayCover2)
    }

    // Вспомогательные методы
    private fun createTestBook(
        id: Long = 1L,
        index: String = "ISBN123",
        authorsMark: String = "Автор",
        title: String = "Тестовая книга",
        placePublication: String = "Москва",
        informationPublication: String = "1-е издание",
        volume: Int = 200,
        quantityTotal: Int = 5,
        quantityRemaining: Int = 3,
        cover: String? = null,
        datePublication: String = "2023",
        lastSynced: Long = System.currentTimeMillis()
    ): BookEntity {
        return BookEntity(
            id = id,
            index = index,
            authorsMark = authorsMark,
            title = title,
            placePublication = placePublication,
            informationPublication = informationPublication,
            volume = volume,
            quantityTotal = quantityTotal,
            quantityRemaining = quantityRemaining,
            cover = cover,
            datePublication = datePublication,
            lastSynced = lastSynced
        )
    }

    private fun isValidPublicationYear(year: String): Boolean {
        return year.matches(Regex("\\d{4}"))
    }

    private fun hasBookCover(book: BookEntity): Boolean {
        return !book.cover.isNullOrBlank()
    }

    private fun getDefaultCoverUrl(): String {
        return "default_cover.jpg"
    }

    private fun searchBooks(books: List<BookEntity>, query: String, field: SearchField): List<BookEntity> {
        return books.filter { book ->
            when (field) {
                SearchField.TITLE -> book.title.contains(query, ignoreCase = true)
                SearchField.AUTHOR -> book.authorsMark.contains(query, ignoreCase = true)
                SearchField.YEAR -> book.datePublication == query
            }
        }
    }

    enum class SearchField { TITLE, AUTHOR, YEAR }
}