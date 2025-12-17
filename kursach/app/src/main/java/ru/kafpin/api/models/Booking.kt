package ru.kafpin.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class BookingResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("dateIssue") val dateIssue: String,
    @SerializedName("dateReturn") val dateReturn: String,
    @SerializedName("issued") val issued: Boolean,
    @SerializedName("returned") val returned: Boolean,
    @SerializedName("bookId") val bookId: Long,
    @SerializedName("bookTitle") val bookTitle: String,
    @SerializedName("readerId") val readerId: Long,
    @SerializedName("readerFullName") val readerFullName: String
) : Parcelable {
    fun toStatus(): ru.kafpin.data.models.BookingStatus {
        return when {
            returned -> ru.kafpin.data.models.BookingStatus.RETURNED
            issued -> ru.kafpin.data.models.BookingStatus.ISSUED
            else -> ru.kafpin.data.models.BookingStatus.CONFIRMED
        }
    }
}

@Parcelize
data class BookingCreateRequest(
    @SerializedName("bookId") val bookId: Long,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("dateIssue") val dateIssue: String,
    @SerializedName("dateReturn") val dateReturn: String
) : Parcelable

@Parcelize
data class BookingUpdateRequest(
    @SerializedName("quantity") val quantity: Int
) : Parcelable