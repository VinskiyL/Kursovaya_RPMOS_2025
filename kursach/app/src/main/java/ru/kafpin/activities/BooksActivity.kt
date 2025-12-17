package ru.kafpin.activities

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import ru.kafpin.adapters.BooksAdapter
import ru.kafpin.databinding.ActivityBooksBinding
import ru.kafpin.viewmodels.BookViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.kafpin.R
import ru.kafpin.viewmodels.BookViewModelFactory
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider

class BooksActivity : BaseActivity<ActivityBooksBinding>() {
    private val TAG = "BooksActivity"

    private val viewModel: BookViewModel by viewModels {
        BookViewModelFactory(this)
    }
    private lateinit var adapter: BooksAdapter
    private var wasOffline = false

    override fun inflateBinding(): ActivityBooksBinding {
        Log.d(TAG, "inflateBinding()")
        return ActivityBooksBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        Log.d(TAG, "setupUI()")

        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        setupClickListeners()

        setToolbarTitle("Библиотека")
        setupToolbarButtons(
            showBackButton = true,
            showLogoutButton = false
        )
        showLoadingState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onCreateOptionsMenu()")
        menuInflater.inflate(R.menu.menu_books, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

        searchView.queryHint = "Поиск по названию..."

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                btnBack.visibility = View.GONE
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                btnBack.visibility = View.VISIBLE
                return true
            }
        })

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d(TAG, "Search query changed: '$newText'")
                viewModel.performSearch(newText ?: "")
                return true
            }
        })

        return true
    }

    private fun setupSwipeRefresh() {
        Log.d(TAG, "setupSwipeRefresh()")
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe to refresh triggered")
            checkTokenAndRefresh()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView()")
        adapter = BooksAdapter { bookWithDetails ->
            Log.d(TAG, "Book clicked: ${bookWithDetails.book.title}")
            showBookDetails(bookWithDetails)
        }

        binding.booksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@BooksActivity)
            adapter = this@BooksActivity.adapter
            setHasFixedSize(true)
        }
        Log.d(TAG, "RecyclerView setup complete")
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers()")

        // Отслеживаем загрузку
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    Log.d(TAG, "Loading state changed: $isLoading")
                    if (isLoading) {
                        showLoadingState()
                    } else {
                        Log.d(TAG, "Loading finished")
                        stopSwipeRefresh()
                        showContentState()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentPageBooks.collect { books ->
                    Log.d(TAG, "📚 currentPageBooks updated: ${books.size} books")

                    if (books.isNotEmpty()) {
                        Log.d(TAG, "📖 Showing ${books.size} books")
                        showContentState()
                        adapter.submitList(books)
                    } else {
                        Log.d(TAG, "📭 No books to show")
                        showEmptyState()
                    }
                }
            }
        }

        // Отслеживаем статус сети
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isOnline.collect { isOnline ->
                    Log.d(TAG, "Network status changed: $isOnline")

                    if (wasOffline != !isOnline) {
                        val message = if (isOnline) "✅ Сеть восстановлена" else "🔴 Нет подключения"
                        showToast(message)
                        wasOffline = !isOnline
                    }

                    updateToolbarWithNetworkStatus(isOnline)
                }
            }
        }

        // Отслеживаем пагинацию
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paginationInfo.collect { info ->
                    Log.d(TAG, "Pagination info: ${info.pageInfoText}")
                    binding.nextPageButton.isEnabled = info.hasNextPage
                    binding.prevPageButton.isEnabled = info.hasPreviousPage
                    binding.pageIndicator.text = info.pageInfoText
                    updateToolbarWithBookCount()
                }
            }
        }

        // Отслеживаем ошибки
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { errorMessage ->
                    errorMessage?.let { message ->
                        Log.e(TAG, "Error received: $message")

                        showToast(message)
                        stopSwipeRefresh()

                        viewModel.clearErrorMessage()
                    }
                }
            }
        }
    }

    private fun updateToolbarWithNetworkStatus(isOnline: Boolean) {
        Log.d(TAG, "updateToolbarWithNetworkStatus: $isOnline")
        val networkStatus = if (isOnline) "✅ on" else "🔴 off"
        val bookCount = viewModel.allBooksWithDetails.value.size
        val title = "$bookCount книг $networkStatus"
        Log.d(TAG, "Setting toolbar title: '$title'")
        setToolbarTitle(title)
    }

    private fun updateToolbarWithBookCount() {
        val bookCount = viewModel.allBooksWithDetails.value.size
        Log.d(TAG, "updateToolbarWithBookCount: $bookCount books")
        val currentTitle = supportActionBar?.title?.toString() ?: ""

        val networkStatus = when {
            currentTitle.contains("✅") -> " ✅ on"
            currentTitle.contains("🔴") -> " 🔴 off"
            else -> ""
        }

        setToolbarTitle("$bookCount книг $networkStatus")
    }

    private fun stopSwipeRefresh() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            Log.d(TAG, "Stopping swipe refresh")
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupClickListeners() {
        Log.d(TAG, "setupClickListeners()")
        binding.prevPageButton.setOnClickListener {
            Log.d(TAG, "Previous page button clicked")
            viewModel.previousPage()
        }

        binding.nextPageButton.setOnClickListener {
            Log.d(TAG, "Next page button clicked")
            viewModel.nextPage()
        }

        binding.retryButton.setOnClickListener {
            Log.d(TAG, "Retry button clicked")
            checkTokenAndRefresh()
        }
    }

    private fun checkTokenAndRefresh() {
        lifecycleScope.launch {
            try {
                val database = LibraryDatabase.getInstance(this@BooksActivity)

                val authRepository = RepositoryProvider.getAuthRepository(database, this@BooksActivity)
                val sessionInfo = authRepository.getSessionInfo()
                Log.d(TAG, "📊 Информация о сессии: $sessionInfo")

                val status = sessionInfo["status"] as? String ?: "unknown"

                when (status) {
                    "no_session" -> {
                        Log.w(TAG, "❌ Нет активной сессии")
                        showToast("Нет активной сессии. Войдите заново.")
                        navigateToLogin()
                        return@launch
                    }

                    "expired" -> {
                        Log.w(TAG, "⏰ Токены истекли")
                        showToast("Сессия истекла. Войдите заново.")
                        authRepository.forceLogout()
                        navigateToLogin()
                        return@launch
                    }

                    "refresh_expiring" -> {
                        Log.w(TAG, "⚠️ Refresh token скоро истечёт")
                        showToast("Сессия скоро истечёт. Рекомендуем перезайти.")
                        if (authRepository.refreshTokenIfNeeded()) {
                            showToast("Токен обновлён")
                        }
                    }

                    "can_refresh" -> {
                        Log.d(TAG, "🔄 Можно обновить токен")
                        authRepository.refreshTokenIfNeeded()
                    }

                    "access_valid" -> {
                        Log.d(TAG, "✅ Токен валиден")
                    }
                }

                viewModel.refresh()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при проверке токена", e)
                showToast("Ошибка. Повторите позднее.")
                viewModel.refresh()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // region Состояния UI

    private fun showLoadingState() {
        Log.d(TAG, "showLoadingState()")
        binding.progressBar.visibility = View.VISIBLE
        binding.booksRecyclerView.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.swipeRefreshLayout.isEnabled = false
        binding.pageIndicator.text = "Загрузка..."

        binding.prevPageButton.isEnabled = false
        binding.nextPageButton.isEnabled = false
    }

    private fun showContentState() {
        Log.d(TAG, "showContentState()")
        binding.progressBar.visibility = View.GONE
        binding.booksRecyclerView.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
        binding.swipeRefreshLayout.isEnabled = true

        val pagination = viewModel.paginationInfo.value
        binding.prevPageButton.isEnabled = pagination.hasPreviousPage
        binding.nextPageButton.isEnabled = pagination.hasNextPage
        binding.pageIndicator.text = pagination.pageInfoText
    }

    private fun showEmptyState() {
        Log.d(TAG, "showEmptyState()")
        binding.progressBar.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isEnabled = false

        binding.errorText.text = "Книги не найдены"
        binding.errorText.setTextColor(getColor(R.color.colorWarning))
        binding.retryButton.visibility = View.VISIBLE
        binding.pageIndicator.text = "Нет данных"

        binding.prevPageButton.isEnabled = false
        binding.nextPageButton.isEnabled = false
    }

    // endregion

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showBookDetails(bookWithDetails: ru.kafpin.data.models.BookWithDetails) {
        Log.d(TAG, "showBookDetails() for book ID: ${bookWithDetails.book.id}, title: ${bookWithDetails.book.title}")
        BookDetailsActivity.start(this, bookWithDetails.book.id)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, BooksActivity::class.java)
            context.startActivity(intent)
        }
    }
}