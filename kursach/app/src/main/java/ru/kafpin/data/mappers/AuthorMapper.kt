package ru.kafpin.data.mappers

import ru.kafpin.api.models.Author
import ru.kafpin.data.models.AuthorEntity

fun Author.toAuthorEntity(): AuthorEntity {
    return AuthorEntity(
        id = id,
        surname = authorSurname,
        name = authorName,
        patronymic = authorPatronymic
    )
}

fun AuthorEntity.toAuthor(): Author {
    return Author(
        id = id,
        authorSurname = surname ?: "",
        authorName = name,
        authorPatronymic = patronymic
    )
}

// Списки
fun List<Author>.toAuthorEntities(): List<AuthorEntity> = map { it.toAuthorEntity() }
fun List<AuthorEntity>.toAuthors(): List<Author> = map { it.toAuthor() }