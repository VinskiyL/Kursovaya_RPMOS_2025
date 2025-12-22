package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChangeEmailRequest(
    @SerializedName("password") val password: String,
    @SerializedName("newEmail") val newEmail: String
) : Parcelable