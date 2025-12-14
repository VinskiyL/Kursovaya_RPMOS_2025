package ru.kafpin.api.models

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String,    // ← СЕРВЕР ДОЛЖЕН ВОЗВРАЩАТЬ!

    @SerializedName("tokenType")
    val tokenType: String = "Bearer",

    @SerializedName("user")
    val user: UserResponse
)