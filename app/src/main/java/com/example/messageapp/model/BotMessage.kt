package com.example.messageapp.model;

data class BotMessage(
    val text: String,
    val isUser: Boolean // true = user, false = bot
)
