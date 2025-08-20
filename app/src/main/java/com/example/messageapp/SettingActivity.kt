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
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

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
        val phone = sharedPrefs.getString("user_phone", "Unknown")

        binding.userNameText.text = name
        binding.userPhoneText.text = phone
    }

    private fun setupNotificationToggle() {
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        binding.notificationSwitch.isChecked = notificationsEnabled

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("notifications_enabled", isChecked).apply()

            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("user_$currentUserId")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("user_$currentUserId")
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
                    if (newName.isNotEmpty()) {
                        updateUsername(newName)
                    } else {
                        Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
        }
    }

    private fun updateUsername(newName: String) {
        // Update locally
        sharedPrefs.edit().putString("user_name", newName).apply()
        binding.userNameText.text = newName

        // Update in Firebase Realtime Database
        FirebaseDatabase.getInstance().getReference("users")
            .child(currentUserId)
            .child("name")
            .setValue(newName)
            .addOnSuccessListener {
                Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show()
            }
    }
}
