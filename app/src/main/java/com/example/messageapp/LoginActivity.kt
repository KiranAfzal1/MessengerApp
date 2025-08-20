package com.example.messageapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import com.messenger.databinding.ActivityLoginBinding
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    enum class LoginStep { PHONE, OTP }
    private var currentStep = LoginStep.PHONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val sharedPrefs = getSharedPreferences("messenger_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPrefs.getBoolean("is_logged_in", false)

        if (isLoggedIn && auth.currentUser != null) {
            startActivity(Intent(this, ChatListActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.sendCodeButton.isEnabled =
                    !binding.phoneInput.text.isNullOrBlank() &&
                            !binding.nameInput.text.isNullOrBlank()
            }
        }

        binding.phoneInput.addTextChangedListener(textWatcher)
        binding.nameInput.addTextChangedListener(textWatcher)

        binding.otpInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.verifyButton.isEnabled = s?.length == 6
            }
        })
    }

    private fun setupClickListeners() {
        binding.sendCodeButton.setOnClickListener {
            if (binding.phoneInput.text.toString().isNotBlank() &&
                binding.nameInput.text.toString().isNotBlank()
            ) {
                sendVerificationCode()
            }
        }

        binding.verifyButton.setOnClickListener {
            val code = binding.otpInput.text.toString()
            if (code.length == 6 && storedVerificationId != null) {
                verifyOTP(code)
            }
        }

        binding.backButton.setOnClickListener {
            showPhoneStep()
        }
    }

    private fun sendVerificationCode() {
        var phone = binding.phoneInput.text.toString().trim()
        if (!phone.startsWith("+")) phone = "+92$phone"

        binding.progressBar.visibility = View.VISIBLE
        binding.sendCodeButton.isEnabled = false
        binding.sendCodeButton.text = "Sending..."

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    binding.progressBar.visibility = View.GONE
                    binding.sendCodeButton.isEnabled = true
                    binding.sendCodeButton.text = "Send Verification Code"
                    e.printStackTrace()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    storedVerificationId = verificationId
                    resendToken = token
                    binding.progressBar.visibility = View.GONE
                    showOTPStep()
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOTP(code: String) {
        binding.verifyButton.isEnabled = false
        binding.verifyButton.text = "Verifying..."
        binding.progressBar.visibility = View.VISIBLE

        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            binding.progressBar.visibility = View.GONE
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                val name = binding.nameInput.text.toString().trim()
                val phone = binding.phoneInput.text.toString().trim()

                saveUserToDatabase(uid, name, phone)

                val sharedPrefs = getSharedPreferences("messenger_prefs", MODE_PRIVATE)
                sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
                sharedPrefs.edit().putString("user_phone", phone).apply()
                sharedPrefs.edit().putString("user_name", name).apply()

                startActivity(Intent(this, ChatListActivity::class.java))
                finish()
            } else {
                binding.verifyButton.isEnabled = true
                binding.verifyButton.text = "Verify & Continue"
                task.exception?.printStackTrace()
            }
        }
    }

    private fun saveUserToDatabase(uid: String, name: String, phone: String) {
        val userMap = mapOf("name" to name, "phone" to phone)
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .setValue(userMap)
            .addOnSuccessListener { println("User saved to database") }
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    private fun showPhoneStep() {
        currentStep = LoginStep.PHONE
        binding.phoneContainer.visibility = View.VISIBLE
        binding.otpContainer.visibility = View.GONE
        binding.titleText.text = "Welcome to Messenger"
        binding.descriptionText.text = "Enter your phone number and name to get started"
    }

    private fun showOTPStep() {
        currentStep = LoginStep.OTP
        binding.phoneContainer.visibility = View.GONE
        binding.otpContainer.visibility = View.VISIBLE
        binding.titleText.text = "Verify Phone Number"
        binding.descriptionText.text = "Enter the verification code sent to your phone"
        binding.phoneNumberDisplay.text = "Code sent to ${binding.phoneInput.text}"
        binding.sendCodeButton.text = "Send Verification Code"
        binding.sendCodeButton.isEnabled = true
    }
}
