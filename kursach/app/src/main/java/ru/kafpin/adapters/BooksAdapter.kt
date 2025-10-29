package ru.kafpin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.kafpin.R
import ru.kafpin.api.models.Book
import ru.kafpin.databinding.ItemBookBinding
import ru.kafpin.utils.loadImage

class BooksAdapter(
    private val onItemClick: (Book) -> Unit,
    private val onDetailsClick: (Book) -> Unit
) : ListAdapter<Book, BooksAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)
    }

    inner class BookViewHolder(
        private val binding: ItemBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val book = getItem(adapterPosition)
                onItemClick(book)
            }

            binding.detailsButton.setOnClickListener {
                val book = getItem(adapterPosition)
                onDetailsClick(book)
            }
        }

        fun bind(book: Book) {
            binding.apply {
                bookTitle.text = book.title
                bookYear.text = book.datePublication
                bookVolume.text = "Том: ${book.volume}"
                bookIndex.text = "Индекс: ${book.index}"
                bookPlace.text = "Издательство: ${book.placePublication}"
                bookGenre.text = book.genreDisplay

                bookAuthor.text = book.authorDisplay

                // Статус доступности
                if (book.isAvailable) {
                    bookStatus.text = "✅ Доступно: ${book.quantityRemaining}/${book.quantityTotal}"
                } else {
                    bookStatus.text = "❌ Нет в наличии"
                }

                // Загрузка обложки
                bookCover.loadImage(
                    book.cover,
                    placeholder = R.drawable.ic_book_placeholder
                )
            }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
}