package ru.kafpin.data.mappers

import ru.kafpin.api.models.AuthorBook
import ru.kafpin.api.models.BookGenre
import ru.kafpin.data.models.BookAuthorCrossRef
import ru.kafpin.data.models.BookGenreCrossRef

fun AuthorBook.toBookAuthorCrossRef(): BookAuthorCrossRef {
    return BookAuthorCrossRef(
        id = id,
        bookId = bookId,
        authorId = authorId
    )
}

fun BookGenre.toBookGenreCrossRef(): BookGenreCrossRef {
    return BookGenreCrossRef(
        id = id,
        bookId = bookId,
        genreId = genreId
    )
}

// Списки
fun List<AuthorBook>.toBookAuthorCrossRefs(): List<BookAuthorCrossRef> = map { it.toBookAuthorCrossRef() }
fun List<BookGenre>.toBookGenreCrossRefs(): List<BookGenreCrossRef> = map { it.toBookGenreCrossRef() }