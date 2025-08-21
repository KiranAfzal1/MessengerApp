package com.example.messageapp.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val fcmToken: String = "",
    val status: String = "",
    val lastSeen: Long = 0L
)
