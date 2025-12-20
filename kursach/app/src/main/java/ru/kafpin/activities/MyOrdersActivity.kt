package ru.kafpin.activities

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kafpin.MyApplication
import ru.kafpin.R
import ru.kafpin.activities.BaseActivity
import ru.kafpin.databinding.ActivityMyOrdersBinding
import ru.kafpin.ui.orders.CreateOrderDialog
import ru.kafpin.ui.orders.OrdersAdapter
import ru.kafpin.utils.NetworkMonitor
import ru.kafpin.viewmodels.OrderViewModel
import ru.kafpin.viewmodels.OrderViewModelFactory

class MyOrdersActivity : BaseActivity<ActivityMyOrdersBinding>() {

    private val TAG = "MyOrdersActivity"

    private val viewModel: OrderViewModel by viewModels {
        OrderViewModelFactory.getInstance(this)
    }

    private var wasOffline = false
    private lateinit var adapter: OrdersAdapter
    private val shownErrorIds = mutableSetOf<Long>()

    override fun inflateBinding(): ActivityMyOrdersBinding {
        return ActivityMyOrdersBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbarButtons(
            showBackButton = true,
            showLogoutButton = false
        )
        setToolbarTitle("Мои заказы")

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
        setupFAB()

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
        val baseTitle = "Заказы "
        val networkStatus = if (isOnline) " ✅ on" else " 🔴 off"
        setToolbarTitle("$baseTitle$networkStatus")
    }

    private fun setupRecyclerView() {
        adapter = OrdersAdapter(
            onItemClick = { order ->
                OrdersDetailActivity.start(this@MyOrdersActivity, order.order.localId)
            },
            onDeleteClick = { order ->
                showDeleteDialog(order)
            },
            networkMonitor.isOnline.value
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyOrdersActivity)
            adapter = this@MyOrdersActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.orders.collect { orders ->
                adapter.submitList(orders)
                binding.emptyView.isVisible = orders.isEmpty()
                binding.recyclerView.isVisible = orders.isNotEmpty()
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
                "❌ Ошибка при создании заказа '${error.bookTitle}'"
            ru.kafpin.repositories.SyncErrorType.INSUFFICIENT_BOOKS ->
                "📚 Ошибка: '${error.bookTitle}'"
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

                        viewModel.syncOrders()
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

    private fun setupFAB() {
        binding.fabCreateOrder.setOnClickListener {
            showCreateOrderDialog()
        }
    }

    private fun showCreateOrderDialog() {
        lifecycleScope.launch {
            val canCreate = viewModel.canCreateNewOrder()
            if (!canCreate) {
                showSnackbar("Нельзя создать более 5 заказов. Удалите старые.")
                return@launch
            }

            val dialog = CreateOrderDialog.newInstance()
            dialog.setOnOrderCreatedListener { orderData ->
                lifecycleScope.launch {
                    val result = viewModel.createOrder(
                        title = orderData.title,
                        authorSurname = orderData.authorSurname,
                        authorName = orderData.authorName,
                        authorPatronymic = orderData.authorPatronymic,
                        quantity = orderData.quantity,
                        year = orderData.year
                    )

                    if (result.isSuccess) {
                        showSnackbar("Заказ создан")
                    } else {
                        showSnackbar("Ошибка: ${result.exceptionOrNull()?.message}")
                    }
                }
            }
            dialog.show(supportFragmentManager, "CreateOrderDialog")
        }
    }

    private fun showDeleteDialog(order: ru.kafpin.data.models.OrderWithDetails) {
        AlertDialog.Builder(this)
            .setTitle("Удаление заказа")
            .setMessage("Удалить заказ ${order.displayId} на книгу \"${order.order.title}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteOrder(order)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteOrder(order: ru.kafpin.data.models.OrderWithDetails) {
        if (!viewModel.canPerformAction()) {
            showSnackbar("Дождитесь завершения синхронизации")
            return
        }

        lifecycleScope.launch {
            val success = viewModel.deleteOrder(order.order.localId)
            if (success) {
                showSnackbar("Заказ удалён")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Поиск не нужен по ТЗ (максимум 5 заказов)
        // Оставляем на будущее если понадобится
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
            val intent = Intent(context, MyOrdersActivity::class.java)
            context.startActivity(intent)
        }
    }
}