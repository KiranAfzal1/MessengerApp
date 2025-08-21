package com.example.messageapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.widget.doAfterTextChanged
import com.example.messageapp.adapter.ConversationAdapter
import com.example.messageapp.model.Conversation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.messenger.databinding.ActivityChatListBinding
import com.example.messageapp.BotChatActivity

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var adapter: ConversationAdapter
    private val allUsers = mutableListOf<Conversation>()

    private val usersRef = FirebaseDatabase.getInstance(
        "https://messageapp-28a37-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).getReference("users")
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureCurrentUserExists()
        setupRecycler()
        listenForUsers()
        setupClickListeners()
        setupSearch()
    }

    /**
     * Ensure current user exists in DB
     */
    private fun ensureCurrentUserExists() {
        if (currentUserId.isEmpty()) return

        val sharedPrefs = getSharedPreferences("messenger_prefs", MODE_PRIVATE)
        val name = sharedPrefs.getString("user_name", "Unknown") ?: "Unknown"
        val email = sharedPrefs.getString("user_email", "Unknown") ?: "Unknown"

        usersRef.child(currentUserId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Create new user record with full data
                val userMap = mapOf(
                    "name" to name,
                    "email" to email,
                    "isOnline" to true,
                    "lastSeen" to System.currentTimeMillis()
                )
                usersRef.child(currentUserId).setValue(userMap)
            } else {
                // Update UI with saved name
                val dbName = snapshot.child("name").getValue(String::class.java) ?: name
                binding.userNameText.text = dbName

                // Update online status
                usersRef.child(currentUserId).updateChildren(
                    mapOf(
                        "isOnline" to true,
                        "lastSeen" to System.currentTimeMillis()
                    )
                )
            }
        }.addOnFailureListener { it.printStackTrace() }
    }

    private fun setupRecycler() {
        adapter = ConversationAdapter { conv ->
            // Open ChatDetailActivity with conversationId and name
            val conversationId = getConversationId(currentUserId, conv.id)
            startActivity(Intent(this, ChatDetailActivity::class.java).apply {
                putExtra("conversation_id", conversationId)
                putExtra("conversation_name", conv.name)
            })
        }
        binding.conversationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.conversationsRecyclerView.adapter = adapter
    }

    private fun listenForUsers() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                snapshot.children.forEach { userSnapshot ->
                    val uid = userSnapshot.key ?: return@forEach
                    if (uid == currentUserId) return@forEach

                    val name = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val email = userSnapshot.child("email").getValue(String::class.java) ?: "Unknown"

                    allUsers.add(
                        Conversation(
                            id = uid,
                            name = name,
                            isGroup = false,
                            participants = listOf(uid, currentUserId),
                            lastMessage = "",
                            avatarUrl = "",
                            extra = email
                        )
                    )
                }
                updateAdapter(allUsers)
            }

            override fun onCancelled(error: DatabaseError) = error.toException().printStackTrace()
        })
    }

    private fun setupClickListeners() {
        binding.signOutButton.setOnClickListener {
            if (currentUserId.isNotEmpty()) {
                usersRef.child(currentUserId).updateChildren(
                    mapOf(
                        "isOnline" to false,
                        "lastSeen" to System.currentTimeMillis()
                    )
                )
            }
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        binding.newChatBtn.setOnClickListener {
            // Open BotChatActivity (AI Chat)
            startActivity(Intent(this, BotChatActivity::class.java))
        }
    }

    private fun setupSearch() {
        binding.searchInput.doAfterTextChanged { editable ->
            val query = editable?.toString()?.trim()?.lowercase() ?: ""
            val filtered = if (query.isEmpty()) {
                allUsers
            } else {
                allUsers.filter {
                    it.name.lowercase().contains(query) || it.extra.lowercase().contains(query)
                }
            }
            updateAdapter(filtered)
        }
    }

    private fun updateAdapter(list: List<Conversation>) {
        adapter.submitList(list)
        binding.emptyState.visibility =
            if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun getConversationId(uid1: String, uid2: String) =
        if (uid1 < uid2) "$uid1-$uid2" else "$uid2-$uid1"

    /**
     * Update presence automatically
     */
    override fun onStart() {
        super.onStart()
        if (currentUserId.isNotEmpty()) {
            usersRef.child(currentUserId).child("isOnline").setValue(true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (currentUserId.isNotEmpty()) {
            usersRef.child(currentUserId).updateChildren(
                mapOf(
                    "isOnline" to false,
                    "lastSeen" to System.currentTimeMillis()
                )
            )
        }
    }
}
