package com.example.messageapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messageapp.adapter.MessageAdapter
import com.example.messageapp.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.messenger.databinding.ActivityChatDetailBinding

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private lateinit var otherUserId: String
    private lateinit var otherUserName: String

    private val db = FirebaseDatabase.getInstance().reference
    private lateinit var messagesRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the other user id and name from intent
        otherUserId = intent.getStringExtra("conversation_id") ?: ""
        otherUserName = intent.getStringExtra("conversation_name") ?: ""
        binding.contactName.text = otherUserName

        setupRecycler()
        setupSendButton()
        setupMessagesListener()
    }

    private fun setupRecycler() {
        messageAdapter = MessageAdapter(currentUserId)
        binding.messagesRecyclerView.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.messagesRecyclerView.adapter = messageAdapter
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isNotBlank()) sendMessage(text)
        }
    }

    private fun setupMessagesListener() {
        val conversationId = getConversationId(currentUserId, otherUserId)
        messagesRef = db.child("chats").child(conversationId).child("messages")

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
                binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    private fun sendMessage(text: String) {
        val conversationId = getConversationId(currentUserId, otherUserId)
        val newMsgRef = db.child("chats").child(conversationId).child("messages").push()
        val timestamp = System.currentTimeMillis()
        val msg = mapOf(
            "senderId" to currentUserId,
            "content" to text,
            "timestamp" to timestamp,
            "isRead" to false
        )
        newMsgRef.setValue(msg)

        // Save lastMessage summary for chat list display
        val lastMsgMap = mapOf(
            "lastMessage" to text,
            "lastMessageTime" to timestamp,
            "participants" to listOf(currentUserId, otherUserId)
        )
        db.child("chats").child(conversationId).child("meta").setValue(lastMsgMap)

        binding.messageInput.text?.clear()
    }

    // Generate a unique conversation id for 1-1 chat
    private fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "$uid1-$uid2" else "$uid2-$uid1"
    }
}
