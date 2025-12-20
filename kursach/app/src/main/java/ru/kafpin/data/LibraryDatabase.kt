package ru.kafpin.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import ru.kafpin.data.dao.*
import ru.kafpin.data.models.*

@Database(
    entities = [
        BookEntity::class,
        AuthorEntity::class,
        GenreEntity::class,
        BookAuthorCrossRef::class,
        BookGenreCrossRef::class,
        UserEntity::class,
        AuthSessionEntity::class,
        BookingEntity::class,
        OrderEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun authorDao(): AuthorDao
    abstract fun genreDao(): GenreDao
    abstract fun bookAuthorDao(): BookAuthorDao
    abstract fun bookGenreDao(): BookGenreDao
    abstract fun userDao(): UserDao
    abstract fun authDao(): AuthDao
    abstract fun bookingDao(): BookingDao

    abstract fun orderDao(): OrderDao

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