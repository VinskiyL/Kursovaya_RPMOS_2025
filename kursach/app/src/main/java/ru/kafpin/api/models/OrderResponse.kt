package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import ru.kafpin.data.models.OrderStatus

@Parcelize
data class OrderResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("authorSurname") val authorSurname: String,
    @SerializedName("authorName") val authorName: String?,
    @SerializedName("authorPatronymic") val authorPatronymic: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("datePublication") val datePublication: String?,
    @SerializedName("confirmed") val confirmed: Boolean,
    @SerializedName("readerId") val readerId: Long
) : Parcelable {

    fun toStatus(): OrderStatus {
        return if (confirmed) OrderStatus.CONFIRMED else OrderStatus.SERVER_PENDING
    }
}