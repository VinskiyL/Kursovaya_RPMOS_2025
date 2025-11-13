package ru.kafpin.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Умный мониторинг для точки доступа - проверяет доступность порта сервера
 */
class NetworkMonitor(context: Context) {

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    // IP и порт твоего сервера
    private val serverIp = "192.168.43.210"
    private val serverPort = 8080

    // Флаг для остановки проверки
    private var isChecking = true
    private var checkThread: Thread? = null

    fun start() {
        println("🌐 NetworkMonitor: HOTSPOT MODE - checking server port $serverPort")

        stop()
        isChecking = true

        // Первая проверка сразу
        checkServerAvailability()

        // Запускаем фоновую проверку каждые 10 секунд
        Thread {
            while (isChecking) {
                Thread.sleep(10000) // Ждём 10 секунд
                if (isChecking) {
                    checkServerAvailability()
                }
            }
            println("🌐 NetworkMonitor: Stopped checking")
        }.start()
    }

    private fun checkServerAvailability() {
        Thread {
            try {
                // Пытаемся подключиться к порту сервера
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(serverIp, serverPort), 1000)
                    // Если подключились успешно - сервер доступен
                    if (!_isOnline.value) {
                        _isOnline.value = true
                        println("🌐 NetworkMonitor: ✅ Server port $serverPort is OPEN - ONLINE")
                    }
                }
            } catch (e: Exception) {
                // Не смогли подключиться - сервер недоступен
                if (_isOnline.value) {
                    _isOnline.value = false
                    println("🌐 NetworkMonitor: ❌ Server port $serverPort is CLOSED - OFFLINE: ${e.message}")
                }
            }
        }.start()
    }

    fun stop() {
        isChecking = false
        checkThread?.interrupt()
        checkThread = null
        println("🌐 NetworkMonitor: Stopping network monitoring")
    }
}