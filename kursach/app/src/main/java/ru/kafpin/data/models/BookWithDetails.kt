package ru.kafpin.data.models

import ru.kafpin.api.models.Book
import ru.kafpin.api.models.Author
import ru.kafpin.api.models.Genre

data class BookWithDetails(
    val book: Book,
    val authors: List<Author>,
    val genres: List<Genre>
) {
    val authorsFormatted: String
        get() = authors.joinToString(", ") { it.authorSurname }

    val genresFormatted: String
        get() = genres.joinToString(", ") { it.name }
}