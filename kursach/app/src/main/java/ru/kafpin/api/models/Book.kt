package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    @SerializedName("id") val id: Long,
    @SerializedName("index") val index: String,
    @SerializedName("authorsMark") val authorsMark: String,
    @SerializedName("title") val title: String,
    @SerializedName("placePublication") val placePublication: String,
    @SerializedName("informationPublication") val informationPublication: String,
    @SerializedName("volume") val volume: Int,
    @SerializedName("quantityTotal") val quantityTotal: Int,
    @SerializedName("quantityRemaining") val quantityRemaining: Int,
    @SerializedName("cover") val cover: String?,
    @SerializedName("datePublication") val datePublication: String,
) : Parcelable