package ru.kafpin.activities

import ru.kafpin.R
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch
import ru.kafpin.MyApplication
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB
    protected lateinit var toolbar: Toolbar

    protected val networkMonitor get() = (application as MyApplication).networkMonitor

    private lateinit var btnBack: ImageButton
    private lateinit var btnLogout: ImageButton

    abstract fun inflateBinding(): VB
    abstract fun setupUI()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Настройка полноэкранного режима
        setupFullScreen()

        // Устанавливаем базовый layout с тулбаром
        setContentView(R.layout.activity_base)

        // Инициализация тулбара
        setupToolbar()

        // Инициализация ViewBinding наследника
        binding = inflateBinding()
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        contentContainer.addView(binding.root)

        // Настройка контента
        setupUI()
    }

    private fun setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        btnBack = findViewById(R.id.btnBack)
        btnLogout = findViewById(R.id.btnLogout)

        setSupportActionBar(toolbar)
    }

    protected fun setupToolbarButtons(
        showBackButton: Boolean = false,
        showLogoutButton: Boolean = true
    ) {
        if (showBackButton) {
            btnBack.visibility = android.view.View.VISIBLE
            btnBack.setOnClickListener { onBackPressed() }
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        } else {
            btnBack.visibility = android.view.View.GONE
        }

        if (showLogoutButton) {
            btnLogout.visibility = android.view.View.VISIBLE
            btnLogout.setOnClickListener { performLogout() }
        } else {
            btnLogout.visibility = android.view.View.GONE
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                val database = LibraryDatabase.getInstance(this@BaseActivity)
                val authRepository = RepositoryProvider.getAuthRepository(database, this@BaseActivity)
                authRepository.logout()

                val intent = Intent(this@BaseActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@BaseActivity,
                    "Ошибка при выходе: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    protected fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun onResume() {
        super.onResume()
        setupFullScreen()
    }
}