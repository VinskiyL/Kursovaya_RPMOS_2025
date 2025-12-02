package ru.kafpin.data.mappers

import ru.kafpin.api.models.Book
import ru.kafpin.data.models.BookEntity

/**
 * Конвертируем Book (API модель) в BookEntity (БД модель)
 */
fun Book.toBookEntity(): BookEntity {
    return BookEntity(
        id = id,
        index = index,
        authorsMark = authorsMark,
        title = title,
        placePublication = placePublication,
        informationPublication = informationPublication,
        volume = volume,
        quantityTotal = quantityTotal,
        quantityRemaining = quantityRemaining,
        cover = cover  ?: "",
        datePublication = datePublication,
        lastSynced = System.currentTimeMillis() // текущее время как метка синхронизации
    )
}

/**
 * Конвертируем BookEntity (БД модель) в Book (API модель)
 */
fun BookEntity.toBook(): Book {
    return Book(
        id = id,
        index = index,
        authorsMark = authorsMark,
        title = title,
        placePublication = placePublication,
        informationPublication = informationPublication,
        volume = volume,
        quantityTotal = quantityTotal,
        quantityRemaining = quantityRemaining,
        cover = cover ?: "",
        datePublication = datePublication
    )
}

/**
 * Конвертируем списки
 */
fun List<Book>.toBookEntities(): List<BookEntity> {
    return map { it.toBookEntity() }
}

fun List<BookEntity>.toBooks(): List<Book> {
    return map { it.toBook() }
}