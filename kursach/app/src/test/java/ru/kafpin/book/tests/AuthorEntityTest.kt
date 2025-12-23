package ru.kafpin.book.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.data.models.AuthorEntity

class AuthorEntityTest {

    @Test
    fun форматирование_полного_имени_автора_для_UI() {
        // Arrange
        val testCases = listOf(
            Triple("Толстой", "Лев", "Николаевич") to "Толстой Лев Николаевич",
            Triple("Пушкин", "Александр", "Сергеевич") to "Пушкин Александр Сергеевич",
            Triple("Иванов", "Иван", null) to "Иванов Иван",
            Triple("Сидоров", null, null) to "Сидоров",
            Triple("Smith", "John", "Doe") to "Smith John Doe",
            Triple("ван Гог", "Винсент", null) to "ван Гог Винсент",
            Triple("О'Коннор", "Фланнери", null) to "О'Коннор Фланнери"
        )

        testCases.forEach { (data, expected) ->
            val (surname, name, patronymic) = data

            // Act
            val author = AuthorEntity(id = 1L, surname = surname, name = name, patronymic = patronymic)
            val fullName = formatAuthorName(author)

            // Assert
            assertEquals("Форматирование для '$surname' должно быть '$expected'",
                expected, fullName)
        }
    }

    @Test
    fun валидация_имени_автора_для_библиотечного_учета() {
        // Arrange
        val validAuthors = listOf(
            AuthorEntity(id = 1L, surname = "Толстой", name = "Лев", patronymic = "Николаевич"),
            AuthorEntity(id = 2L, surname = "Пушкин", name = "Александр", patronymic = "Сергеевич"),
            AuthorEntity(id = 3L, surname = "Smith-Jones", name = "John", patronymic = null),
            AuthorEntity(id = 4L, surname = "ван Гог", name = "Винсент", patronymic = null)
        )

        val invalidAuthors = listOf(
            AuthorEntity(id = 5L, surname = "", name = "Иван", patronymic = null),
            AuthorEntity(id = 6L, surname = "   ", name = "Петр", patronymic = null),
            AuthorEntity(id = 7L, surname = "Иванов", name = "", patronymic = null)
        )

        // Act & Assert для валидных
        validAuthors.forEach { author ->
            assertTrue("Автор '${author.surname}' должен быть валидным",
                isAuthorValid(author))
        }

        // Act & Assert для невалидных
        invalidAuthors.forEach { author ->
            assertFalse("Автор с пустым полем должен быть невалидным",
                isAuthorValid(author))
        }
    }

    @Test
    fun поиск_автора_по_разным_критериям() {
        // Arrange
        val authors = listOf(
            AuthorEntity(id = 1L, surname = "Толстой", name = "Лев", patronymic = "Николаевич"),
            AuthorEntity(id = 2L, surname = "Толстая", name = "Татьяна", patronymic = null),
            AuthorEntity(id = 3L, surname = "Пушкин", name = "Александр", patronymic = "Сергеевич"),
            AuthorEntity(id = 4L, surname = "Тургенев", name = "Иван", patronymic = "Сергеевич"),
            AuthorEntity(id = 5L, surname = "Достоевский", name = "Фёдор", patronymic = "Михайлович")
        )

        // Act
        val bySurname = findAuthorsBySurnamePart(authors, "Тол")
        val byName = findAuthorsByNamePart(authors, "Иван")
        val byFullName = findAuthorsByFullNamePart(authors, "Сергеевич")

        // Assert
        assertEquals("По фамилии 'Тол' должно найти 2 автора", 2, bySurname.size)
        assertEquals("По имени 'Иван' должно найти 1 автора", 1, byName.size)
        assertEquals("По отчеству 'Сергеевич' должно найти 2 автора", 2, byFullName.size)

        assertTrue("Должен найти Толстого", bySurname.any { it.surname == "Толстой" })
        assertTrue("Должен найти Тургенева", byName.any { it.surname == "Тургенев" })
        assertTrue("Должен найти Пушкина", byFullName.any { it.surname == "Пушкин" })
    }

    // Вспомогательные методы (должны быть в приложении)
    private fun formatAuthorName(author: AuthorEntity): String {
        return buildString {
            append(author.surname)
            author.name?.let { append(" $it") }
            author.patronymic?.let { append(" $it") }
        }.trim()
    }

    private fun isAuthorValid(author: AuthorEntity): Boolean {
        return author.surname.trim().isNotEmpty() &&
                author.name?.trim()?.isNotEmpty() ?: false
    }

    private fun findAuthorsBySurnamePart(authors: List<AuthorEntity>, query: String): List<AuthorEntity> {
        return authors.filter { it.surname.contains(query, ignoreCase = true) }
    }

    private fun findAuthorsByNamePart(authors: List<AuthorEntity>, query: String): List<AuthorEntity> {
        return authors.filter { it.name?.contains(query, ignoreCase = true) == true }
    }

    private fun findAuthorsByFullNamePart(authors: List<AuthorEntity>, query: String): List<AuthorEntity> {
        return authors.filter {
            it.surname.contains(query, ignoreCase = true) ||
                    it.name?.contains(query, ignoreCase = true) == true ||
                    it.patronymic?.contains(query, ignoreCase = true) == true
        }
    }
}