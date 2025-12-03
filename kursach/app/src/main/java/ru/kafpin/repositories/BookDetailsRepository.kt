package ru.kafpin.repositories

import android.util.Log
import ru.kafpin.data.dao.*
import ru.kafpin.data.models.BookWithDetails

class BookDetailsRepository(
    private val bookDao: BookDao,
    private val authorDao: AuthorDao,
    private val genreDao: GenreDao,
    private val bookAuthorDao: BookAuthorDao,
    private val bookGenreDao: BookGenreDao
) {
    suspend fun searchBooksWithDetails(query: String): List<BookWithDetails> {
        val byTitle = bookDao.searchBooksByTitle(query)
        val byAuthor = bookDao.searchBooksByAuthor(query)
        val byGenre = bookDao.searchBooksByGenre(query)

        val allBooks = (byTitle + byAuthor + byGenre)
        val uniqueBookIds = allBooks.map { it.id }.distinct()

        if (uniqueBookIds.isEmpty()) return emptyList()

        val authorRelations = bookAuthorDao.getAuthorRelationsForBooks(uniqueBookIds)
        val genreRelations = bookGenreDao.getGenreRelationsForBooks(uniqueBookIds)

        val authorIds = authorRelations.map { it.authorId }.distinct()
        val allAuthors = authorDao.getAuthorsByIds(authorIds).associateBy { it.id }

        val genreIds = genreRelations.map { it.genreId }.distinct()
        val allGenres = genreDao.getGenresByIds(genreIds).associateBy { it.id }

        return allBooks.map { bookEntity ->
            val bookAuthors = authorRelations
                .filter { it.bookId == bookEntity.id }
                .mapNotNull { allAuthors[it.authorId] }

            val bookGenres = genreRelations
                .filter { it.bookId == bookEntity.id }
                .mapNotNull { allGenres[it.genreId] }

            BookWithDetails(
                book = bookEntity,
                authors = bookAuthors,
                genres = bookGenres
            )
        }
    }

    suspend fun getBookWithDetails(bookId: Long): BookWithDetails? {
        val bookEntity = bookDao.getBookById(bookId) ?: return null

        val authorIds = bookAuthorDao.getAuthorIdsForBook(bookId)
        val genreIds = bookGenreDao.getGenreIdsForBook(bookId)

        val authors = authorDao.getAuthorsByIds(authorIds)
        val genres = genreDao.getGenresByIds(genreIds)

        return BookWithDetails(
            book = bookEntity,
            authors = authors,
            genres = genres
        )
    }

    suspend fun getAllBooksWithDetails(): List<BookWithDetails> {
        Log.d("BookDetailsRepo", "=== DEBUG START ===")

        val books = bookDao.getAllBooks()
        if (books.isEmpty()) return emptyList()

        Log.d("BookDetailsRepo", "Books in DB (${books.size}): ${books.map { it.id }}")
        val bookIds = books.map { it.id }

        val authorRelations = bookAuthorDao.getAuthorRelationsForBooks(bookIds)

        Log.d("BookDetailsRepo", "All author relations (${authorRelations.size}):")
        authorRelations.forEach { rel ->
            Log.d("BookDetailsRepo", "  Book ${rel.bookId} -> Author ${rel.authorId}")
        }

        val genreRelations = bookGenreDao.getGenreRelationsForBooks(bookIds)

        Log.d("BookDetailsRepo", "All author relations (${genreRelations.size}):")
        genreRelations.forEach { rel ->
            Log.d("BookDetailsRepo", "  Book ${rel.bookId} -> Author ${rel.genreId}")
        }

        val authorIds = authorRelations.map { it.authorId }.distinct()
        val allAuthors = authorDao.getAuthorsByIds(authorIds).associateBy { it.id }

        Log.d("BookDetailsRepo", "All authors in DB (${allAuthors.size}):")
        allAuthors.forEach { id, author ->
            Log.d("BookDetailsRepo", "  Author ${id}: ${author.surname} ${author.name}")
        }

        val genreIds = genreRelations.map { it.genreId }.distinct()
        val allGenres = genreDao.getGenresByIds(genreIds).associateBy { it.id }

        Log.d("BookDetailsRepo", "All genres in DB (${allGenres.size}):")
        allGenres.forEach { id, genre ->
            Log.d("BookDetailsRepo", "  Genre ${id}: ${genre.name}")
        }

        Log.d("BookDetailsRepo", "=== DEBUG END ===")

        return books.map { bookEntity ->
            val bookAuthors = authorRelations
                .filter { it.bookId == bookEntity.id }
                .mapNotNull { allAuthors[it.authorId] }

            val bookGenres = genreRelations
                .filter { it.bookId == bookEntity.id }
                .mapNotNull { allGenres[it.genreId] }

            BookWithDetails(
                book = bookEntity,
                authors = bookAuthors,
                genres = bookGenres
            )
        }
    }
}