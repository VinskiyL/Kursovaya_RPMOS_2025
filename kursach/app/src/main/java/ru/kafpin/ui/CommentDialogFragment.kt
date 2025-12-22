package ru.kafpin.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.kafpin.databinding.DialogCommentBinding

class CommentDialogFragment : DialogFragment() {
    private lateinit var binding: DialogCommentBinding

    private var isEditMode = false
    private var commentId: Long = -1L
    private var initialText = ""

    private var onCommentCreated: ((String) -> Unit)? = null
    private var onCommentUpdated: ((Long, String) -> Unit)? = null

    companion object {
        private const val ARG_COMMENT_ID = "comment_id"
        private const val ARG_COMMENT_TEXT = "comment_text"

        fun newInstance(): CommentDialogFragment {
            return CommentDialogFragment()
        }

        fun editInstance(commentId: Long, text: String): CommentDialogFragment {
            return CommentDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_COMMENT_ID, commentId)
                    putString(ARG_COMMENT_TEXT, text)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            commentId = it.getLong(ARG_COMMENT_ID, -1L)
            if (commentId != -1L) {
                isEditMode = true
                initialText = it.getString(ARG_COMMENT_TEXT, "")
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCommentBinding.inflate(layoutInflater)

        if (isEditMode) {
            binding.etComment.setText(initialText)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(if (isEditMode) "Редактировать комментарий" else "Новый комментарий")
            .setPositiveButton(if (isEditMode) "Сохранить" else "Добавить", null)
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val text = binding.etComment.text.toString().trim()
                if (text.isEmpty()) {
                    binding.etComment.error = "Введите текст комментария"
                    return@setOnClickListener
                }

                if (text.length > 1000) {
                    binding.etComment.error = "Максимум 1000 символов"
                    return@setOnClickListener
                }

                if (isEditMode) {
                    onCommentUpdated?.invoke(commentId, text)
                } else {
                    onCommentCreated?.invoke(text)
                }
                dialog.dismiss()
            }
        }

        return dialog
    }

    fun setOnCommentCreatedListener(listener: (String) -> Unit) {
        onCommentCreated = listener
    }

    fun setOnCommentUpdatedListener(listener: (Long, String) -> Unit) {
        onCommentUpdated = listener
    }
}