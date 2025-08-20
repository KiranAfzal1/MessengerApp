package com.example.messageapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messageapp.adapter.ConversationAdapter
import com.example.messageapp.model.Conversation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.messenger.databinding.ActivityChatListBinding

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var adapter: ConversationAdapter
    private val usersRef = FirebaseDatabase.getInstance(
        "https://messageapp-28a37-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).getReference("users")
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        saveCurrentUser() // ✅ Save/update current user in DB

        // Set current user name in header
        if (currentUserId.isNotEmpty()) {
            usersRef.child(currentUserId).child("name")
                .get()
                .addOnSuccessListener { nameSnapshot ->
                    val userName = nameSnapshot.getValue(String::class.java) ?: "Me"
                    binding.userNameText.text = userName
                }
                .addOnFailureListener {
                    binding.userNameText.text = "Me"
                }
        }

        setupRecycler()
        listenForUsers()
        setupClickListeners()
    }

    private fun saveCurrentUser() {
        val sharedPrefs = getSharedPreferences("messenger_prefs", MODE_PRIVATE)
        val name = sharedPrefs.getString("user_name", "Unknown") ?: "Unknown"
        val phone = sharedPrefs.getString("user_phone", "Unknown") ?: "Unknown"

        val userMap = mapOf("name" to name, "phone" to phone)

        usersRef.child(currentUserId)
            .setValue(userMap)
            .addOnSuccessListener { println("✅ User saved/updated in DB: $currentUserId") }
            .addOnFailureListener { e ->
                println("❌ Failed to save user")
                e.printStackTrace()
            }
    }

    private fun setupRecycler() {
        adapter = ConversationAdapter { conv ->
            val conversationId = getConversationId(currentUserId, conv.id)
            val intent = Intent(this, ChatDetailActivity::class.java).apply {
                putExtra("conversation_id", conversationId)
                putExtra("conversation_name", conv.name)
                putExtra("is_group", false)
            }
            startActivity(intent)
        }
        binding.conversationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.conversationsRecyclerView.adapter = adapter
    }

    private fun listenForUsers() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { userSnapshot ->
                    val uid = userSnapshot.key ?: return@mapNotNull null
                    if (uid == currentUserId) return@mapNotNull null

                    val name = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"

                    Conversation(
                        id = uid,
                        name = name,
                        isGroup = false,
                        participants = listOf(uid, currentUserId),
                        lastMessage = "",
                        avatarUrl = ""
                    )
                }
                adapter.submitList(list)
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    private fun setupClickListeners() {
        binding.signOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        binding.newChatFab.setOnClickListener {
            // TODO: Navigate to new chat screen
        }
    }

    // Utility: generate conversation ID for 1-1 chats
    private fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "$uid1-$uid2" else "$uid2-$uid1"
    }
}
