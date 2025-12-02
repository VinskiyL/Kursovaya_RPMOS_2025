package ru.kafpin.repositories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.kafpin.api.ApiClient
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.mappers.toAuthor
import ru.kafpin.data.mappers.toAuthorEntity
import ru.kafpin.utils.NetworkMonitor

class AuthorRepository(context: Context) {
    private val TAG = "AuthorRepository"
    private val database = LibraryDatabase.getInstance(context)
    private val apiService = ApiClient.apiService
    private val networkMonitor: NetworkMonitor

    init {
        val appContext = context.applicationContext
        networkMonitor = if (appContext is ru.kafpin.MyApplication) {
            appContext.networkMonitor
        } else {
            NetworkMonitor(context).apply { start() }
        }
    }

    // ==================== ЛОКАЛЬНЫЕ ДАННЫЕ ====================

    suspend fun getLocalAuthors(): List<ru.kafpin.api.models.Author> {
        return try {
            database.authorDao().getAllAuthors().map { it.toAuthor() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAuthorById(id: Long): ru.kafpin.api.models.Author? {
        return try {
            database.authorDao().getAuthorById(id)?.toAuthor()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAuthorsByIds(ids: List<Long>): List<ru.kafpin.api.models.Author> {
        return try {
            database.authorDao().getAuthorsByIds(ids).map { it.toAuthor() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveAuthorsToLocal(authors: List<ru.kafpin.api.models.Author>) {
        try {
            database.authorDao().insertAuthors(authors.map { it.toAuthorEntity() })
        } catch (e: Exception) {
            // Логируем при необходимости
        }
    }

    // ==================== УДАЛЕННЫЕ ДАННЫЕ ====================

    private suspend fun getRemoteAuthors(): List<ru.kafpin.api.models.Author> {
        val response = apiService.getAllAuthors()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception("Ошибка сервера: ${response.code()}")
        }
    }

    // ==================== СИНХРОНИЗАЦИЯ ====================

    suspend fun syncAuthors(): Boolean {
        if (!networkMonitor.isOnline.value) return false

        return try {
            val remoteAuthors = getRemoteAuthors()
            saveAuthorsToLocal(remoteAuthors)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== ПОИСК В БД ====================

    suspend fun searchAuthors(query: String): List<ru.kafpin.api.models.Author> {
        return try {
            database.authorDao().searchAuthors(query).map { it.toAuthor() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}