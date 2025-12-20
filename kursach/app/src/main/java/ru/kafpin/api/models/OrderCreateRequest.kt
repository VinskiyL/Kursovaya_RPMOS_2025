package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderCreateRequest(
    @SerializedName("title") val title: String,
    @SerializedName("authorSurname") val authorSurname: String,
    @SerializedName("authorName") val authorName: String?,
    @SerializedName("authorPatronymic") val authorPatronymic: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("datePublication") val datePublication: String?
) : Parcelable