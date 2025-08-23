package com.example.messageapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.widget.doAfterTextChanged
import com.example.messageapp.adapter.MessageAdapter
import com.example.messageapp.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.messenger.databinding.ActivityChatDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var otherUserId: String
    private lateinit var otherUserName: String

    private lateinit var messagesRef: DatabaseReference
    private lateinit var metaRef: DatabaseReference
    private lateinit var conversationId: String
    private lateinit var otherUserStatusRef: DatabaseReference

    private val databaseUrl =
        "https://messageapp-28a37-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationId = intent.getStringExtra("conversation_id") ?: "test-uid1-uid2"
        otherUserId = conversationId.split("-").find { it != currentUserId } ?: "uid2"
        otherUserName = intent.getStringExtra("conversation_name") ?: "Test User"
        binding.contactName.text = otherUserName

        setupRecycler()
        setupInputWatcher()
        setupSendButton()
        setupMessagesListener()
        setupOtherUserStatusListener()
        setupBackButton()

        metaRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("chats/$conversationId/meta")
        val participantsUpdate = mapOf(
            "participants" to listOf(currentUserId, otherUserId)
        )
        metaRef.updateChildren(participantsUpdate)
            .addOnSuccessListener { Log.d("ChatDetail", "Participants ensured in meta") }
            .addOnFailureListener { Log.e("ChatDetail", "Failed to write participants", it) }
    }

    private fun setupRecycler() {
        messageAdapter = MessageAdapter(currentUserId)
        binding.messagesRecyclerView.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.messagesRecyclerView.adapter = messageAdapter
    }

    private fun setupInputWatcher() {
        binding.messageInput.doAfterTextChanged { text ->
            binding.sendButton.isEnabled = !text.isNullOrBlank()
        }
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isNotBlank()) sendMessage(text)
        }
    }

    private fun setupMessagesListener() {
        messagesRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("chats/$conversationId/messages")
        metaRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("chats/$conversationId/meta")

        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                snapshot.children.forEach { child ->
                    val message = Message(
                        id = child.key ?: "",
                        conversationId = conversationId,
                        senderId = child.child("senderId").getValue(String::class.java) ?: "",
                        content = child.child("content").getValue(String::class.java) ?: "",
                        timestamp = child.child("timestamp").getValue(Long::class.java)
                            ?: System.currentTimeMillis(),
                        isRead = child.child("isRead").getValue(Boolean::class.java) ?: false,
                        isSent = true
                    )
                    messages.add(message)
                }
                messageAdapter.submitList(messages.toList())
                if (messages.isNotEmpty()) {
                    binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
                }
                Log.d("ChatDetail", "Messages loaded: ${messages.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatDetail", "Failed to load messages", error.toException())
            }
        })
    }

    private fun setupOtherUserStatusListener() {
        otherUserStatusRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users")
            .child(otherUserId)

        otherUserStatusRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L

                binding.statusText.text = if (isOnline) {
                    "Online"
                } else {
                    if (lastSeen > 0) {
                        val time = getFormattedTime(lastSeen)
                        "Last seen: $time"
                    } else {
                        "Offline"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatDetail", "Failed to load user status", error.toException())
            }
        })
    }

    private fun getFormattedTime(timeMillis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    private fun sendMessage(text: String) {
        if (currentUserId.isEmpty() || otherUserId.isEmpty()) {
            Log.e("ChatDetail", "Cannot send message. User IDs missing.")
            return
        }

        val newMsgRef = messagesRef.push()
        val timestamp = System.currentTimeMillis()

        val msg = mapOf(
            "senderId" to currentUserId,
            "content" to text,
            "timestamp" to timestamp,
            "isRead" to false
        )

        newMsgRef.setValue(msg)
            .addOnSuccessListener {
                Log.d("ChatDetail", "Message sent: $text")
                binding.messageInput.text?.clear()
            }
            .addOnFailureListener {
                Log.e("ChatDetail", "Failed to send message", it)
            }

        val meta = mapOf(
            "lastMessage" to text,
            "lastMessageTime" to timestamp,
            "participants" to listOf(currentUserId, otherUserId)
        )
        metaRef.updateChildren(meta)
            .addOnSuccessListener { Log.d("ChatDetail", "Meta updated") }
            .addOnFailureListener { Log.e("ChatDetail", "Failed to update meta", it) }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener { finish() }
    }
}
