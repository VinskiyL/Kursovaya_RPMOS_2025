package ru.kafpin.api.models

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("login")
    val login: String,

    @SerializedName("displayName")
    val displayName: String?,

    @SerializedName("isActive")
    val isActive: Boolean,

    @SerializedName("isAdmin")
    val isAdmin: Boolean
)