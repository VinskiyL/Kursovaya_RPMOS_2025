package ru.kafpin.activities

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.launch
import ru.kafpin.R
import ru.kafpin.databinding.ActivityBookDetailsBinding
import ru.kafpin.utils.Constants.COVER_URL
import ru.kafpin.viewmodels.BookDetailsViewModel
import ru.kafpin.viewmodels.BookDetailsViewModelFactory

class BookDetailsActivity : BaseActivity<ActivityBookDetailsBinding>() {

    private val bookId: Long by lazy {
        intent.getLongExtra(EXTRA_BOOK_ID, -1L)
    }

    private val viewModel: BookDetailsViewModel by viewModels {
        BookDetailsViewModelFactory(this, bookId)
    }

    companion object {
        const val EXTRA_BOOK_ID = "book_id"

        fun start(context: Context, bookId: Long) {
            val intent = Intent(context, BookDetailsActivity::class.java)
            intent.putExtra(EXTRA_BOOK_ID, bookId)
            context.startActivity(intent)
        }
    }

    override fun inflateBinding(): ActivityBookDetailsBinding {
        return ActivityBookDetailsBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        if (bookId == -1L) {
            showErrorState("Неверный ID книги")
            return
        }

        setupSwipeRefresh()
        setupObservers()
        setupClickListeners()

        enableBackButton(true)
        setToolbarTitle("Детали книги")
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshBook()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun setupObservers() {
        // 1. Наблюдаем за состоянием загрузки
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // Не показываем ProgressBar если SwipeRefresh активен
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        binding.progressBar.isVisible = isLoading
                    }

                    // При загрузке скрываем контент и ошибку
                    if (isLoading) {
                        binding.contentScrollView.visibility = View.GONE
                        binding.errorLayout.visibility = View.GONE
                    }

                    // Останавливаем SwipeRefresh когда загрузка закончена
                    if (!isLoading) {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }

        // 2. Наблюдаем за данными книги
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.bookDetails.collect { details ->
                    details?.let {
                        bindBookDetails(it)
                        // Показываем контент, скрываем ошибку
                        binding.contentScrollView.visibility = View.VISIBLE
                        binding.errorLayout.visibility = View.GONE
                    }
                }
            }
        }

        // 3. Наблюдаем за ошибками
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        showErrorState(it)
                        // При ошибке показываем только её, скрываем контент
                        binding.contentScrollView.visibility = View.GONE
                    }
                }
            }
        }

        // 4. Наблюдаем за toast сообщениями
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.toastMessage.collect { message ->
                    message?.let {
                        Toast.makeText(this@BookDetailsActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearToast()
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }

        // Убрал refreshButton - теперь только SwipeRefresh
    }

    private fun bindBookDetails(details: ru.kafpin.data.models.BookWithDetails) {
        with(binding) {
            // ==================== ВСЕ ПОЛЯ ИЗ BookEntity ====================

            // 1. Основная информация
            bookTitle.text = details.book.title
            bookIndex.text = "Индекс: ${details.book.index}"
            bookAuthorsMark.text = "Авторский знак: ${details.book.authorsMark}"

            // 2. Издательство
            bookPlace.text = "Место издания: ${details.book.placePublication}"
            bookInfoPublication.text = "Сведения об издании: ${details.book.informationPublication}"

            // 3. Даты и объём
            bookYear.text = "Год издания: ${details.book.datePublication}"
            bookVolume.text = "Том: ${details.book.volume}"

            // 4. Количество
            val total = details.book.quantityTotal
            val remaining = details.book.quantityRemaining
            val isAvailable = remaining > 0

            bookTotal.text = "Всего экземпляров: $total"
            bookRemaining.text = "Осталось: $remaining"

            bookAvailability.text = if (isAvailable) {
                "✅ Доступно для выдачи"
            } else {
                "❒ Нет в наличии"
            }

            // 5. Авторы
            if (details.authors.isNotEmpty()) {
                authorsLabel.visibility = View.VISIBLE
                authorsList.visibility = View.VISIBLE
                authorsList.text = details.authors.joinToString("\n") { author ->
                    "${author.surname} ${author.name ?: ""} ${author.patronymic ?: ""}".trim()
                }
            } else {
                authorsLabel.visibility = View.GONE
                authorsList.visibility = View.GONE
            }

            // 6. Жанры
            if (details.genres.isNotEmpty()) {
                genresLabel.visibility = View.VISIBLE
                genresList.visibility = View.VISIBLE
                genresList.text = details.genres.joinToString("\n") { it.name }
            } else {
                genresLabel.visibility = View.GONE
                genresList.visibility = View.GONE
            }

            // 7. Обложка
            val fullImageUrl = COVER_URL + details.book.cover
            Glide.with(this@BookDetailsActivity)
                .load(fullImageUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(bookCover)

            // ==================== ВИДИМОСТЬ ПОЛЕЙ ====================
            bookTitle.isVisible = details.book.title.isNotBlank()
            bookIndex.isVisible = details.book.index.isNotBlank()
            bookAuthorsMark.isVisible = details.book.authorsMark.isNotBlank()
            bookPlace.isVisible = details.book.placePublication.isNotBlank()
            bookInfoPublication.isVisible = details.book.informationPublication.isNotBlank()
            bookYear.isVisible = details.book.datePublication.isNotBlank()
            bookVolume.isVisible = details.book.volume > 0
            bookTotal.isVisible = details.book.quantityTotal > 0
            bookRemaining.isVisible = true // всегда показываем
        }
    }

    private fun showErrorState(message: String) {
        with(binding) {
            errorText.text = message
            errorLayout.visibility = View.VISIBLE
            contentScrollView.visibility = View.GONE
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
        }
    }
}