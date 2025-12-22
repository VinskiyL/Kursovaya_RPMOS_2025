package ru.kafpin.data

import android.content.Context
import ru.kafpin.repositories.AuthRepository
import ru.kafpin.repositories.BookDetailsRepository
import ru.kafpin.repositories.BookRepository
import ru.kafpin.repositories.BookingRepository
import ru.kafpin.repositories.OrderRepository
import ru.kafpin.repositories.ProfileRepository

object RepositoryProvider {
    private var _bookDetailsRepository: BookDetailsRepository? = null
    private var _authRepository: AuthRepository? = null
    private var _orderRepository: OrderRepository? = null
    private var _bookingRepository: BookingRepository? = null

    private var _profileRepository: ProfileRepository? = null

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
            orderDao = database.orderDao(),
            profileDao = database.profileDao(),
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

    fun getOrderRepository(
        database: LibraryDatabase,
        authRepository: AuthRepository,
        context: Context
    ): OrderRepository {
        return _orderRepository ?: OrderRepository(
            orderDao = database.orderDao(),
            authRepository = authRepository,
            context = context
        ).also {
            _orderRepository = it
        }
    }

    fun getProfileRepository(
        database: LibraryDatabase,
        authRepository: AuthRepository,
        context: Context
    ): ProfileRepository {
        return _profileRepository ?: ProfileRepository(
            profileDao = database.profileDao(),
            authRepository = authRepository,
            context = context
        ).also {
            _profileRepository = it
        }
    }
}