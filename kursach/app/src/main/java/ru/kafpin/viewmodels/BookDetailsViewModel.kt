package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.data.models.BookWithDetails
import ru.kafpin.repositories.BookRepository
import ru.kafpin.utils.NotificationHelper
import java.time.LocalDate

class BookDetailsViewModel(private val context: Context, private val bookId: Long) : ViewModel() {
    private val TAG = "BookDetailsViewModel"

    private val database = LibraryDatabase.getInstance(context)
    private val bookRepository = BookRepository(context)
    private val bookDetailsRepository = RepositoryProvider.getBookDetailsRepository(database)
    private val authRepository = RepositoryProvider.getAuthRepository(database, context)
    private val bookingRepository = RepositoryProvider.getBookingRepository(
        database = database,
        authRepository = authRepository,
        context = context
    )

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _bookDetails = MutableStateFlow<BookWithDetails?>(null)
    val bookDetails: StateFlow<BookWithDetails?> = _bookDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _hasActiveBooking = MutableStateFlow(false)
    val hasActiveBooking: StateFlow<Boolean> = _hasActiveBooking.asStateFlow()

    init {
        Log.d(TAG, "Initializing for bookId: $bookId")
        loadBookDetailsWithFlow()

        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch

            bookingRepository.getBookingsByUserFlow(userId)
                .collect { bookings ->
                    val hasActiveBooking = bookings.any { booking ->
                        booking.booking.bookId == bookId &&
                                booking.booking.status in listOf(
                            ru.kafpin.data.models.BookingStatus.PENDING,
                            ru.kafpin.data.models.BookingStatus.CONFIRMED,
                            ru.kafpin.data.models.BookingStatus.ISSUED
                        )
                    }

                    Log.d(TAG, "Активная бронь на книгу $bookId: $hasActiveBooking")
                }
        }
    }

    private fun loadBookDetailsWithFlow() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            bookDetailsRepository.getBookWithDetailsFlow(bookId)
                .catch { e ->
                    Log.e(TAG, "Flow error", e)
                    _errorMessage.value = "Ошибка загрузки: ${e.message}"
                    _isLoading.value = false
                }
                .collect { bookWithDetails ->
                    _bookDetails.value = bookWithDetails
                    _isLoading.value = false

                    if (bookWithDetails == null) {
                        _errorMessage.value = "Книга не найдена"
                    } else {
                        val fifteenMinutes = 15 * 60 * 1000L
                        val needRefresh = System.currentTimeMillis() - bookWithDetails.book.lastSynced > fifteenMinutes

                        if (needRefresh) {
                            try {
                                bookRepository.syncSingleBook(bookId)
                                Log.d(TAG, "🔄 Auto-refresh book $bookId")
                            } catch (e: Exception) {
                                Log.e(TAG, "Auto-refresh error", e)
                            }
                        }
                    }
                }
        }
    }

    fun refreshBook() {
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = bookRepository.syncSingleBook(bookId)

                if (success) {
                    _toastMessage.value = "✅ Книга обновлена"
                    Log.d(TAG, "✅ Manual refresh successful")
                } else {
                    _toastMessage.value = "❌ Не удалось обновить"
                    Log.w(TAG, "⚠️ Manual refresh failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Manual refresh error", e)
                _errorMessage.value = "Ошибка обновления: ${e.message}"
                _toastMessage.value = "⚠️ Ошибка обновления"
            } finally {
                delay(500)
                _isLoading.value = false
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun canBookThisBook(): Boolean {
        return (bookDetails.value?.book?.quantityRemaining ?: 0) > 0
    }

    suspend fun createBooking(
        bookId: Long,
        quantity: Int,
        dateIssue: LocalDate,
        dateReturn: LocalDate
    ): Long? {
        return try {
            _isCreating.value = true
            _errorMessage.value = null

            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _errorMessage.value = "Пользователь не авторизован"
                return null
            }

            val bookDetails = _bookDetails.value
                ?: bookDetailsRepository.getBookWithDetails(bookId)

            if (bookDetails == null) {
                _errorMessage.value = "Не удалось загрузить данные книги"
                return null
            }

            if (quantity > bookDetails.book.quantityRemaining) {
                _errorMessage.value = "Недостаточно книг в наличии"
                return null
            }

            val hasExisting = bookingRepository.hasExistingBooking(
                bookId = bookId,
                userId = userId
            )

            if (hasExisting) {
                _errorMessage.value = "У вас уже есть активная бронь на эту книгу"
                return null
            }

            val authorsString = bookDetails.authors.joinToString(", ") {
                "${it.surname} ${it.name}".trim()
            }

            val genresString = bookDetails.genres.joinToString(", ") { it.name }

            val bookingId = bookingRepository.createLocalBooking(
                bookId = bookDetails.book.id,
                bookTitle = bookDetails.book.title,
                bookAuthors = authorsString,
                bookGenres = genresString,
                availableCopies = bookDetails.book.quantityRemaining,
                userId = userId,
                quantity = quantity,
                dateIssue = dateIssue,
                dateReturn = dateReturn
            )

            if (authRepository.hasValidTokenForApi()) {
                val syncResults = bookingRepository.syncPendingBookings()

                val bookingErrors = syncResults.filterIsInstance<ru.kafpin.repositories.SyncResult.Error>()
                    .filter { it.bookingId == bookingId }

                if (bookingErrors.isNotEmpty()) {
                    val error = bookingErrors.first()
                    when (error.errorType) {
                        ru.kafpin.repositories.SyncErrorType.DUPLICATE_BOOKING -> {
                            _errorMessage.value = "Не удалось создать бронь: ${error.message}"
                            _toastMessage.value = "❌ Бронь не создана (дубликат)"
                            return null
                        }
                        ru.kafpin.repositories.SyncErrorType.INSUFFICIENT_BOOKS -> {
                            _errorMessage.value = "Не удалось создать бронь: ${error.message}"
                            _toastMessage.value = "⚠️ Бронь создана, но требует внимания"
                        }
                        else -> {
                            _errorMessage.value = "Ошибка синхронизации: ${error.message}"
                            _toastMessage.value = "⚠️ Бронь создана локально"
                        }
                    }
                } else {
                    _toastMessage.value = "✅ Бронь создана и синхронизирована!"
                }
            } else {
                _toastMessage.value = "📴 Бронь создана локально (оффлайн)"
            }

            if (bookingId != null) {
                NotificationHelper.showBookingCreatedNotification(
                    context = context,
                    bookTitle = bookDetails.book.title,
                    bookingId = bookingId
                )

                try {
                    bookRepository.syncSingleBook(bookId)
                } catch (e: Exception) {
                    // Игнорируем ошибки обновления
                }
            }

            bookingId
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка создания брони: ${e.message}"
            Log.e(TAG, "Ошибка создания брони", e)
            null
        } finally {
            _isCreating.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun hasActiveBookingForThisBook(): Boolean {
        val userId = authRepository.getCurrentUserId() ?: return false
        val bookDetails = _bookDetails.value ?: return false

        return try {
            bookingRepository.hasExistingBooking(bookDetails.book.id, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки активных броней", e)
            false
        }
    }
}