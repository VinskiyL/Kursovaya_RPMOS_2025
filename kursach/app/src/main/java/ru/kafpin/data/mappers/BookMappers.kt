package ru.kafpin.data.mappers

import ru.kafpin.api.models.Book
import ru.kafpin.data.models.BookEntity

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
        lastSynced = System.currentTimeMillis()
    )
}
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

fun List<Book>.toBookEntities(): List<BookEntity> {
    return map { it.toBookEntity() }
}

fun List<BookEntity>.toBooks(): List<Book> {
    return map { it.toBook() }
}