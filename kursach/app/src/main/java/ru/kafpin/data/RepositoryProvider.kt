package ru.kafpin.data

import android.content.Context
import ru.kafpin.repositories.AuthRepository
import ru.kafpin.repositories.BookDetailsRepository

object RepositoryProvider {
    private var _bookDetailsRepository: BookDetailsRepository? = null
    private var _authRepository: AuthRepository? = null

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

    fun getAuthRepository(
        database: LibraryDatabase,
        context: Context
    ): AuthRepository {
        return _authRepository ?: AuthRepository(
            authDao = database.authDao(),
            userDao = database.userDao(),
            networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor
        ).also {
            _authRepository = it
        }
    }
}