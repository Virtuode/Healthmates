package com.corps.healthmate.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.corps.healthmate.R
import com.corps.healthmate.activities.ChatDetailActivity
import com.corps.healthmate.adapters.ChatAdapterPatient
import com.corps.healthmate.models.Chat
import com.corps.healthmate.viewmodel.ChatViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ChatFragmentPatient : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapterPatient
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var mainContent: View
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_patient, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        observeChats()
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.chat_recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        loadingIndicator = view.findViewById(R.id.loading_indicator)
        emptyStateText = view.findViewById(R.id.empty_state_text)
        shimmerLayout = view.findViewById(R.id.shimmer_layout)
        mainContent = view.findViewById(R.id.main_content)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapterPatient(
            context = requireContext(),
            onChatClick = { chat ->
                if (viewModel.isChatTimeValid(chat)) {
                    navigateToChatDetail(chat)
                } else {
                    showChatTimeError(chat)
                }
            },
            viewModel = viewModel
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshChats()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                swipeRefreshLayout.isRefreshing = isLoading
            }
        }
    }

    private fun observeChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chats.collect { chats ->
                updateUI(chats)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    shimmerLayout.visibility = View.VISIBLE
                    shimmerLayout.startShimmer()
                    mainContent.visibility = View.GONE
                } else {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    mainContent.visibility = View.VISIBLE
                }
                // Keep the loading indicator for swipe refresh
                loadingIndicator.visibility = if (isLoading && swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { errorMessage ->
                errorMessage?.let {
                    Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun updateUI(chats: List<Chat>) {
        chatAdapter.submitList(chats)
        if (chats.isEmpty() && !viewModel.isLoading.value) {
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun navigateToChatDetail(chat: Chat) {
        startActivity(
            Intent(requireContext(), ChatDetailActivity::class.java)
                .putExtra("chatId", chat.id)
                .putExtra("doctorName", chat.doctorName)
        )
    }

    private fun showChatTimeError(chat: Chat) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val appointmentTime = sdf.parse(chat.appointmentTime) ?: Date()
            val displayFormat = SimpleDateFormat("MMM d, yyyy, HH:mm", Locale.getDefault())
            val formattedTime = displayFormat.format(appointmentTime)
            val message = "Chat available on $formattedTime"
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                .setAction("Dismiss") { /* No action needed */ }
                .setBackgroundTint(resources.getColor(R.color.colorPrimary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .setActionTextColor(resources.getColor(android.R.color.white, null))
                .show()
        } catch (e: Exception) {
            Snackbar.make(requireView(), "Chat availability TBD", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shimmerLayout.stopShimmer()
        recyclerView.adapter = null
    }
}