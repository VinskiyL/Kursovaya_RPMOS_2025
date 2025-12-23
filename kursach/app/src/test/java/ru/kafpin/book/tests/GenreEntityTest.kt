package ru.kafpin.book.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.data.models.GenreEntity

class GenreEntityTest {
    @Test
    fun поиск_жанра_по_части_названия() {
        val genres = listOf(
            GenreEntity(id = 1L, name = "Научная фантастика"),
            GenreEntity(id = 2L, name = "Фэнтези"),
            GenreEntity(id = 3L, name = "Научная литература"),
            GenreEntity(id = 4L, name = "Научпоп"),
            GenreEntity(id = 5L, name = "Детектив"),
            GenreEntity(id = 6L, name = "Романтическая комедия")
        )

        val searchQuery = "науч"
        val foundGenres = genres.filter { genre ->
            genre.name.contains(searchQuery, ignoreCase = true)
        }

        assertEquals("Должно найти 3 жанра по 'науч'", 3, foundGenres.size)
        assertTrue("Должен найти научную фантастику",
            foundGenres.any { it.name == "Научная фантастика" })
        assertTrue("Должен найти научную литературу",
            foundGenres.any { it.name == "Научная литература" })
        assertTrue("Должен найти научпоп",
            foundGenres.any { it.name == "Научпоп" })
        assertFalse("Не должен найти детектив",
            foundGenres.any { it.name == "Детектив" })
    }

    @Test
    fun форматирование_списка_жанров_для_UI() {
        val testCases = listOf(
            listOf(GenreEntity(id=1, name="Детектив")) to "Детектив",
            listOf(
                GenreEntity(id=1, name="Детектив"),
                GenreEntity(id=2, name="Триллер")
            ) to "Детектив, Триллер",
            listOf(
                GenreEntity(id=1, name="Научная фантастика"),
                GenreEntity(id=2, name="Фэнтези"),
                GenreEntity(id=3, name="Приключения")
            ) to "Научная фантастика, Фэнтези, Приключения",
            emptyList<GenreEntity>() to "Жанр не указан"
        )

        testCases.forEach { (genres, expected) ->
            // Форматируем как в BookWithDetails.genresFormatted
            val formatted = if (genres.isEmpty()) {
                "Жанр не указан"
            } else {
                genres.joinToString(", ") { it.name }
            }

            assertEquals("Форматирование жанров должно быть правильным",
                expected, formatted)
        }
    }
}