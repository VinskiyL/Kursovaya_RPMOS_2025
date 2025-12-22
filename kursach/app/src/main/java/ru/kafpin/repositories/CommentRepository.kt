package ru.kafpin.repositories

import android.content.Context
import ru.kafpin.api.ApiClient
import ru.kafpin.api.models.CommentCreateRequest
import ru.kafpin.api.models.CommentResponse
import ru.kafpin.api.models.CommentUpdateRequest

class CommentRepository(
    private val authRepository: AuthRepository,
    private val context: Context
) {
    private val apiService = ApiClient.apiService
    private val TAG = "CommentRepository"

    suspend fun getComments(): Result<List<CommentResponse>> {
        return try {
            val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            val response = apiService.getAllComments(token)

            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Ошибка загрузки: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createComment(text: String): Result<CommentResponse> {
        return try {
            val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            val request = CommentCreateRequest(text)
            val response = apiService.createComment(request, token)

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Ошибка создания: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateComment(id: Long, text: String): Result<CommentResponse> {
        return try {
            val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            val request = CommentUpdateRequest(text)
            val response = apiService.updateComment(id, request, token)

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Ошибка обновления: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(id: Long): Result<Unit> {
        return try {
            val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            val response = apiService.deleteComment(id, token)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка удаления: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}