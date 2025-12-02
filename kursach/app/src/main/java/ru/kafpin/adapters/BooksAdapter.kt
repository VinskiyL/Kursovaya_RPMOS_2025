package ru.kafpin.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.kafpin.R
import ru.kafpin.data.models.BookWithDetails
import ru.kafpin.databinding.ItemBookBinding
import ru.kafpin.utils.loadImage

class BooksAdapter(
    private val onItemClick: (BookWithDetails) -> Unit
) : ListAdapter<BookWithDetails, BooksAdapter.BookViewHolder>(BookWithDetailsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(
        private val binding: ItemBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemClick(getItem(adapterPosition))
            }
            binding.detailsButton.visibility = View.GONE
        }

        fun bind(bookWithDetails: BookWithDetails) {
            Log.d("BooksAdapter", "=== BINDING BOOK ===")
            Log.d("BooksAdapter", "Title: ${bookWithDetails.book.title}")
            Log.d("BooksAdapter", "Book ID: ${bookWithDetails.book.id}")

            // Авторы
            Log.d("BooksAdapter", "Authors list size: ${bookWithDetails.authors.size}")
            bookWithDetails.authors.forEachIndexed { index, author ->
                Log.d("BooksAdapter",
                    "  Author[$index]: " +
                            "id=${author.id}, " +
                            "authorSurname='${author.surname}', " +
                            "authorName='${author.name}', " +
                            "authorPatronymic='${author.patronymic}'"
                )
            }
            Log.d("BooksAdapter", "authorsFormatted: '${bookWithDetails.authorsFormatted}'")

            // Жанры
            Log.d("BooksAdapter", "Genres list size: ${bookWithDetails.genres.size}")
            bookWithDetails.genres.forEachIndexed { index, genre ->
                Log.d("BooksAdapter", "  Genre[$index]: id=${genre.id}, name='${genre.name}'")
            }
            Log.d("BooksAdapter", "genresFormatted: '${bookWithDetails.genresFormatted}'")

            // Проверим Book поля
            Log.d("BooksAdapter", "Book.cover: '${bookWithDetails.book.cover}'")
            Log.d("BooksAdapter", "Book.quantityRemaining: ${bookWithDetails.book.quantityRemaining}")
            binding.apply {
                bookTitle.text = bookWithDetails.book.title

                // Авторы
                bookAuthor.text = bookWithDetails.authorsFormatted.ifEmpty { "Автор не указан" }

                // Жанры
                bookGenre.text = bookWithDetails.genresFormatted.ifEmpty { "Жанр не указан" }
                bookGenre.visibility = View.VISIBLE

                // Статус
                bookStatus.text = if (bookWithDetails.book.quantityRemaining > 0) {
                    "✅ В наличии"
                } else {
                    "❌ Нет в наличии"
                }

                // Обложка
                bookCover.loadImage(
                    bookWithDetails.book.cover,
                    placeholder = R.drawable.ic_book_placeholder
                )

                // Скрытые поля
                bookYear.visibility = View.GONE
                bookVolume.visibility = View.GONE
                bookIndex.visibility = View.GONE
                bookPlace.visibility = View.GONE
            }
        }
    }

    class BookWithDetailsDiffCallback : DiffUtil.ItemCallback<BookWithDetails>() {
        override fun areItemsTheSame(oldItem: BookWithDetails, newItem: BookWithDetails): Boolean {
            return oldItem.book.id == newItem.book.id
        }

        override fun areContentsTheSame(oldItem: BookWithDetails, newItem: BookWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}