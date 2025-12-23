package ru.kafpin.comment.tests

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class CommentDateFormatterTest {

    @Test
    fun форматирование_ISO_даты_в_читаемый_формат() {
        // Arrange
        val testCases = listOf(
            "2024-12-23T14:30:45" to "23.12.2024 14:30",
            "2024-01-01T00:00:00" to "01.01.2024 00:00",
            "2023-12-31T23:59:59" to "31.12.2023 23:59",
            "2024-02-29T12:15:30" to "29.02.2024 12:15" // високосный год
        )

        testCases.forEach { (isoDate, expectedFormatted) ->
            // Act
            val formatted = formatCommentDate(isoDate)

            // Assert
            assertEquals("Дата '$isoDate' должна форматироваться как '$expectedFormatted'",
                expectedFormatted, formatted)
        }
    }
    private fun formatCommentDate(isoDate: String): String {
        return try {
            val date = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME)
            date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        } catch (e: Exception) {
            isoDate // fallback
        }
    }
}