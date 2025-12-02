package ru.kafpin.data

import ru.kafpin.repositories.BookDetailsRepository

object RepositoryProvider {
    private var _bookDetailsRepository: BookDetailsRepository? = null

    fun getBookDetailsRepository(database: LibraryDatabase): BookDetailsRepository {
        return _bookDetailsRepository ?: BookDetailsRepository(
            bookDao = database.bookDao(),
            authorDao = database.authorDao(),
            genreDao = database.genreDao(),
            bookAuthorDao = database.bookAuthorDao(),
            bookGenreDao = database.bookGenreDao()
        ).also {
            _bookDetailsRepository = it
        }
    }
}