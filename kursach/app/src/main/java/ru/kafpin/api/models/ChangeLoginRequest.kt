package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChangeLoginRequest(
    @SerializedName("password") val password: String,
    @SerializedName("newLogin") val newLogin: String
) : Parcelable