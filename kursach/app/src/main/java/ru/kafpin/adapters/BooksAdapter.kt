package ru.kafpin.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.kafpin.R
import ru.kafpin.api.models.Book
import ru.kafpin.databinding.ItemBookBinding
import ru.kafpin.utils.loadImage

class BooksAdapter(
    private val onItemClick: (Book) -> Unit
) : ListAdapter<Book, BooksAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        Log.d("BooksAdapter", "onCreateViewHolder()")
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        Log.d("BooksAdapter", "onBindViewHolder() position: $position")
        val book = getItem(position)
        Log.d("BooksAdapter", "Book at $position: ${book.title} (ID: ${book.id})")
        holder.bind(book)
    }

    override fun submitList(list: List<Book>?) {
        Log.d("BooksAdapter", "submitList() called")
        Log.d("BooksAdapter", "List size: ${list?.size ?: 0}")

        if (list != null && list.isNotEmpty()) {
            Log.d("BooksAdapter", "First 3 books:")
            list.take(3).forEachIndexed { index, book ->
                Log.d("BooksAdapter", "  $index: ${book.title} (ID: ${book.id})")
            }
        }

        super.submitList(list)
    }

    inner class BookViewHolder(
        private val binding: ItemBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            Log.d("BooksAdapter", "BookViewHolder init()")
            binding.root.setOnClickListener {
                val book = getItem(adapterPosition)
                Log.d("BooksAdapter", "Book clicked: ${book.title} (ID: ${book.id})")
                onItemClick(book)
            }

            binding.detailsButton.visibility = View.GONE
        }

        fun bind(book: Book) {
            Log.d("BooksAdapter", "bind() book: ${book.title}")

            binding.apply {
                bookTitle.text = book.title
                bookAuthor.text = book.authorsMark

                bookGenre.visibility = View.GONE
                bookYear.visibility = View.GONE
                bookVolume.visibility = View.GONE
                bookIndex.visibility = View.GONE
                bookPlace.visibility = View.GONE

                val isAvailable = book.quantityRemaining > 0
                bookStatus.text = if (isAvailable) {
                    "✅ В наличии"
                } else {
                    "❌ Нет в наличии"
                }

                Log.d("BooksAdapter", "Loading cover: ${book.cover}")
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