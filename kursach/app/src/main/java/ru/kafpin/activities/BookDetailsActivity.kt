package ru.kafpin.activities

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.kafpin.R
import ru.kafpin.databinding.ActivityBookDetailsBinding
import ru.kafpin.utils.loadImage
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

        setupToolbar()
        setupObservers()
        setupClickListeners()
    }

    private fun setupToolbar() {
        enableBackButton(true)
        setToolbarTitle("Детали книги")
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.isVisible = isLoading
                    binding.contentScrollView.visibility = View.VISIBLE
                    binding.errorLayout.visibility = View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.bookDetails.collect { details ->
                    details?.let { bindBookDetails(it) }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let { showErrorState(it) }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun bindBookDetails(details: ru.kafpin.data.models.BookWithDetails) {
        with(binding) {
            // Основная информация
            bookTitle.text = details.book.title
            bookIndex.text = "Индекс: ${details.book.index}"
            bookYear.text = "Год издания: ${details.book.datePublication}"
            bookVolume.text = "Том: ${details.book.volume}"
            bookPlace.text = "Издательство: ${details.book.placePublication}"
            bookDescription.text = details.book.informationPublication

            // Авторы
            if (details.authors.isNotEmpty()) {
                authorsLabel.visibility = View.VISIBLE
                authorsList.visibility = View.VISIBLE
                authorsList.text = details.authorsFormatted
            } else {
                authorsLabel.visibility = View.GONE
                authorsList.visibility = View.GONE
            }

            // Жанры
            if (details.genres.isNotEmpty()) {
                genresLabel.visibility = View.VISIBLE
                genresList.visibility = View.VISIBLE
                genresList.text = details.genresFormatted
            } else {
                genresLabel.visibility = View.GONE
                genresList.visibility = View.GONE
            }

            // Доступность
            val total = details.book.quantityTotal
            val remaining = details.book.quantityRemaining
            val isAvailable = remaining > 0

            bookAvailability.text = if (isAvailable) {
                "✅ Доступно: $remaining из $total"
            } else {
                "❌ Нет в наличии"
            }

            // Обложка
            bookCover.loadImage(
                details.book.cover,
                placeholder = R.drawable.ic_book_placeholder
            )

            // Показать контент
            contentScrollView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            errorLayout.visibility = View.GONE
        }
    }

    private fun showErrorState(message: String) {
        with(binding) {
            errorText.text = message
            errorLayout.visibility = View.VISIBLE
            contentScrollView.visibility = View.GONE
            progressBar.visibility = View.GONE
        }
    }
}