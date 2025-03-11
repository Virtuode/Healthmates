package com.corps.healthmate.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.activities.ChatDetailActivity
import com.corps.healthmate.adapters.ChatAdapterPatient
import com.corps.healthmate.models.Chat
import com.corps.healthmate.viewmodel.ChatViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ChatFragmentPatient : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapterPatient
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
        setupRecyclerView()
        observeChats()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapterPatient(
            requireContext(),
            emptyList(),
            { chat ->
                if (viewModel.isChatTimeValid(chat)) {
                    navigateToChatDetail(chat)
                } else {
                    showChatTimeError(chat)
                }
            },
            viewModel
        )

        recyclerView = requireView().findViewById<RecyclerView>(R.id.chat_recycler_view).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chats.collect { chats ->
                Log.d("ChatFragmentPatient", "Fetched chats: $chats")
                chatAdapter.updateChats(chats) // All chats are shown
            }
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
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("ChatFragmentPatient", "Failed to parse time: ${chat.appointmentTime}", e)
            Snackbar.make(requireView(), "Chat availability TBD", Snackbar.LENGTH_LONG).show()
        }
    }
}