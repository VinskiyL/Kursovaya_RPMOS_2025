package ru.kafpin.data.models

data class BookingWithDetails(
    val booking: BookingEntity
) {
    val formattedDates: String
        get() = "с ${booking.dateIssue} по ${booking.dateReturn}"

    val statusText: String
        get() = when (booking.status) {
            BookingStatus.PENDING -> "Ожидает подтверждения"
            BookingStatus.CONFIRMED -> "Подтверждено"
            BookingStatus.ISSUED -> "Выдано"
            BookingStatus.RETURNED -> "Возвращено"
        }

    val displayId: String
        get() = booking.serverId?.let { "$it" } ?: "Локальная ${booking.localId}"

    val canEdit: Boolean
        get() = booking.status in listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)

    val canDelete: Boolean
        get() = booking.status in listOf(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.RETURNED
        )

    val isOverdue: Boolean
        get() {
            return if (booking.status == BookingStatus.ISSUED) {
                try {
                    val returnDate = java.time.LocalDate.parse(booking.dateReturn)
                    returnDate.isBefore(java.time.LocalDate.now())
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
        }

    val daysRemaining: Int?
        get() {
            return if (booking.status == BookingStatus.ISSUED) {
                try {
                    val returnDate = java.time.LocalDate.parse(booking.dateReturn)
                    val today = java.time.LocalDate.now()
                    if (returnDate.isBefore(today)) {
                        null
                    } else {
                        java.time.temporal.ChronoUnit.DAYS.between(today, returnDate).toInt()
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
}