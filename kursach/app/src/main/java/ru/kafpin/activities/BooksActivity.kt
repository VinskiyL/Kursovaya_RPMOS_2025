package ru.kafpin.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ru.kafpin.adapters.BooksAdapter
import ru.kafpin.api.models.Book
import ru.kafpin.databinding.ActivityBooksBinding
import ru.kafpin.viewmodels.BookViewModel

class BooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBooksBinding
    private val viewModel: BookViewModel by viewModels()
    private lateinit var adapter: BooksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Показываем начальное состояние
        showLoadingState()
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
        viewModel.currentBooks.observe(this) { books ->
            if (books.isNotEmpty()) {
                showContentState()
                adapter.submitList(books)
            } else {
                showEmptyState()
            }
            updatePaginationButtons()
        }

        // Наблюдаем за загрузкой
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showLoadingState()
            }
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Наблюдаем за ошибками
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let { message ->
                showErrorState(message)
            }
        }
    }

    private fun setupClickListeners() {
        // Кнопки пагинации
        binding.prevPageButton.setOnClickListener {
            viewModel.previousPage()
        }

        binding.nextPageButton.setOnClickListener {
            viewModel.nextPage()
        }

        // Кнопка повтора при ошибке
        binding.retryButton.setOnClickListener {
            viewModel.refresh()
        }
    }

    private fun updatePaginationButtons() {
        binding.prevPageButton.isEnabled = viewModel.hasPreviousPage
        binding.nextPageButton.isEnabled = viewModel.hasNextPage
        binding.pageIndicator.text = viewModel.pageInfo

        // Показываем общее количество книг в заголовке
        supportActionBar?.title = "Библиотека (${viewModel.totalBooksCount} книг)"
    }

    // region Состояния UI

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.booksRecyclerView.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.pageIndicator.text = "Загрузка..."

        // Блокируем кнопки пагинации во время загрузки
        binding.prevPageButton.isEnabled = false
        binding.nextPageButton.isEnabled = false
    }

    private fun showContentState() {
        binding.progressBar.visibility = View.GONE
        binding.booksRecyclerView.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE

        updatePaginationButtons()
    }

    private fun showErrorState(errorMessage: String) {
        binding.progressBar.visibility = View.GONE
        binding.booksRecyclerView.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE

        binding.errorText.text = errorMessage
        binding.pageIndicator.text = "Ошибка"

        // Блокируем кнопки пагинации при ошибке
        binding.prevPageButton.isEnabled = false
        binding.nextPageButton.isEnabled = false
    }

    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.booksRecyclerView.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE

        binding.errorText.text = "Книги не найдены"
        binding.retryButton.visibility = View.VISIBLE
        binding.pageIndicator.text = "Нет данных"

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