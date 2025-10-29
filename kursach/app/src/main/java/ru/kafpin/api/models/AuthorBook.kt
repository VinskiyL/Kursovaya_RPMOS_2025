package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AuthorBook(
    @SerializedName("id")
    val id: Long,

    @SerializedName("author")
    val author: Author?
) : Parcelable