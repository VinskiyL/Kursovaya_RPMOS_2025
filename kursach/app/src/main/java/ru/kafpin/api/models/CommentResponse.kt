package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("comment") val comment: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("login") val login: String? = null
) : Parcelable

@Parcelize
data class CommentUserResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("login") val login: String,
    @SerializedName("displayName") val displayName: String?
) : Parcelable