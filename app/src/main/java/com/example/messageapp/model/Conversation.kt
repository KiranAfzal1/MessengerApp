package com.example.messageapp.model

data class Conversation(
    val id: String = "",
    val name: String = "",
    val isGroup: Boolean = false,
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val avatarUrl: String = "",
    val extra: String = "" 
)
