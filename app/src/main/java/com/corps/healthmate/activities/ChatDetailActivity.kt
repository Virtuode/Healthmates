package com.corps.healthmate.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.adapters.MessageAdapter
import com.corps.healthmate.viewmodel.ChatDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatDetailActivity : AppCompatActivity() {
    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_detail)

        setupViews()
        setupRecyclerView()
        observeMessages()
        setupSendButton()
    }

    private fun setupViews() {
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.messagesRecyclerView)

        val chatId = intent.getStringExtra("chatId") ?: run {
            Toast.makeText(this, "Invalid chat ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val doctorName = intent.getStringExtra("doctorName") ?: "Doctor"

        supportActionBar?.apply {
            title = doctorName
            setDisplayHomeAsUpEnabled(true)
        }

        viewModel.setChatId(chatId)
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatDetailActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun observeMessages() {
        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                if (messages.isEmpty()) {
                    Toast.makeText(this@ChatDetailActivity, "No messages yet", Toast.LENGTH_SHORT).show()
                }
                messageAdapter.updateMessages(messages)
                recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                messageInput.text.clear()
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}