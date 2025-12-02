package ru.kafpin.activities

import ru.kafpin.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        // Настройка тулбара
        setToolbarTitle("Главная")

        // Простой текст в центре
        binding.welcomeText.text = "Добро пожаловать в мое приложение!"
        binding.subtitleText.text = "Курсовой проект"

        // Можно добавить клик-обработчики если нужно
        binding.clickableText.setOnClickListener {
            binding.welcomeText.text = "Текст изменен по клику!"
        }
    }
}

/*class MainActivity : AppCompatActivity() {
    private lateinit var syncService: SmartSyncService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        syncService = SmartSyncService(this)

        // Запускаем фоновую синхронизацию при старте
        lifecycleScope.launch {
            syncService.syncIfNeeded()
        }
    }
}*/