package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentUpdateRequest(
    @SerializedName("comment") val comment: String
) : Parcelable