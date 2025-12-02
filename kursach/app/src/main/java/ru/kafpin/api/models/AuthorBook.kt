package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AuthorBook(
    val id: Long,
    val bookId: Long,
    val authorId: Long
) : Parcelable