package ru.kafpin.book.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.data.models.BookEntity
import ru.kafpin.data.models.AuthorEntity
import ru.kafpin.data.models.GenreEntity
import ru.kafpin.data.models.BookWithDetails

class BookWithDetailsTest {

    @Test
    fun проверка_доступности_книги_в_BookWithDetails() {
        val bookAvailable = BookEntity(
            id = 1L, index = "1", authorsMark = "Автор", title = "Доступная книга",
            placePublication = "", informationPublication = "", volume = 0,
            quantityTotal = 5, quantityRemaining = 3, cover = null, datePublication = "2023"
        )

        val bookUnavailable = bookAvailable.copy(
            id = 2L, title = "Недоступная книга", quantityRemaining = 0
        )

        val authors = listOf(AuthorEntity(id=1, surname="Автор", name="Имя", patronymic=null))
        val genres = listOf(GenreEntity(id=1, name="Жанр"))

        val availableDetails = BookWithDetails(bookAvailable, authors, genres)
        val unavailableDetails = BookWithDetails(bookUnavailable, authors, genres)

        assertTrue("Книга должна быть доступной",
            availableDetails.book.quantityRemaining > 0)
        assertFalse("Книга должна быть недоступной",
            unavailableDetails.book.quantityRemaining > 0)

        // В UI обычно показывается статус
        val availableStatus = if (availableDetails.book.quantityRemaining > 0)
            "✅ В наличии" else "❌ Нет в наличии"
        val unavailableStatus = if (unavailableDetails.book.quantityRemaining > 0)
            "✅ В наличии" else "❌ Нет в наличии"

        assertEquals("✅ В наличии", availableStatus)
        assertEquals("❌ Нет в наличии", unavailableStatus)
    }

    @Test
    fun сравнение_BookWithDetails_по_id_книги() {
        val book1 = BookEntity(id=1L, index="1", authorsMark="А", title="Книга 1",
            placePublication="", informationPublication="", volume=0,
            quantityTotal=1, quantityRemaining=1, cover=null, datePublication="2023")
        val book2 = BookEntity(id=2L, index="2", authorsMark="Б", title="Книга 2",
            placePublication="", informationPublication="", volume=0,
            quantityTotal=1, quantityRemaining=1, cover=null, datePublication="2023")

        val authors = emptyList<AuthorEntity>()
        val genres = emptyList<GenreEntity>()

        val details1 = BookWithDetails(book1, authors, genres)
        val details2 = BookWithDetails(book2, authors, genres)
        val details3 = BookWithDetails(book1, listOf(AuthorEntity(id=3, surname="Другой", name="Автор", patronymic=null)), genres)

        assertNotEquals("Разные ID книг → разные объекты",
            details1.book.id, details2.book.id)
        assertEquals("Одинаковые ID книг → одинаковые ключи для DiffUtil",
            details1.book.id, details3.book.id)
    }
}