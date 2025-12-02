package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Author(
    @SerializedName("id")
    val id: Long,

    @SerializedName("authorSurname")
    val authorSurname: String,

    @SerializedName("authorName")
    val authorName: String?,

    @SerializedName("authorPatronymic")
    val authorPatronymic: String?
) : Parcelable