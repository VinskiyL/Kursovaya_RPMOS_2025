package ru.kafpin.data.models

data class BookWithDetails(
    val book: BookEntity,
    val authors: List<AuthorEntity>,
    val genres: List<GenreEntity>
) {
    val authorsFormatted: String
        get() = authors.joinToString(", ") { it.surname }

    val genresFormatted: String
        get() = genres.joinToString(", ") { it.name }
}