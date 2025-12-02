package ru.kafpin.repositories

import ru.kafpin.data.dao.*
import ru.kafpin.data.mappers.*
import ru.kafpin.data.models.BookWithDetails

class BookDetailsRepository(
    private val bookDao: BookDao,
    private val authorDao: AuthorDao,
    private val genreDao: GenreDao,
    private val bookAuthorDao: BookAuthorDao,
    private val bookGenreDao: BookGenreDao
) {
    suspend fun getBookWithDetails(bookId: Long): BookWithDetails? {
        val bookEntity = bookDao.getBookById(bookId) ?: return null

        val authorIds = bookAuthorDao.getAuthorIdsForBook(bookId)
        val genreIds = bookGenreDao.getGenreIdsForBook(bookId)

        val authors = authorDao.getAuthorsByIds(authorIds).map { it.toAuthor() }
        val genres = genreDao.getGenresByIds(genreIds).map { it.toGenre() }

        return BookWithDetails(
            book = bookEntity.toBook(),
            authors = authors,
            genres = genres
        )
    }

    suspend fun getBooksByAuthor(authorId: Long): List<BookWithDetails> {
        val bookIds = bookAuthorDao.getBookIdsForAuthor(authorId)
        return bookIds.mapNotNull { bookId ->
            getBookWithDetails(bookId)
        }
    }

    suspend fun getBooksByGenre(genreId: Long): List<BookWithDetails> {
        val bookIds = bookGenreDao.getBookIdsForGenre(genreId)
        return bookIds.mapNotNull { bookId ->
            getBookWithDetails(bookId)
        }
    }

    suspend fun getAllBooksWithDetails(): List<BookWithDetails> {
        val books = bookDao.getAllBooks()
        return books.mapNotNull { bookEntity ->
            getBookWithDetails(bookEntity.id)
        }
    }
}