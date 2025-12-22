package ru.kafpin.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val TAG = "MainActivity"

    override fun inflateBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        Log.d(TAG, "setupUI()")

        // Настройка тулбара
        setToolbarTitle("Главная")

        setupToolbarButtons(
            showBackButton = false,
            showLogoutButton = true
        )

        updateToolbarWithUserAndNetwork()

        observeNetworkStatus()
    }

    private fun updateToolbarWithUserAndNetwork() {
        lifecycleScope.launch {
            val database = LibraryDatabase.getInstance(this@MainActivity)

            val authRepository = RepositoryProvider.getAuthRepository(database, this@MainActivity)

            val currentUser = authRepository.getCurrentUser()
            val isOnline = networkMonitor.isOnline.value
            val networkStatus = if (isOnline) "✅ on" else "🔴 off"

            val title = if (currentUser != null) {
                val userName = currentUser.displayName ?: currentUser.login
                "$userName • $networkStatus"
            } else {
                "Гость • $networkStatus"
            }

            setToolbarTitle(title)
        }
    }

    private fun observeNetworkStatus() {
        lifecycleScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                updateToolbarWithUserAndNetwork()
                if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                    val message = if (isOnline) "✅ Сеть восстановлена" else "🔴 Нет подключения"
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun showSettings(view: View) {
        Log.d(TAG, "showBookList()")
        SettingsActivity.start(this)
    }

    fun showProfile(view: View) {
        Log.d(TAG, "showProfile()")
        ProfileActivity.start(this)
    }

    fun showComments(view: View) {
        Toast.makeText(this, "Комментарии в разработке", Toast.LENGTH_SHORT).show()
    }

    fun showOrderList(view: View) {
        Log.d(TAG, "showOrderList()")
        MyOrdersActivity.start(this)
    }

    fun showBookingList(view: View) {
        Log.d(TAG, "showBookingList()")
        MyBookingsActivity.start(this)
    }

    fun showBookList(view: View) {
        Log.d(TAG, "showBookList()")
        BooksActivity.start(this)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}