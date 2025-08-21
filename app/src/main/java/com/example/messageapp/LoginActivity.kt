package com.example.messageapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.messenger.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        checkAlreadyLoggedIn()
        setupUI()
        setupClickListeners()
    }

    private fun checkAlreadyLoggedIn() {
        val sharedPrefs = getSharedPreferences("messenger_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPrefs.getBoolean("is_logged_in", false)
        if (isLoggedIn && auth.currentUser != null) {
            startActivity(Intent(this, ChatListActivity::class.java))
            finish()
        }
    }

    private fun setupUI() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.sendCodeButton.isEnabled =
                    binding.nameInput.text.toString().isNotBlank() &&
                            binding.emailInput.text.toString().isNotBlank() &&
                            binding.passwordInput.text.toString().isNotBlank()
            }
        }

        binding.nameInput.addTextChangedListener(watcher)
        binding.emailInput.addTextChangedListener(watcher)
        binding.passwordInput.addTextChangedListener(watcher)
    }

    private fun setupClickListeners() {
        binding.sendCodeButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            signInOrRegister(name, email, password)
        }
    }

    private fun signInOrRegister(name: String, email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.sendCodeButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    saveUserData(name, email)
                } else {

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { createTask ->
                            binding.progressBar.visibility = View.GONE
                            binding.sendCodeButton.isEnabled = true
                            if (createTask.isSuccessful) {
                                saveUserData(name, email)
                            } else {
                                val errorMsg = createTask.exception?.message ?: "Unknown error"
                                Log.e("LoginActivity", "Auth Failed: $errorMsg")
                                Toast.makeText(this, "Auth Failed: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
    }

    private fun saveUserData(name: String, email: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Error: User UID is null", Toast.LENGTH_LONG).show()
            return
        }

        val database = FirebaseDatabase.getInstance(
            "https://messageapp-28a37-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        val usersRef = database.getReference("users")

        val userMap = mapOf(
            "name" to name,
            "email" to email,
            "isOnline" to true, // mark as online on login
            "lastSeen" to System.currentTimeMillis() // current timestamp
        )

        usersRef.child(uid).setValue(userMap).addOnCompleteListener { task ->
            binding.progressBar.visibility = View.GONE
            binding.sendCodeButton.isEnabled = true

            if (task.isSuccessful) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                    if (tokenTask.isSuccessful) {
                        val fcmToken = tokenTask.result
                        usersRef.child(uid).child("fcm_token").setValue(fcmToken)
                    }
                }

                // Save SharedPrefs
                val sharedPrefs = getSharedPreferences("messenger_prefs", MODE_PRIVATE)
                sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
                sharedPrefs.edit().putString("user_name", name).apply()
                sharedPrefs.edit().putString("user_email", email).apply()

                startActivity(Intent(this, ChatListActivity::class.java))
                finish()
            } else {
                val errorMsg = task.exception?.message ?: "Failed to save user data"
                Log.e("LoginActivity", "DB Save Failed: $errorMsg")
                Toast.makeText(this, "DB Save Failed: $errorMsg", Toast.LENGTH_LONG).show()
            }
        }
    }

}
