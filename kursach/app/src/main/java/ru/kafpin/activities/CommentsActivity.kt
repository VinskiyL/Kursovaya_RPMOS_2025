package ru.kafpin.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kafpin.R
import ru.kafpin.adapters.CommentsAdapter
import ru.kafpin.api.models.CommentResponse
import ru.kafpin.databinding.ActivityCommentsBinding
import ru.kafpin.ui.CommentDialogFragment
import ru.kafpin.viewmodels.CommentsViewModel
import ru.kafpin.viewmodels.CommentsViewModelFactory

class CommentsActivity : BaseActivity<ActivityCommentsBinding>() {
    private val TAG = "CommentsActivity"

    private val viewModel: CommentsViewModel by viewModels {
        CommentsViewModelFactory.getInstance(this)
    }

    private lateinit var adapter: CommentsAdapter

    override fun inflateBinding(): ActivityCommentsBinding {
        return ActivityCommentsBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbarButtons(
            showBackButton = true,
            showLogoutButton = false
        )
        setToolbarTitle("Комментарии")

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
        setupFAB()
    }

    private fun setupRecyclerView() {
        adapter = CommentsAdapter(viewModel) { comment ->
            showEditDialog(comment)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CommentsActivity)
            adapter = this@CommentsActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.comments.collect { comments ->
                adapter.submitList(comments)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.isVisible = isLoading

                if (!isLoading) {
                    stopSwipeRefresh()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    showSnackbar(it)
                    viewModel.clearError()
                    stopSwipeRefresh()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.showEmptyState.collect { show ->
                binding.emptyView.isVisible = show
                binding.recyclerView.isVisible = !show
            }
        }

        lifecycleScope.launch {
            viewModel.emptyStateText.collect { text ->
                binding.emptyView.text = text
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                try {
                    if (viewModel.isLoading.value) {
                        stopSwipeRefresh()
                        return@launch
                    }

                    viewModel.loadComments()

                    launch {
                        delay(3000)
                        if (binding.swipeRefresh.isRefreshing) {
                            stopSwipeRefresh()
                        }
                    }
                } catch (e: Exception) {
                    stopSwipeRefresh()
                    showSnackbar("Ошибка: ${e.message}")
                }
            }
        }

        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun stopSwipeRefresh() {
        if (binding.swipeRefresh.isRefreshing) {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupFAB() {
        binding.fabAddComment.setOnClickListener {
            showCreateDialog()
        }
    }

    private fun showCreateDialog() {
        val dialog = CommentDialogFragment.newInstance()
        dialog.setOnCommentCreatedListener { text ->
            viewModel.addComment(text) {
                showSnackbar("Комментарий добавлен")
            }
        }
        dialog.show(supportFragmentManager, "CommentDialog")
    }

    private fun showEditDialog(comment: CommentResponse) {
        val dialog = CommentDialogFragment.editInstance(comment.id, comment.comment)
        dialog.setOnCommentUpdatedListener { id, text ->
            viewModel.updateComment(id, text) {
                showSnackbar("Комментарий обновлён")
            }
        }
        dialog.show(supportFragmentManager, "CommentDialog")
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, CommentsActivity::class.java)
            context.startActivity(intent)
        }
    }
}