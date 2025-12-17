package ru.kafpin.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import ru.kafpin.databinding.ActivitySettingsBinding
import ru.kafpin.utils.NotificationHelper

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

        fun start(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }

        fun isNotificationsEnabled(context: Context): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        }
    }

    override fun inflateBinding(): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbarButtons(
            showBackButton = true,
            showLogoutButton = true
        )
        setToolbarTitle("Настройки")

        loadSettings()

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(isChecked)

            if (isChecked) {
                NotificationHelper.showStatusChangeNotification(
                    context = this,
                    bookingId = 999,
                    bookTitle = "Тестовая книга",
                    oldStatus = "PENDING",
                    newStatus = "CONFIRMED"
                )
            }
        }
    }

    private fun loadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        binding.switchNotifications.isChecked = notificationsEnabled
    }

    private fun saveSetting(enabled: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit {
            putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
        }
    }
}