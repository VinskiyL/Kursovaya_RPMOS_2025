package ru.kafpin.api.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("login")
    val login: String,

    @SerializedName("password")
    val password: String
)