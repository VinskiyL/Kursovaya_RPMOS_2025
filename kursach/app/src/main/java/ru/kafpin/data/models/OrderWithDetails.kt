package ru.kafpin.data.models

data class OrderWithDetails(
    val order: OrderEntity,
    val isOnline: Boolean = false
) {

    val authorFull: String
        get() = buildString {
            append(order.authorSurname)
            order.authorName?.let { append(" $it") }
            order.authorPatronymic?.let { append(" $it") }
        }

    val statusText: String
        get() = when (order.status) {
            OrderStatus.LOCAL_PENDING -> "Ожидает отправки"
            OrderStatus.SERVER_PENDING -> "Ждёт подтверждения"
            OrderStatus.CONFIRMED -> "Подтверждён"
        }

    val displayId: String
        get() = order.serverId?.toString() ?: "Локальный ${order.localId}"

    fun canEdit(isOnline: Boolean): Boolean {
        return when (order.status) {
            OrderStatus.LOCAL_PENDING -> true
            OrderStatus.SERVER_PENDING -> isOnline
            OrderStatus.CONFIRMED -> false
        }
    }

    fun canDelete(isOnline: Boolean): Boolean {
        return when (order.status) {
            OrderStatus.LOCAL_PENDING -> true
            OrderStatus.SERVER_PENDING -> isOnline
            OrderStatus.CONFIRMED -> false
        }
    }

    val formattedDate: String
        get() = try {
            val date = java.time.Instant.ofEpochMilli(order.createdAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        } catch (e: Exception) {
            ""
        }
}