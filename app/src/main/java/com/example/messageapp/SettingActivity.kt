package com.example.messageapp

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.messenger.databinding.ActivitySettingsBinding

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPrefs: SharedPreferences

    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("messenger_prefs", MODE_PRIVATE)

        loadUserData()
        setupNotificationToggle()
        setupBackButton()
        setupEditUsernameButton()
    }

    private fun loadUserData() {
        val name = sharedPrefs.getString("user_name", "Unknown")
        val email = sharedPrefs.getString("user_email", "Unknown")
        binding.userNameText.text = name
        binding.userPhoneText.text = email
    }

    private fun setupNotificationToggle() {
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        binding.notificationSwitch.isChecked = notificationsEnabled

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("notifications_enabled", isChecked).apply()

            if (currentUserId.isEmpty()) return@setOnCheckedChangeListener

            val userRef = FirebaseDatabase.getInstance("https://messageapp-28a37-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users")
                .child(currentUserId)

            if (isChecked) {

                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    userRef.child("fcmToken").setValue(token)
                    FirebaseMessaging.getInstance().subscribeToTopic("user_$currentUserId")
                    Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
                }
            } else {

                userRef.child("fcmToken").removeValue()
                FirebaseMessaging.getInstance().unsubscribeFromTopic("user_$currentUserId")
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupBackButton() {
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupEditUsernameButton() {
        binding.editUsernameButton.setOnClickListener {
            val editText = EditText(this)
            editText.setText(binding.userNameText.text.toString())

            AlertDialog.Builder(this)
                .setTitle("Edit Username")
                .setView(editText)
                .setPositiveButton("Save") { _, _ ->
                    val newName = editText.text.toString().trim()
                    if (newName.isNotEmpty()) updateUsername(newName)
                    else Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateUsername(newName: String) {
        if (currentUserId.isEmpty()) return

        sharedPrefs.edit().putString("user_name", newName).apply()

        val userRef = FirebaseDatabase.getInstance("https://messageapp-28a37-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("users")
            .child(currentUserId)
            .child("name")

        userRef.setValue(newName)

        val chatsRef = FirebaseDatabase.getInstance("https://messageapp-28a37-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("chats")

        chatsRef.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { chatSnapshot ->
                val participants = chatSnapshot.child("meta/participants").children.map { it.value.toString() }
                if (participants.contains(currentUserId)) {

                    chatSnapshot.ref.child("meta/participantsName").child(currentUserId).setValue(newName)

                    chatSnapshot.child("messages").children.forEach { msgSnapshot ->
                        val senderId = msgSnapshot.child("senderId").getValue(String::class.java)
                        if (senderId == currentUserId) {
                            msgSnapshot.ref.child("senderName").setValue(newName)
                        }
                    }
                }
            }
        }

        binding.userNameText.text = newName
        Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show()
    }

}
