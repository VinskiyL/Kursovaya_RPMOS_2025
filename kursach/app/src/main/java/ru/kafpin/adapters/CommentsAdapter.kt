package ru.kafpin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.kafpin.api.models.CommentResponse
import ru.kafpin.databinding.ItemCommentBinding
import ru.kafpin.viewmodels.CommentsViewModel
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CommentsAdapter(
    private val viewModel: CommentsViewModel,
    private val onCommentClick: (CommentResponse) -> Unit
) : ListAdapter<CommentResponse, CommentsAdapter.ViewHolder>(CommentDiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: CommentResponse) {
            with(binding) {
                tvAuthor.text = comment.login ?: "Пользователь"
                tvComment.text = comment.comment

                try {
                    val date = LocalDateTime.parse(
                        comment.createdAt,
                        DateTimeFormatter.ISO_DATE_TIME
                    )
                    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    tvDate.text = date.format(formatter)
                } catch (e: Exception) {
                    tvDate.text = comment.createdAt
                }

                val isMyComment = viewModel.isMyComment(comment)
                actionsLayout.visibility = if (isMyComment) View.VISIBLE else View.GONE

                btnEdit.setOnClickListener {
                    onCommentClick(comment)
                }

                btnDelete.setOnClickListener {
                    showDeleteDialog(comment)
                }
            }
        }

        private fun showDeleteDialog(comment: CommentResponse) {
            val context = binding.root.context
            AlertDialog.Builder(context)
                .setTitle("Удаление комментария")
                .setMessage("Удалить комментарий?")
                .setPositiveButton("Удалить") { _, _ ->
                    viewModel.deleteComment(comment.id) {
                        Snackbar.make(
                            binding.root,
                            "Комментарий удалён",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCommentBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CommentDiffCallback : DiffUtil.ItemCallback<CommentResponse>() {
    override fun areItemsTheSame(oldItem: CommentResponse, newItem: CommentResponse): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CommentResponse, newItem: CommentResponse): Boolean {
        return oldItem == newItem
    }
}