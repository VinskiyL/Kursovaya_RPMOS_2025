package ru.kafpin.repositories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.kafpin.api.ApiClient
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.mappers.toGenre
import ru.kafpin.data.mappers.toGenreEntity
import ru.kafpin.utils.NetworkMonitor

class GenreRepository(context: Context) {
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

    suspend fun getLocalGenres(): List<ru.kafpin.api.models.Genre> {
        return try {
            database.genreDao().getAllGenres().map { it.toGenre() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getGenreById(id: Long): ru.kafpin.api.models.Genre? {
        return try {
            database.genreDao().getGenreById(id)?.toGenre()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getGenresByIds(ids: List<Long>): List<ru.kafpin.api.models.Genre> {
        return try {
            database.genreDao().getGenresByIds(ids).map { it.toGenre() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveGenresToLocal(genres: List<ru.kafpin.api.models.Genre>) {
        try {
            database.genreDao().insertGenres(genres.map { it.toGenreEntity() })
        } catch (e: Exception) {}
    }

    private suspend fun getRemoteGenres(): List<ru.kafpin.api.models.Genre> {
        val response = apiService.getAllGenres()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception("Ошибка сервера: ${response.code()}")
        }
    }

    suspend fun syncGenres(): Boolean {
        if (!networkMonitor.isOnline.value) return false

        return try {
            val remoteGenres = getRemoteGenres()
            saveGenresToLocal(remoteGenres)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun searchGenres(query: String): List<ru.kafpin.api.models.Genre> {
        return try {
            database.genreDao().searchGenres(query).map { it.toGenre() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}