package ru.kafpin.data.mappers

import ru.kafpin.api.models.Genre
import ru.kafpin.data.models.GenreEntity

fun Genre.toGenreEntity(): GenreEntity {
    return GenreEntity(
        id = id,
        name = name
    )
}

fun GenreEntity.toGenre(): Genre {
    return Genre(
        id = id,
        name = name
    )
}

// Списки
fun List<Genre>.toGenreEntities(): List<GenreEntity> = map { it.toGenreEntity() }
fun List<GenreEntity>.toGenres(): List<Genre> = map { it.toGenre() }