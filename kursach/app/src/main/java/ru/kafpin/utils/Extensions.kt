package ru.kafpin.utils

import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes
import coil.load

/**
 * Загрузка изображения с помощью Coil
 */
fun ImageView.loadImage(
    url: String?,
    placeholder: Int = android.R.drawable.ic_menu_report_image
) {
    load(url) {
        crossfade(true)
        placeholder(placeholder)
        error(placeholder)
        fallback(placeholder)
    }
}

/**
 * Показать Toast сообщение
 */
fun android.content.Context.showToast(
    message: String,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Показать Toast из ресурсов
 */
fun android.content.Context.showToast(
    @StringRes messageRes: Int,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(this, messageRes, duration).show()
}