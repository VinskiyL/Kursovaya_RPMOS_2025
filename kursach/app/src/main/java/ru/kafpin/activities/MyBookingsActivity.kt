package ru.kafpin.activities

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kafpin.MyApplication
import ru.kafpin.R
import ru.kafpin.databinding.ActivityMyBookingsBinding
import ru.kafpin.ui.bookings.BookingsAdapter
import ru.kafpin.viewmodels.BookingViewModel
import ru.kafpin.viewmodels.BookingViewModelFactory

class MyBookingsActivity : BaseActivity<ActivityMyBookingsBinding>() {

    private val TAG = "MyBookingsActivity"

    private val viewModel: BookingViewModel by viewModels {
        BookingViewModelFactory.getInstance(this)
    }

    private var wasOffline = false
    private lateinit var adapter: BookingsAdapter
    private val shownErrorIds = mutableSetOf<Long>()

    override fun inflateBinding(): ActivityMyBookingsBinding {
        return ActivityMyBookingsBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbarButtons(
            showBackButton = true,
            showLogoutButton = false
        )
        setToolbarTitle("Мои бронирования")

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()

        startNetworkMonitoring()
    }

    private fun startNetworkMonitoring() {
        val app = application as MyApplication
        lifecycleScope.launch {
            app.networkMonitor.isOnline.collect { isOnline ->
                updateNetworkStatus(isOnline)

                if (wasOffline != !isOnline) {
                    val message = if (isOnline) "✅ Сеть восстановлена" else "🔴 Нет подключения"
                    showToast(message)
                    wasOffline = !isOnline
                }
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateNetworkStatus(isOnline: Boolean) {
        val baseTitle = "Бронирования "
        val networkStatus = if (isOnline) " ✅ on" else " 🔴 off"
        setToolbarTitle("$baseTitle$networkStatus")
    }

    private fun setupRecyclerView() {
        adapter = BookingsAdapter(
            onItemClick = { booking ->
                BookingDetailsActivity.start(this@MyBookingsActivity, booking.booking.localId)
            },
            onDeleteClick = { booking ->
                showDeleteDialog(booking)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyBookingsActivity)
            adapter = this@MyBookingsActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.bookings.collect { bookings ->
                adapter.submitList(bookings)
                binding.emptyView.isVisible = bookings.isEmpty()
                binding.recyclerView.isVisible = bookings.isNotEmpty()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.isVisible = isLoading
                Log.d(TAG, "Loading state: $isLoading")

                if (!isLoading) {
                    stopSwipeRefresh()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    showSnackbar(it)
                    viewModel.clearError()
                    stopSwipeRefresh()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.syncErrors.collect { errors ->
                errors.forEach { error ->
                    if (!shownErrorIds.contains(error.bookingId)) {
                        showSyncErrorSnackbar(error)
                        shownErrorIds.add(error.bookingId)
                    }
                }

                shownErrorIds.retainAll(errors.map { it.bookingId }.toSet())
            }
        }
    }

    private fun showSyncErrorSnackbar(error: ru.kafpin.repositories.SyncResult.Error) {
        val message = when (error.errorType) {
            ru.kafpin.repositories.SyncErrorType.DUPLICATE_BOOKING ->
                "❌ Уже есть активная бронь на '${error.bookTitle}'"
            ru.kafpin.repositories.SyncErrorType.INSUFFICIENT_BOOKS ->
                "📚 Не хватает книг '${error.bookTitle}'"
            ru.kafpin.repositories.SyncErrorType.NETWORK_ERROR ->
                "🌐 Сетевая ошибка"
            ru.kafpin.repositories.SyncErrorType.SERVER_ERROR ->
                "⚠️ Ошибка сервера"
            ru.kafpin.repositories.SyncErrorType.AUTH_ERROR ->
                "🔐 Требуется авторизация"
        }

        val snackbar = Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        )

        snackbar.setAction("Понятно") {
            viewModel.clearSyncError(error.bookingId)
        }

        if (error.errorType == ru.kafpin.repositories.SyncErrorType.DUPLICATE_BOOKING ||
            error.errorType == ru.kafpin.repositories.SyncErrorType.INSUFFICIENT_BOOKS) {

            snackbar.addCallback(object : Snackbar.Callback() {
                override fun onShown(sb: Snackbar?) {
                    // Автоматически удаляем ошибку через 10 секунд
                    lifecycleScope.launch {
                        delay(10000)
                        if (sb?.isShown == true) {
                            viewModel.clearSyncError(error.bookingId)
                        }
                    }
                }
            })
        }

        snackbar.show()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "SwipeRefresh triggered")
            lifecycleScope.launch {
                try {
                    if (viewModel.isLoading.value) {
                        Log.d(TAG, "Already loading, ignoring swipe")
                        stopSwipeRefresh()
                        return@launch
                    }

                    if (viewModel.canPerformAction()) {
                        shownErrorIds.clear()

                        viewModel.syncBookings()
                        launch {
                            delay(10000)
                            if (binding.swipeRefresh.isRefreshing) {
                                Log.w(TAG, "Force stopping swipe refresh after timeout")
                                stopSwipeRefresh()
                            }
                        }
                    } else {
                        stopSwipeRefresh()
                        showSnackbar("Дождитесь завершения синхронизации")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in swipe refresh", e)
                    stopSwipeRefresh()
                    showSnackbar("Ошибка: ${e.message}")
                }
            }
        }

        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }
    private fun stopSwipeRefresh() {
        if (binding.swipeRefresh.isRefreshing) {
            Log.d(TAG, "Stopping swipe refresh")
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showDeleteDialog(booking: ru.kafpin.data.models.BookingWithDetails) {
        AlertDialog.Builder(this)
            .setTitle("Удаление брони")
            .setMessage("Удалить бронь ${booking.displayId} на книгу \"${booking.booking.bookTitle}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteBooking(booking)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteBooking(booking: ru.kafpin.data.models.BookingWithDetails) {
        if (!viewModel.canPerformAction()) {
            showSnackbar("Дождитесь завершения синхронизации")
            return
        }

        lifecycleScope.launch {
            val success = viewModel.deleteBooking(booking.booking.localId)
            if (success) {
                showSnackbar("Бронь удалена")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bookings, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

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

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean = false

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.setSearchQuery(newText)
                return true
            }
        })

        return true
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MyBookingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}