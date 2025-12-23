package ru.kafpin.book.tests

import org.junit.Test
import org.junit.Assert.*
import ru.kafpin.data.models.GenreEntity

class GenreEntityTest {

    @Test
    fun категоризация_жанров_по_типам() {
        val allGenres = listOf(
            GenreEntity(id = 1L, name = "Научная фантастика"),
            GenreEntity(id = 2L, name = "Фэнтези"),
            GenreEntity(id = 3L, name = "Детектив"),
            GenreEntity(id = 4L, name = "Триллер"),
            GenreEntity(id = 5L, name = "Роман"),
            GenreEntity(id = 6L, name = "Поэзия"),
            GenreEntity(id = 7L, name = "Драма"),
            GenreEntity(id = 8L, name = "Комедия"),
            GenreEntity(id = 9L, name = "Программирование"),
            GenreEntity(id = 10L, name = "Научная литература")
        )

        val fictionGenres = allGenres.filter { genre ->
            genre.name in listOf("Научная фантастика", "Фэнтези", "Детектив",
                "Триллер", "Роман", "Драма", "Комедия")
        }

        val nonFictionGenres = allGenres.filter { genre ->
            genre.name in listOf("Программирование", "Научная литература")
        }

        val poetryGenres = allGenres.filter { genre ->
            genre.name == "Поэзия"
        }

        assertEquals("Должно быть 7 художественных жанров", 7, fictionGenres.size)
        assertEquals("Должно быть 2 нехудожественных жанра", 2, nonFictionGenres.size)
        assertEquals("Должен быть 1 поэтический жанр", 1, poetryGenres.size)
        assertTrue("Должен включать научную фантастику",
            fictionGenres.any { it.name == "Научная фантастика" })
        assertTrue("Должен включать программирование",
            nonFictionGenres.any { it.name == "Программирование" })
    }

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
    fun проверка_уникальности_имен_жанров() {
        val genres = listOf(
            GenreEntity(id = 1L, name = "Детектив"),
            GenreEntity(id = 2L, name = "Детектив"),
            GenreEntity(id = 3L, name = "Триллер"),
            GenreEntity(id = 4L, name = "Триллер"),
            GenreEntity(id = 5L, name = "Роман")
        )

        val uniqueNames = genres.map { it.name }.distinct()
        val duplicateNames = genres.groupBy { it.name }
            .filter { it.value.size > 1 }
            .keys

        assertEquals("Должно быть 3 уникальных имени", 3, uniqueNames.size)
        assertEquals("Должно быть 2 дублирующихся имени", 2, duplicateNames.size)
        assertTrue("Детектив должен быть дубликатом", duplicateNames.contains("Детектив"))
        assertTrue("Триллер должен быть дубликатом", duplicateNames.contains("Триллер"))
        assertFalse("Роман не должен быть дубликатом", duplicateNames.contains("Роман"))
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