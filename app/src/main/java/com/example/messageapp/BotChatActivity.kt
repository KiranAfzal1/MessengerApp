package com.example.messageapp

import com.example.messageapp.adapter.BotMessageAdapter
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messageapp.model.BotMessage
import com.messenger.R
import com.messenger.databinding.ActivityBotChatBinding
import java.text.SimpleDateFormat
import java.util.*

class BotChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBotChatBinding
    private lateinit var adapter: BotMessageAdapter
    private val messages = mutableListOf<BotMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBotChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        adapter = BotMessageAdapter(messages)
        binding.botChatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.botChatRecyclerView.adapter = adapter

        // Back button
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }

        // Send button
        binding.sendButton.setOnClickListener {
            val text = binding.messageBox.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(BotMessage(text, true)) // user message
                binding.messageBox.setText("")
                botReply(text)
            } else {
                Toast.makeText(this, "Type something...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addMessage(message: BotMessage) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        binding.botChatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun botReply(userMessage: String) {
        val reply = getBotResponse(userMessage)
        addMessage(BotMessage(reply, false)) // bot message
    }

    private fun getBotResponse(message: String): String {
        val lower = message.lowercase(Locale.ROOT)

        return when {
            lower == "help" -> """
                Here are the commands you can use:
                • hello,hi,hey,assalamalikum → Greet the bot
                • how are you → Check bot status
                • who are you → Know bot’s name
                • time → Get current time
                • date → Get today’s date
                • bye → End the chat
            """.trimIndent()

            "hello" in lower -> "Hi there! I am your Messenger Bot "
            "hey" in lower -> "Hi there ! I am your Messenger Bot"
            "hi" in lower-> "Hello ! I am your Messenger Bot"
            "assalamalikum" in lower-> "waalikumassalam"
            "how are you" in lower -> "I’m doing great, thanks for asking!"
            "who are you" in lower -> "I’m your Messenger Bot "
            "bye" in lower -> "Goodbye! Have a nice day."

            // Date & Time
            "time" in lower -> {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                "The current time is ${sdf.format(Date())}"
            }
            "date" in lower -> {
                val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                "Today is ${sdf.format(Date())}"
            }

            else -> "I didn’t understand that. Type 'help' to see available commands."
        }
    }
}
