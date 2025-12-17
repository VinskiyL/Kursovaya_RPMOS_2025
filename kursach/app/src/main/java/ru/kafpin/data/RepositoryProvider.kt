package ru.kafpin.data

import android.content.Context
import ru.kafpin.repositories.AuthRepository
import ru.kafpin.repositories.BookDetailsRepository
import ru.kafpin.repositories.BookRepository
import ru.kafpin.repositories.BookingRepository

object RepositoryProvider {
    private var _bookDetailsRepository: BookDetailsRepository? = null
    private var _authRepository: AuthRepository? = null
    private var _bookRepository: BookRepository? = null
    private var _bookingRepository: BookingRepository? = null

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
            bookingDao = database.bookingDao(),
            networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor
        ).also {
            _authRepository = it
        }
    }

    fun getBookingRepository(
        database: LibraryDatabase,
        authRepository: AuthRepository,
        context: Context
    ): BookingRepository {
        return _bookingRepository ?: BookingRepository(
            bookingDao = database.bookingDao(),
            authRepository = authRepository,
            context = context
        ).also {
            _bookingRepository = it
        }
    }
}