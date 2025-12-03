package ru.kafpin.activities

import ru.kafpin.R

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB
    protected lateinit var toolbar: Toolbar

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
        setSupportActionBar(toolbar)
    }

    protected fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    protected fun enableBackButton(enable: Boolean = true) {
        supportActionBar?.setDisplayHomeAsUpEnabled(enable)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        setupFullScreen()
    }
}