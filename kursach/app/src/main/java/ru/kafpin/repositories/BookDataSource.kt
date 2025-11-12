package ru.kafpin.repositories

import ru.kafpin.api.models.Book

interface BookDataSource {
    suspend fun getAllBooks(): List<Book>
}