package ru.kafpin.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import ru.kafpin.data.dao.AuthorDao
import ru.kafpin.data.dao.BookDao
import ru.kafpin.data.dao.GenreDao
import ru.kafpin.data.dao.BookAuthorDao
import ru.kafpin.data.dao.BookGenreDao
import ru.kafpin.data.models.AuthorEntity
import ru.kafpin.data.models.BookEntity
import ru.kafpin.data.models.GenreEntity
import ru.kafpin.data.models.BookAuthorCrossRef
import ru.kafpin.data.models.BookGenreCrossRef

@Database(
    entities = [
        BookEntity::class,
        AuthorEntity::class,
        GenreEntity::class,
        BookAuthorCrossRef::class,
        BookGenreCrossRef::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun authorDao(): AuthorDao
    abstract fun genreDao(): GenreDao
    abstract fun bookAuthorDao(): BookAuthorDao
    abstract fun bookGenreDao(): BookGenreDao

    companion object {
        @Volatile
        private var INSTANCE: LibraryDatabase? = null

        fun getInstance(context: Context): LibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    "library.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}