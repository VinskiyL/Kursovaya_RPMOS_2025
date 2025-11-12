package ru.kafpin.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import ru.kafpin.adapters.BooksAdapter
import ru.kafpin.api.models.Book
import ru.kafpin.databinding.ActivityBooksBinding
import ru.kafpin.viewmodels.BookViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.kafpin.viewmodels.BookViewModelFactory

class BooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBooksBinding
    private val viewModel: BookViewModel by viewModels {
        BookViewModelFactory(this)
    }
    private lateinit var adapter: BooksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        setupClickListeners()

        // Показываем начальное состояние
        showLoadingState()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("BooksActivity", "Swipe to refresh triggered")
            viewModel.refresh()

            binding.swipeRefreshLayout.postDelayed({
                if (binding.swipeRefreshLayout.isRefreshing) {
                    Log.w("BooksActivity", "Swipe refresh timeout - stopping animation")
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }, 10000)
        }

        // Настраиваем цвета индикатора (опционально)
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun setupRecyclerView() {
        adapter = BooksAdapter(
            onItemClick = { book ->
                showBookDetails(book)
            },
            onDetailsClick = { book ->
                showBookDetails(book)
            }
        )

        binding.booksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@BooksActivity)
            adapter = this@BooksActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        // Наблюдаем за текущими книгами
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentBooks.collect { books ->
                    if (books.isNotEmpty()) {
                        showContentState()
                        adapter.submitList(books)
                    } else {
                        showEmptyState()
                    }
                    // Кружок должен остановиться когда данные обновились
                    stopSwipeRefresh()
                }
            }
        }

        // Наблюдаем за состоянием кнопки "вперёд"
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasNextPage.collect { hasNext ->
                    binding.nextPageButton.isEnabled = hasNext
                }
            }
        }

        // Наблюдаем за состоянием кнопки "назад"
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasPreviousPage.collect { hasPrevious ->
                    binding.prevPageButton.isEnabled = hasPrevious
                }
            }
        }

        // Наблюдаем за информацией о странице
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pageInfo.collect { info ->
                    binding.pageIndicator.text = info
                    supportActionBar?.title = "Библиотека (${viewModel.totalBooksCount} книг)"
                }
            }
        }

        // Наблюдаем за загрузкой
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) {
                        showLoadingState()
                    } else {
                        // Когда загрузка завершилась - останавливаем кружок
                        stopSwipeRefresh()
                    }
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        // Наблюдаем за ошибками
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { errorMessage ->
                    errorMessage?.let { message ->
                        showErrorState(message)
                        // При ошибке тоже останавливаем кружок
                        stopSwipeRefresh()
                    }
                }
            }
        }
    }

    /**
     * Останавливает анимацию SwipeRefreshLayout
     * Вызывается когда данные загружены или произошла ошибка
     */
    private fun stopSwipeRefresh() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            Log.d("BooksActivity", "Stopping swipe refresh animation")
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupClickListeners() {
        binding.prevPageButton.setOnClickListener {
            Log.d("BooksActivity", "Previous page button clicked")
            viewModel.previousPage()
        }

        binding.nextPageButton.setOnClickListener {
            Log.d("BooksActivity", "Next page button clicked")
            viewModel.nextPage()
        }

        binding.retryButton.setOnClickListener {
            Log.d("BooksActivity", "Retry button clicked - calling refresh()")
            viewModel.refresh()
        }
    }

    // region Состояния UI

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefreshLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.swipeRefreshLayout.isEnabled = false
        binding.pageIndicator.text = "Загрузка..."

        // Блокируем кнопки пагинации во время загрузки
        binding.prevPageButton.isEnabled = false
        binding.nextPageButton.isEnabled = false
    }

    private fun showContentState() {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
        binding.swipeRefreshLayout.isEnabled = true
        // НЕ останавливаем здесь кружок - это делает stopSwipeRefresh()
    }

    private fun showErrorState(errorMessage: String) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isEnabled = false

        binding.errorText.text = errorMessage
        binding.pageIndicator.text = "Ошибка"
        // НЕ останавливаем здесь кружок - это делает stopSwipeRefresh()

        // Блокируем кнопки пагинации при ошибке
        binding.prevPageButton.isEnabled = false
        binding.nextPageButton.isEnabled = false
    }

    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isEnabled = false

        binding.errorText.text = "Книги не найдены"
        binding.retryButton.visibility = View.VISIBLE
        binding.pageIndicator.text = "Нет данных"
        // НЕ останавливаем здесь кружок - это делает stopSwipeRefresh()

        // Блокируем кнопки пагинации при пустом списке
        binding.prevPageButton.isEnabled = false
        binding.nextPageButton.isEnabled = false
    }

    // endregion

    private fun showBookDetails(book: Book) {
        // Создаем диалог с детальной информацией о книге
        val message = """
            📖 ${book.title}
            
            👨‍💼 Автор: ${book.authorDisplay}
            🏷️ Жанр: ${book.genreDisplay}
            📅 Год: ${book.datePublication}
            📍 Место: ${book.placePublication}
            🔢 Индекс: ${book.index}
            📚 Том: ${book.volume}
            
            ℹ️ ${book.informationPublication}
            
            ${if (book.isAvailable) "✅ Доступно: ${book.quantityRemaining} из ${book.quantityTotal}" else "❌ Нет в наличии"}
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("Информация о книге")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Поделиться") { dialog, _ ->
                shareBookInfo(book)
                dialog.dismiss()
            }
            .show()
    }

    private fun shareBookInfo(book: Book) {
        val shareText = """
            Рекомендую книгу: "${book.title}"
            Автор: ${book.authorDisplay}
            ${book.cover ?: ""}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(intent, "Поделиться книгой"))
    }

    // Обработка кнопки "Назад"
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}